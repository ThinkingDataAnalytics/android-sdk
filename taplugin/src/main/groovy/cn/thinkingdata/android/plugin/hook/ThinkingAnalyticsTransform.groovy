package cn.thinkingdata.android.plugin.hook

import cn.thinkingdata.android.plugin.config.ThinkingClassNameAnalytics
import cn.thinkingdata.android.plugin.config.ThinkingAnalyticsTransformHelper
import cn.thinkingdata.android.plugin.config.ThinkingFragmentHookConfig
import cn.thinkingdata.android.plugin.utils.LoggerUtil
import cn.thinkingdata.android.plugin.utils.ThinkingAnalyticsUtil
import cn.thinkingdata.android.plugin.utils.ThinkingVersionUtils
import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.ide.common.internal.WaitableExecutor
import groovy.io.FileType
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

import java.security.CodeSource
import java.security.ProtectionDomain
import java.util.concurrent.Callable
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * 扫描项目中的类
 */
class ThinkingAnalyticsTransform extends Transform {

    private ThinkingAnalyticsTransformHelper mThinkingTransformHelper
    private URLClassLoader urlClassLoader
    private String thinkingSdkJarPath
    private WaitableExecutor buildExecutor //多线程编译
    private volatile boolean isFindThinkingSDK = false

    ThinkingAnalyticsTransform(ThinkingAnalyticsTransformHelper transformHelper) {
        this.mThinkingTransformHelper = transformHelper
        if (mThinkingTransformHelper.enableTAMultiThread) {
            buildExecutor = WaitableExecutor.useGlobalSharedThreadPool()
        }
    }

    @Override
    String getName() {
        return "thinkingAnalyticsAutoTrack"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return mThinkingTransformHelper.enableTAIncremental
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        //扫描之前读取配置信息
        beforeBuild(transformInvocation)
        //开始扫描
        transformAllClass(transformInvocation.context, transformInvocation.inputs, transformInvocation.outputProvider, transformInvocation.incremental)
        //结束扫描
        afterBuild()
    }

    private void transformAllClass(Context context, Collection<TransformInput> inputs, TransformOutputProvider outputProvider, boolean isIncremental)
            throws IOException, TransformException, InterruptedException {
        long startTime = System.currentTimeMillis()
        if (!isIncremental) {
            outputProvider.deleteAll()
        }

        inputs.each { TransformInput input ->
            //遍历 jar
            input.jarInputs.each { JarInput jarInput ->
                if (buildExecutor) {
                    buildExecutor.execute(new Callable<Object>() {
                        @Override
                        Object call() throws Exception {
                            traversalJar(isIncremental, jarInput, outputProvider, context)
                            return null
                        }
                    })
                } else {
                    traversalJar(isIncremental, jarInput, outputProvider, context)
                }
            }

            //遍历目录
            input.directoryInputs.each { DirectoryInput directoryInput ->
                if (buildExecutor) {
                    buildExecutor.execute(new Callable<Object>() {
                        @Override
                        Object call() throws Exception {
                            traversalDirectory(isIncremental, directoryInput, outputProvider, context)
                            return null
                        }
                    })
                } else {
                    traversalDirectory(isIncremental, directoryInput, outputProvider, context)
                }
            }
        }

        if (buildExecutor) {
            buildExecutor.waitForTasksWithQuickFail(true)
        }

        LoggerUtil.info("编译共耗时:${System.currentTimeMillis() - startTime}毫秒")
    }

