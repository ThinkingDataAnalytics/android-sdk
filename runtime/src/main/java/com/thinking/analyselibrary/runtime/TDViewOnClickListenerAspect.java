package com.thinking.analyselibrary.runtime;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Aspect
public class TDViewOnClickListenerAspect {

    @Pointcut("execution(@butterknife.OnClick * *(..))")
    public void methodAnnotatedWithButterknifeClick() {
    }

    @After("methodAnnotatedWithButterknifeClick()")
    public void onButterknifeClickAOP(final JoinPoint joinPoint) throws Throwable {
        TDAopUtil.sendTrackEventToSDK(joinPoint, "onButterknifeClick");
    }

    /**
     * android.view.View.OnClickListener.onClick(android.view.View)
     *
     * @param joinPoint JoinPoint
     * @throws Throwable Exception
     */
    @After("execution(* android.view.View.OnClickListener.onClick(android.view.View)) && args(view)")
    public void onViewClickAOP(final JoinPoint joinPoint, android.view.View view) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        if (null == methodSignature) return;

        Method method = methodSignature.getMethod();
        if (null == method) return;

        Annotation annotation = null;

        Class clazz = null;
        try {
            clazz = Class.forName("com.thinking.analyselibrary.ThinkingDataIgnoreTrackOnClick");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (null != clazz) {
            annotation = method.getAnnotation(clazz);
        }
        TDAopUtil.sendTrackEventToSDK("onViewOnClick", view, annotation);
    }

    /**
     * android.view.View.OnLongClickListener.onLongClick(android.view.View)
     *
     * @param joinPoint JoinPoint
     * @throws Throwable Exception
     */
    @After("execution(* android.view.View.OnLongClickListener.onLongClick(android.view.View))")
    public void onViewLongClickAOP(JoinPoint joinPoint) throws Throwable {

    }
}

