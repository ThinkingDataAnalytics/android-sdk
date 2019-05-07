package com.thinkingdata.analytics.android.plugin

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.gradle.internal.pipeline.TransformTask
import com.google.common.collect.ImmutableSet
import com.thinkingdata.analytics.android.plugin.internal.cache.VariantCache
import com.thinkingdata.analytics.android.plugin.internal.procedure.CacheAspectFilesProcedure
import com.thinkingdata.analytics.android.plugin.internal.procedure.CacheInputFilesProcedure
import com.thinkingdata.analytics.android.plugin.internal.procedure.CheckAspectJXEnableProcedure
import com.thinkingdata.analytics.android.plugin.internal.procedure.DoAspectWorkProcedure
import com.thinkingdata.analytics.android.plugin.internal.procedure.OnFinishedProcedure
import com.thinkingdata.analytics.android.plugin.internal.procedure.TDProcedure
import com.thinkingdata.analytics.android.plugin.internal.procedure.UpdateAspectFilesProcedure
import com.thinkingdata.analytics.android.plugin.internal.procedure.UpdateAspectOutputProcedure
import com.thinkingdata.analytics.android.plugin.internal.procedure.UpdateInputFilesProcedure
import org.gradle.api.Project

class ThinkingAnalyticsTransform extends Transform {

    TDProcedure tdProcedure

    ThinkingAnalyticsTransform(Project p) {
        tdProcedure = new TDProcedure(p)
    }

    @Override
    String getName() {
        return "tda"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return ImmutableSet.<QualifiedContent.ContentType>of(QualifiedContent.DefaultContentType.CLASSES)
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        //是否支持增量编译
        return true
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {

        Project project = tdProcedure.project

        TransformTask transformTask = (TransformTask)transformInvocation.context
        VariantCache variantCache = new VariantCache(tdProcedure.project, tdProcedure.tdCache, transformTask.variantName)

        tdProcedure.with(new CheckAspectJXEnableProcedure(project, variantCache, transformInvocation))

        if (transformInvocation.incremental) {
            //incremental build
            tdProcedure.with(new UpdateAspectFilesProcedure(project, variantCache, transformInvocation))
            tdProcedure.with(new UpdateInputFilesProcedure(project, variantCache, transformInvocation))
            tdProcedure.with(new UpdateAspectOutputProcedure(project, variantCache, transformInvocation))
        } else {
            //delete output and cache before full build
            transformInvocation.outputProvider.deleteAll()
            variantCache.reset()

            tdProcedure.with(new CacheAspectFilesProcedure(project, variantCache, transformInvocation))
            tdProcedure.with(new CacheInputFilesProcedure(project, variantCache, transformInvocation))
            tdProcedure.with(new DoAspectWorkProcedure(project, variantCache, transformInvocation))
        }

        tdProcedure.with(new OnFinishedProcedure(project, variantCache, transformInvocation))

        tdProcedure.doWorkContinuously()
    }
}
