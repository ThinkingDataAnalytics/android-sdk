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

import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import cn.thinkingdata.android.plugin.internal.TDTask
import cn.thinkingdata.android.plugin.internal.TDTaskManager
import cn.thinkingdata.android.plugin.internal.cache.VariantCache
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-04-23
 */
class UpdateAspectOutputProcedure extends AbstractProcedure {
    TDTaskManager tdTaskManager

    UpdateAspectOutputProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
        super(project, variantCache, transformInvocation)
        tdTaskManager = new TDTaskManager(encoding: tdCache.encoding, ajcArgs: tdCache.ajcArgs, bootClassPath: tdCache.bootClassPath,
                            sourceCompatibility: tdCache.sourceCompatibility, targetCompatibility: tdCache.targetCompatibility)
    }

    @Override
    boolean doWorkContinuously() {
        project.logger.debug("~~~~~~~~~~~~~~~~~~~~update aspect output")
        tdTaskManager.aspectPath << variantCache.aspectDir
        tdTaskManager.classPath << variantCache.includeFileDir
        tdTaskManager.classPath << variantCache.excludeFileDir

        if (variantCache.incrementalStatus.isAspectChanged || variantCache.incrementalStatus.isIncludeFileChanged) {
            //process class files
            TDTask tdTask = new TDTask(project)
            File outputJar = transformInvocation.getOutputProvider().getContentLocation("include", variantCache.contentTypes,
                    variantCache.scopes, Format.JAR)
            FileUtils.deleteQuietly(outputJar)

            tdTask.outputJar = outputJar.absolutePath
            tdTask.inPath << variantCache.includeFileDir

            tdTaskManager.addTask(tdTask)
        }

        transformInvocation.inputs.each { TransformInput input ->
            input.jarInputs.each { JarInput jarInput ->
                tdTaskManager.classPath << jarInput.file
                File outputJar = transformInvocation.getOutputProvider().getContentLocation(jarInput.name, jarInput.getContentTypes(),
                        jarInput.getScopes(), Format.JAR)

                if (!outputJar.getParentFile()?.exists()) {
                    outputJar.getParentFile()?.mkdirs()
                }

                if (variantCache.isIncludeJar(jarInput.file.absolutePath)) {
                    if (variantCache.incrementalStatus.isAspectChanged) {
                        FileUtils.deleteQuietly(outputJar)

                        TDTask ajxTask1 = new TDTask(project)
                        ajxTask1.inPath << jarInput.file

                        ajxTask1.outputJar = outputJar.absolutePath

                        tdTaskManager.addTask(ajxTask1)
                    } else {
                        if (!outputJar.exists()) {
                            TDTask ajxTask1 = new TDTask(project)
                            ajxTask1.inPath << jarInput.file

                            ajxTask1.outputJar = outputJar.absolutePath

                            tdTaskManager.addTask(ajxTask1)
                        }
                    }
                }
            }
        }

        tdTaskManager.batchExecute()

        return true
    }
}