    void traversalJar(boolean isIncremental, JarInput jarInput, TransformOutputProvider outputProvider, Context context) {
        String dtName = jarInput.file.name
        def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath).substring(0, 8)
        if (dtName.endsWith(".jar")) {
            dtName = dtName.substring(0, dtName.length() - 4)
        }
        File dtFile = outputProvider.getContentLocation(dtName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR)
        if (isIncremental) {
            Status status = jarInput.getStatus()
            switch (status) {
                case Status.NOTCHANGED:
                    break
                case Status.ADDED:
                case Status.CHANGED:
                    LoggerUtil.info("jar status = $status:$dtFile.absolutePath")
                    transformEachJar(dtFile, jarInput, context)
                    break
                case Status.REMOVED:
                    LoggerUtil.info("jar status = $status:$dtFile.absolutePath")
                    if (dtFile.exists()) {
                        FileUtils.forceDelete(dtFile)
                    }
                    break
                default:
                    break
            }
        } else {
            transformEachJar(dtFile, jarInput, context)
        }
    }

    void transformEachJar(File dest, JarInput jarInput, Context context) {
        def modifiedJar = null
        if (!mThinkingTransformHelper.extension.disableJar || checkJarValidate(jarInput)) {
            LoggerUtil.info("开始遍历 jar：" + jarInput.file.absolutePath)
            modifiedJar = modifyAllJarFile(jarInput.file, context.getTemporaryDir())
            LoggerUtil.info("结束遍历 jar：" + jarInput.file.absolutePath)
        }
        if (modifiedJar == null) {
            modifiedJar = jarInput.file
        }
        FileUtils.copyFile(modifiedJar, dest)
    }

    private boolean checkJarValidate(JarInput jarInput) {
        try {
            if (isFindThinkingSDK || thinkingSdkJarPath == null) {
                return false
            }
            def jarLocation = jarInput.file.toURI().getPath()
            if (thinkingSdkJarPath.length() == jarLocation.length() && thinkingSdkJarPath == jarLocation) {
                isFindThinkingSDK = true
                return true
            } else {
                return false
            }
        } catch (Throwable throwable) {
            LoggerUtil.error("Checking jar's validation error: " + throwable.localizedMessage)
            return false
        }
    }

    /**
     * 修改 jar 文件中对应字节码
     */
    private File modifyAllJarFile(File jarFile, File tempDir) {
        if (jarFile) {
            return modifyEachJar(jarFile, tempDir, true)
        }
        return null
    }

    private File modifyEachJar(File jarFile, File tempDir, boolean isNameHex) {
        //解决 zip file is empty
        if (jarFile == null || jarFile.length() == 0) {
            return null
        }
        def file = new JarFile(jarFile, false)
        def tmpNameHex = ""
        if (isNameHex) {
            tmpNameHex = DigestUtils.md5Hex(jarFile.absolutePath).substring(0, 8)
        }
        def outputJar = new File(tempDir, tmpNameHex + jarFile.name)
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outputJar))
        Enumeration enumeration = file.entries()

        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement()
            InputStream inputStream
            try {
                inputStream = file.getInputStream(jarEntry)
            } catch (Exception e) {
                IOUtils.closeQuietly(inputStream)
                e.printStackTrace()
                return null
            }
            String entryName = jarEntry.getName()
            if (entryName.endsWith(".DSA") || entryName.endsWith(".SF")) {
                //ignore
            } else {
                String className
                JarEntry entry = new JarEntry(entryName)
                byte[] modifiedClassBytes = null
                byte[] sourceClassBytes
                try {
                    jarOutputStream.putNextEntry(entry)
                    sourceClassBytes = ThinkingAnalyticsUtil.toByteArrayStream(inputStream)
                } catch (Exception e) {
                    LoggerUtil.error("Exception encountered while processing jar: " + jarFile.getAbsolutePath())
                    IOUtils.closeQuietly(file)
                    IOUtils.closeQuietly(jarOutputStream)
                    e.printStackTrace()
                    return null
                }
                if (!jarEntry.isDirectory() && entryName.endsWith(".class")) {
                    className = entryName.replace("/", ".").replace(".class", "")
                    ThinkingClassNameAnalytics classNameAnalytics = mThinkingTransformHelper.analytics(className)
                    if (classNameAnalytics.isShouldModify) {
                        modifiedClassBytes = modifyClass(sourceClassBytes, classNameAnalytics)
                    }
                }
                if (modifiedClassBytes == null) {
                    jarOutputStream.write(sourceClassBytes)
                } else {
                    jarOutputStream.write(modifiedClassBytes)
                }
                jarOutputStream.closeEntry()
            }
        }
        jarOutputStream.close()
        file.close()
        return outputJar
    }

    private void beforeBuild(TransformInvocation invocation) {
        LoggerUtil.setDebug(mThinkingTransformHelper.extension.debug)
        mThinkingTransformHelper.beforeTransform()
        LoggerUtil.info("是否开启多线程编译:${mThinkingTransformHelper.enableTAMultiThread}")
        LoggerUtil.info("是否开启增量编译:${mThinkingTransformHelper.enableTAIncremental}")
        LoggerUtil.info("此次是否增量编译:$invocation.incremental")
        LoggerUtil.info("是否在方法进入时插入代码:${mThinkingTransformHelper.isAddOnMethodEnter}")
        LoggerUtil.info("是否启用隐私代码隔离:${mThinkingTransformHelper.enableTASensitiveInfoFilter}")

        traverse(invocation)
    }

    private void traverse(TransformInvocation invocation) {
        def urlList = []
        def androidJar = mThinkingTransformHelper.androidJar()
        urlList << androidJar.toURI().toURL()
        invocation.inputs.each { transformInput ->
            transformInput.jarInputs.each { jarInput ->
                urlList << jarInput.getFile().toURI().toURL()
            }

            transformInput.directoryInputs.each { directoryInput ->
                urlList << directoryInput.getFile().toURI().toURL()
            }
        }
        def urlArray = urlList as URL[]
        urlClassLoader = new URLClassLoader(urlArray)
        mThinkingTransformHelper.mUrlClassLoader = urlClassLoader
        //checkThinkingSDKVersion()
        checkThinkingSDKPath()
    }

    /**
     * 检查sdk版本好是否符合要求
     */
    private void checkThinkingSDKVersion() {
        ThinkingVersionUtils.loadAndroidSDKVersion(urlClassLoader)
        if (ThinkingAnalyticsUtil.compareVersion(ThinkingVersionUtils.MIN_SDK_VERSION, ThinkingVersionUtils.thinkingSDKVersion) > 0) {
            String errMessage = ""
            if (ThinkingVersionUtils.thinkingSDKVersion.isEmpty()) {
                errMessage = "你目前还未集成ThinkingSDK，请接入 v${ThinkingVersionUtils.MIN_SDK_VERSION} 及以上的版本。"
            } else {
                errMessage = "你目前集成的TA埋点 SDK 版本号为 v${ThinkingVersionUtils.thinkingSDKVersion}，请升级到 v${ThinkingVersionUtils.MIN_SDK_VERSION} 及以上的版本。"
            }
            LoggerUtil.error(errMessage)
            throw new Error(errMessage)
        }
    }

    private void checkThinkingSDKPath() {
        try {
            Class sdkClazz = urlClassLoader.loadClass("cn.thinkingdata.android.ThinkingAnalyticsSDK")
            ProtectionDomain pd = sdkClazz.getProtectionDomain()
            CodeSource cs = pd.getCodeSource()
            thinkingSdkJarPath = cs.getLocation().toURI().getPath()
        } catch (Throwable throwable) {
            LoggerUtil.error("Can not load 'cn.thinkingdata.android.ThinkingAnalyticsSDK' class: ${throwable.localizedMessage}")
        }
    }

    private void afterBuild() {
        try {
            if (urlClassLoader != null) {
                urlClassLoader.close()
                urlClassLoader = null
            }
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    /**
     * 遍历目录
     * @param isIncremental
     * @param directoryInput
     * @param outputProvider
     * @param context
     */
    void traversalDirectory(boolean isIncremental, DirectoryInput directoryInput, TransformOutputProvider outputProvider, Context context) {
        File dir = directoryInput.file
        File dest = outputProvider.getContentLocation(directoryInput.getName(),
                directoryInput.getContentTypes(), directoryInput.getScopes(),
                Format.DIRECTORY)
        FileUtils.forceMkdir(dest)
        String srcDirPath = dir.absolutePath
        String destDirPath = dest.absolutePath
        if (isIncremental) {
            Map<File, Status> fileStatusMap = directoryInput.getChangedFiles()
            for (Map.Entry<File, Status> changedFile : fileStatusMap.entrySet()) {
                Status status = changedFile.getValue()
                File inputFile = changedFile.getKey()
                String destFilePath = inputFile.absolutePath.replace(srcDirPath, destDirPath)
                File destFile = new File(destFilePath)
                switch (status) {
                    case Status.NOTCHANGED:
                        break
                    case Status.REMOVED:
                        if (destFile.exists()) {
                            destFile.delete()
                        }
                        break
                    case Status.ADDED:
                    case Status.CHANGED:
                        File modified = modifyClassFile(dir, inputFile, context.getTemporaryDir())
                        if (destFile.exists()) {
                            destFile.delete()
                        }
                        if (modified != null) {
                            FileUtils.copyFile(modified, destFile)
                            modified.delete()
                        } else {
                            FileUtils.copyFile(inputFile, destFile)
                        }
                        break
                    default:
                        break
                }
            }
        } else {
            FileUtils.copyDirectory(dir, dest)
            dir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) {
                File inputFile ->
                    traversalDir(dir, inputFile, context, srcDirPath, destDirPath)
            }
        }
    }

    void traversalDir(File dir, File inputFile, Context context, String srcDirPath, String destDirPath) {
        File modified = modifyClassFile(dir, inputFile, context.getTemporaryDir())
        if (modified != null) {
            File target = new File(inputFile.absolutePath.replace(srcDirPath, destDirPath))
            if (target.exists()) {
                target.delete()
            }
            FileUtils.copyFile(modified, target)
            modified.delete()
        }
    }

    /**
     * 此方法真正修改
     */
    private byte[] modifyClass(byte[] srcClass, ThinkingClassNameAnalytics classNameAnalytics) {
        try {
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
            ClassVisitor classVisitor = new ThinkingAnalyticsClassVisitor(classWriter, classNameAnalytics, mThinkingTransformHelper)
            ClassReader cr = new ClassReader(srcClass)
            //cr.accept(classVisitor, ClassReader.EXPAND_FRAMES + ClassReader.SKIP_FRAMES)
            cr.accept(classVisitor, ClassReader.EXPAND_FRAMES)
            return classWriter.toByteArray()
        } catch (Exception ex) {
            LoggerUtil.error("$classNameAnalytics.className 类执行 modifyClass 方法出现异常")
            ex.printStackTrace()
            if (mThinkingTransformHelper.extension.debug) {
                throw new Error()
            }
            return srcClass
        }
    }

    /**
     * 修改目录文件
     */
    private File modifyClassFile(File dir, File classFile, File tempDir) {
        File modified = null
        FileOutputStream outputStream = null
        try {
            String className = path2ClassName(classFile.absolutePath.replace(dir.absolutePath + File.separator, ""))
            ThinkingClassNameAnalytics classNameAnalytics = mThinkingTransformHelper.analytics(className)
            if (classNameAnalytics.isShouldModify) {
                byte[] sourceClassBytes = ThinkingAnalyticsUtil.toByteArrayStream(new FileInputStream(classFile))
                byte[] modifiedClassBytes = modifyClass(sourceClassBytes, classNameAnalytics)
                if (modifiedClassBytes) {
                    modified = new File(tempDir, UUID.randomUUID().toString() + '.class')
                    if (modified.exists()) {
                        modified.delete()
                    }
                    modified.createNewFile()
                    outputStream = new FileOutputStream(modified)
                    outputStream.write(modifiedClassBytes)
                }
            } else {
                return classFile
            }
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            IOUtils.closeQuietly(outputStream)
        }
        return modified
    }

    private static String path2ClassName(String pathName) {
        pathName.replace(File.separator, ".").replace(".class", "")
    }

}