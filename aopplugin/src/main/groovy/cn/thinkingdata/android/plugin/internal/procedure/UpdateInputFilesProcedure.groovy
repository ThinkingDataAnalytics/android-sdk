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
import cn.thinkingdata.android.plugin.internal.cache.VariantCache
import cn.thinkingdata.android.plugin.internal.concurrent.BatchTaskScheduler
import cn.thinkingdata.android.plugin.internal.concurrent.ITask
import cn.thinkingdata.android.plugin.internal.TDUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-04-23
 */
class UpdateInputFilesProcedure extends AbstractProcedure {

    UpdateInputFilesProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
        super(project, variantCache, transformInvocation)
    }

    @Override
    boolean doWorkContinuously() {
        project.logger.debug("~~~~~~~~~~~~~~~~~~~~update input files")
        BatchTaskScheduler taskScheduler = new BatchTaskScheduler()

        transformInvocation.inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput dirInput ->
                taskScheduler.addTask(new ITask() {
                    @Override
                    Object call() throws Exception {
                        dirInput.changedFiles.each { File file, Status status ->
                            project.logger.debug("~~~~~~~~~~~~~~~~changed file::${status.name()}::${file.absolutePath}")

                            variantCache.includeFileContentTypes = dirInput.contentTypes
                            variantCache.includeFileScopes = dirInput.scopes

                            String path = file.absolutePath
                            String subPath = path.substring(dirInput.file.absolutePath.length())
                            String transPath = subPath.replace(File.separator, ".")

                            boolean isInclude = TDUtils.isIncludeFilterMatched(transPath, tdExtensionConfig.includes) \
                                        && !TDUtils.isExcludeFilterMatched(transPath, tdExtensionConfig.excludes)

                            if (!variantCache.incrementalStatus.isIncludeFileChanged && isInclude) {
                                variantCache.incrementalStatus.isIncludeFileChanged = isInclude
                            }

                            if (!variantCache.incrementalStatus.isExcludeFileChanged && !isInclude) {
                                variantCache.incrementalStatus.isExcludeFileChanged = !isInclude
                            }

                            File target = new File((isInclude ? variantCache.includeFilePath : variantCache.excludeFilePath) + subPath)
                            switch (status) {
                                case Status.REMOVED:
                                    FileUtils.deleteQuietly(target)
                                    break
                                case Status.CHANGED:
                                    FileUtils.deleteQuietly(target)
                                    variantCache.add(file, target)
                                    break
                                case Status.ADDED:
                                    variantCache.add(file, target)
                                    break
                                default:
                                    break
                            }
                        }
                        //??????include files ????????????????????????include??????jar
                        if (variantCache.incrementalStatus.isIncludeFileChanged) {
                            File includeOutputJar = transformInvocation.outputProvider.getContentLocation("include", variantCache.contentTypes,
                                    variantCache.scopes, Format.JAR)
                            FileUtils.deleteQuietly(includeOutputJar)
                        }

                        //??????exclude files??????????????????????????????exclude jar???????????????
                        if (variantCache.incrementalStatus.isExcludeFileChanged) {
                            File excludeOutputJar = transformInvocation.outputProvider.getContentLocation("exclude", variantCache.contentTypes,
                                    variantCache.scopes, Format.JAR)
                            FileUtils.deleteQuietly(excludeOutputJar)
                            TDUtils.mergeJar(variantCache.excludeFileDir, excludeOutputJar)
                        }

                        return null
                    }
                })
            }

            input.jarInputs.each { JarInput jarInput ->
                if (jarInput.status != Status.NOTCHANGED) {
                    taskScheduler.addTask(new ITask() {
                        @Override
                        Object call() throws Exception {
                            project.logger.debug("~~~~~~~changed file::${jarInput.status.name()}::${jarInput.file.absolutePath}")

                            String filePath = jarInput.file.absolutePath
                            File outputJar = transformInvocation.outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)

                            if (jarInput.status == Status.REMOVED) {
                                variantCache.removeIncludeJar(filePath)
                                FileUtils.deleteQuietly(outputJar)
                            } else if (jarInput.status == Status.ADDED) {
                                TDUtils.filterJar(jarInput, variantCache, tdExtensionConfig.includes, tdExtensionConfig.excludes)
                            } else if (jarInput.status == Status.CHANGED) {
                                FileUtils.deleteQuietly(outputJar)
                            }

                            //???????????????AOP?????????????????????copy???????????????
                            if (!variantCache.isIncludeJar(filePath)) {
                                FileUtils.copyFile(jarInput.file, outputJar)
                            }
                            return null
                        }
                    })
                }
            }
        }

        taskScheduler.execute()

        variantCache.commitIncludeJarConfig()

        return true
    }
}
