package cn.thinkingdata.android.plugin

import cn.thinkingdata.android.plugin.config.ThinkingAnalyticsExtension
import cn.thinkingdata.android.plugin.config.ThinkingAnalyticsTransformHelper
import cn.thinkingdata.android.plugin.hook.ThinkingAnalyticsTransform
import cn.thinkingdata.android.plugin.utils.LoggerUtil
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
        Instantiator itt = ((DefaultGradle) target.getGradle()).getServices().get(Instantiator)
        def arguments = [itt] as Object[]
        ThinkingAnalyticsExtension extension = target.extensions.create("ThinkingAnalytics", ThinkingAnalyticsExtension, arguments)
        Map<String, ?> properties = target.getProperties()
        //是否开启插件 默认true开启
        boolean isTAPluginEnable = Boolean.parseBoolean(properties.getOrDefault("ta.enablePlugin", "true"))
        //是否允许多线程编译  默认true开启
        boolean isTAMultiBuildEnable = Boolean.parseBoolean(properties.getOrDefault("ta.enableMultiThreadBuild", "true"))
        //是否支持增量编译 默认true开启
        boolean isTAIncrementalBuildEnable = Boolean.parseBoolean(properties.getOrDefault("ta.enableIncrementalBuild", "true"))
        //是否在方法进入的时候插入 默认false 在方法最后插入
        boolean isAddOnMethodEnter = Boolean.parseBoolean(properties.getOrDefault("ta.isAddOnMethodEnter", "false"))

        if (isTAPluginEnable) {
            AppExtension appExtension = target.extensions.findByType(AppExtension.class)
            ThinkingAnalyticsTransformHelper transformHelper = new ThinkingAnalyticsTransformHelper(extension, appExtension)
            transformHelper.enableTAIncremental = isTAIncrementalBuildEnable
            transformHelper.enableTAMultiThread = isTAMultiBuildEnable
            transformHelper.isAddOnMethodEnter = isAddOnMethodEnter
            appExtension.registerTransform(new ThinkingAnalyticsTransform(transformHelper))
        }else{
            LoggerUtil.error("------------您已关闭了Thinking插件--------------")
        }

    }
}