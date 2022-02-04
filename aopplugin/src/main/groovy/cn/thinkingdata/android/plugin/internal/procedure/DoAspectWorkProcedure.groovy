package cn.thinkingdata.android.plugin.internal.procedure

import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import cn.thinkingdata.android.plugin.internal.TDTask
import cn.thinkingdata.android.plugin.internal.TDTaskManager
import cn.thinkingdata.android.plugin.internal.cache.VariantCache
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

class DoAspectWorkProcedure extends AbstractProcedure {
    TDTaskManager tdTaskManager

    DoAspectWorkProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
        super(project, variantCache, transformInvocation)
        tdTaskManager = new TDTaskManager(encoding: tdCache.encoding, ajcArgs: tdCache.ajcArgs, bootClassPath: tdCache.bootClassPath,
                sourceCompatibility: tdCache.sourceCompatibility, targetCompatibility: tdCache.targetCompatibility)
    }

    @Override
    boolean doWorkContinuously() {
        //do aspectj real work
        project.logger.debug("~~~~~~~~~~~~~~~~~~~~do aspectj real work")
        tdTaskManager.aspectPath << variantCache.aspectDir
        tdTaskManager.classPath << variantCache.includeFileDir
        tdTaskManager.classPath << variantCache.excludeFileDir

        //process class files
        TDTask tdTask = new TDTask(project)
        File includeJar = transformInvocation.getOutputProvider().getContentLocation("include", variantCache.contentTypes,
                variantCache.scopes, Format.JAR)

        if (!includeJar.parentFile.exists()) {
            FileUtils.forceMkdir(includeJar.getParentFile())
        }

        FileUtils.deleteQuietly(includeJar)

        tdTask.outputJar = includeJar.absolutePath
        tdTask.inPath << variantCache.includeFileDir
        tdTaskManager.addTask(tdTask)

        //process jar files
        transformInvocation.inputs.each { TransformInput input ->
            input.jarInputs.each { JarInput jarInput ->
                tdTaskManager.classPath << jarInput.file

                if (variantCache.isIncludeJar(jarInput.file.absolutePath)) {
                    TDTask tdTask1 = new TDTask(project)
                    tdTask1.inPath << jarInput.file

                    File outputJar = transformInvocation.getOutputProvider().getContentLocation(jarInput.name, jarInput.getContentTypes(),
                            jarInput.getScopes(), Format.JAR)
                    if (!outputJar.getParentFile()?.exists()) {
                        outputJar.getParentFile()?.mkdirs()
                    }

                    tdTask1.outputJar = outputJar.absolutePath

                    tdTaskManager.addTask(tdTask1)
                }
            }
        }

        tdTaskManager.batchExecute()

        return true
    }

}
