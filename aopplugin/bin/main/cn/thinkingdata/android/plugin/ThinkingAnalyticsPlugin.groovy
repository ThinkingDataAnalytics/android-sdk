package cn.thinkingdata.android.plugin

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.AppPlugin
import cn.thinkingdata.android.plugin.internal.TimeTrace
import org.gradle.api.Plugin
import org.gradle.api.Project

class ThinkingAnalyticsPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.repositories {
            mavenLocal()
        }

        project.dependencies {
            if (project.gradle.gradleVersion > "4.0") {
                project.logger.debug("gradlew version > 4.0")
                implementation 'org.aspectj:aspectjrt:1.8.13'
                implementation 'cn.thinkingdata.android:ThinkingAnalyticsRuntime:2.0.0'
            } else {
                project.logger.debug("gradlew version < 4.0")
                compile 'org.aspectj:aspectjrt:1.8.13'
                compile 'cn.thinkingdata.android:ThinkingAnalyticsRuntime:2.0.0'
            }
        }

        project.extensions.create("thinkingAnalytics", ThinkingAnalyticsExtension)

        if (project.plugins.hasPlugin(AppPlugin)) {
            project.gradle.addListener(new TimeTrace())

            //register AspectTransform
            AppExtension android = project.extensions.getByType(AppExtension)
            android.registerTransform(new ThinkingAnalyticsTransform(project))
        }
    }
}
