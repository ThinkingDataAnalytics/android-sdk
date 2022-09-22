package cn.thinkingdata.android.plugin.internal.procedure

import cn.thinkingdata.android.plugin.ThinkingAnalyticsExtension
import cn.thinkingdata.android.plugin.ThinkingAnalyticsConfig
import cn.thinkingdata.android.plugin.internal.cache.TDCache
import org.aspectj.weaver.Dump
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class TDProcedure extends AbstractProcedure {

    Project project
    TDCache tdCache

    TDProcedure(Project p) {
        super(p, null, null)

        project = p
        tdCache = new TDCache(project)

        System.setProperty("aspectj.multithreaded", "true")

        def configuration = new ThinkingAnalyticsConfig(project)

        project.afterEvaluate {
            configuration.variants.all { variant ->
                JavaCompile javaCompile = variant.hasProperty('javaCompiler') ? variant.javaCompileProvider.get() : variant.javaCompile
                tdCache.encoding = javaCompile.options.encoding
                tdCache.bootClassPath = configuration.bootClasspath.join(File.pathSeparator)
                tdCache.sourceCompatibility = javaCompile.sourceCompatibility
                tdCache.targetCompatibility = javaCompile.targetCompatibility
            }

            ThinkingAnalyticsExtension tdExtension = project.thinkingAnalytics
            //当过滤条件发生变化，clean掉编译缓存
            if (tdCache.isExtensionChanged(tdExtension)) {
                project.tasks.findByName('preBuild').dependsOn(project.tasks.findByName("clean"))
            }

            tdCache.putExtensionConfig(tdExtension)

            tdCache.ajcArgs = tdExtension.ajcArgs
        }

        //set aspectj build log output dir
        File logDir = new File(project.buildDir.absolutePath + File.separator + "outputs" + File.separator + "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        Dump.setDumpDirectory(logDir.absolutePath)
    }
}
