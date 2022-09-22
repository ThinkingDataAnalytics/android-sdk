/*
 * Copyright 2018 firefly1126, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.gradle_plugin_android_aspectjx
 */
package cn.thinkingdata.android.plugin.internal.procedure


import com.android.build.api.transform.*
import cn.thinkingdata.android.plugin.internal.TDUtils
import cn.thinkingdata.android.plugin.internal.cache.VariantCache
import cn.thinkingdata.android.plugin.internal.concurrent.BatchTaskScheduler
import cn.thinkingdata.android.plugin.internal.concurrent.ITask
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-04-23
 */
class CacheInputFilesProcedure extends AbstractProcedure {
    CacheInputFilesProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
        super(project, variantCache, transformInvocation)
    }

    @Override
    boolean doWorkContinuously() {
        //过滤规则
        //
        // "*" 所有class文件和jar
        // "**" 所有class文件和jar
        // "com.hujiang" 过滤 含"com.hujiang"的文件和jar
        //
        project.logger.debug("~~~~~~~~~~~~~~~~~~~~cache input files")
        BatchTaskScheduler taskScheduler = new BatchTaskScheduler()

        transformInvocation.inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput dirInput ->
                variantCache.includeFileContentTypes = dirInput.contentTypes
                variantCache.includeFileScopes = dirInput.scopes

                taskScheduler.addTask(new ITask() {
                    @Override
                    Object call() throws Exception {
                        dirInput.file.eachFileRecurse { File item ->
                            if (TDUtils.isClassFile(item)) {
                                String path = item.absolutePath
                                String subPath = path.substring(dirInput.file.absolutePath.length())
                                String transPath = subPath.replace(File.separator, ".")

                                boolean isInclude = TDUtils.isIncludeFilterMatched(transPath, tdExtensionConfig.includes) &&
                                        !TDUtils.isExcludeFilterMatched(transPath, tdExtensionConfig.excludes)
                                variantCache.add(item, new File((isInclude ? variantCache.includeFilePath : variantCache.excludeFilePath) + subPath))
                            }
                        }

                        //put exclude files into jar
                        if (TDUtils.countOfFiles(variantCache.excludeFileDir) > 0) {
                            File excludeJar = transformInvocation.getOutputProvider().getContentLocation("exclude", variantCache.contentTypes,
                                    variantCache.scopes, Format.JAR)
                            TDUtils.mergeJar(variantCache.excludeFileDir, excludeJar)
                        }

                        return null
                    }
                })
            }

            input.jarInputs.each { JarInput jarInput ->
                taskScheduler.addTask(new ITask() {
                    @Override
                    Object call() throws Exception {
                        TDUtils.filterJar(jarInput, variantCache, tdExtensionConfig.includes,tdExtensionConfig.excludes)
                        if (!variantCache.isIncludeJar(jarInput.file.absolutePath)) {
                            def dest = transformInvocation.outputProvider.getContentLocation(jarInput.name
                                    , jarInput.contentTypes
                                    , jarInput.scopes
                                    , Format.JAR)
                            FileUtils.copyFile(jarInput.file, dest)
                        }

                        return null
                    }
                })

            }
        }

        taskScheduler.execute()

        variantCache.commitIncludeJarConfig()

        return true
    }
}
