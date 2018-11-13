package com.thinkingdata.analytics.android.plugin;

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class ThinkingAnalyticsPlugin implements Plugin<Project> {
    @Override void apply(Project project) {

        project.repositories.flatDir { dirs 'libs' }

        final def log = project.logger

        project.dependencies {
            compile 'org.aspectj:aspectjrt:1.8.10'
//            compile 'com.thinking.analyselibrary:ThinkingAnalyticsRuntime:1.0.5'
        }

        project.extensions.create("thinkingAnalytics", ThinkingAnalyticsExtension)

        project.android.applicationVariants.all { variant ->
            JavaCompile javaCompile = variant.javaCompile

            javaCompile.doLast {
                String[] args = [
                        "-showWeaveInfo",
                        "-1.7",
                        "-inpath", javaCompile.destinationDir.toString(),
                        "-aspectpath", javaCompile.classpath.asPath,
                        "-d", javaCompile.destinationDir.toString(),
                        "-classpath", javaCompile.classpath.asPath,
                        "-bootclasspath", project.android.bootClasspath.join(File.pathSeparator)
                ]
                MessageHandler handler = new MessageHandler(true);
                new Main().run(args, handler);

                for (IMessage message : handler.getMessages(null, true)) {
                    switch (message.getKind()) {
                        case IMessage.ABORT:
                        case IMessage.ERROR:
                        case IMessage.FAIL:
                            log.error message.message, message.thrown
                            break;
                        case IMessage.WARNING:
                            log.warn message.message, message.thrown
                            break;
                        case IMessage.INFO:
                            log.info message.message, message.thrown
                            break;
                        case IMessage.DEBUG:
                            log.debug message.message, message.thrown
                            break;
                    }
                }
            }
        }
    }
}
