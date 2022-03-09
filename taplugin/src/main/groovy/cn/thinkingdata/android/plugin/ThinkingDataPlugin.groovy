package cn.thinkingdata.android.plugin

import cn.thinkingdata.android.plugin.config.ThinkingAnalyticsExtension
import cn.thinkingdata.android.plugin.config.ThinkingAnalyticsTransformHelper
import cn.thinkingdata.android.plugin.hook.ThinkingAnalyticsTransform
import cn.thinkingdata.android.plugin.utils.Logger
import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.reflect.Instantiator
import org.gradle.invocation.DefaultGradle

/**
 * 插件入口
 */
class ThinkingDataPlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        Instantiator ins = ((DefaultGradle) target.getGradle()).getServices().get(Instantiator)
        def args = [ins] as Object[]
        ThinkingAnalyticsExtension extension = target.extensions.create("ThinkingAnalytics", ThinkingAnalyticsExtension, args)
        Map<String, ?> properties = target.getProperties()
        boolean disableSensorsAnalyticsPlugin = Boolean.parseBoolean(properties.getOrDefault("thinkingAnalytics.disablePlugin", "false")) ||
                Boolean.parseBoolean(properties.getOrDefault("disableThinkingAnalyticsPlugin", "false"))
        boolean disableSensorsAnalyticsMultiThreadBuild = Boolean.parseBoolean(properties.getOrDefault("thinkingAnalytics.disableMultiThreadBuild", "false"))
        boolean disableSensorsAnalyticsIncrementalBuild = Boolean.parseBoolean(properties.getOrDefault("thinkingAnalytics.disableIncrementalBuild", "false"))
        boolean isHookOnMethodEnter = Boolean.parseBoolean(properties.getOrDefault("thinkingAnalytics.isHookOnMethodEnter", "false"))

        if (!disableSensorsAnalyticsPlugin) {
            AppExtension appExtension = target.extensions.findByType(AppExtension.class)
            ThinkingAnalyticsTransformHelper transformHelper = new ThinkingAnalyticsTransformHelper(extension, appExtension)
            transformHelper.disableSensorsAnalyticsIncremental = disableSensorsAnalyticsIncrementalBuild
            transformHelper.disableSensorsAnalyticsMultiThread = disableSensorsAnalyticsMultiThreadBuild
            transformHelper.isHookOnMethodEnter = isHookOnMethodEnter
            appExtension.registerTransform(new ThinkingAnalyticsTransform(transformHelper))
        }else{
            Logger.error("------------您已关闭了Thinking插件--------------")
        }

    }
}