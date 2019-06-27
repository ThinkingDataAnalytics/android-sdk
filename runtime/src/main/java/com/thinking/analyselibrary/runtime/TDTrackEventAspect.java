package com.thinking.analyselibrary.runtime;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Aspect
public class TDTrackEventAspect {

    @After("execution(@com.thinking.analyselibrary.ThinkingDataTrackEvent * *(..))")
    public void trackEvent(final JoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        if (null == methodSignature) return;

        Method method = methodSignature.getMethod();
        if (null != method) {
            Class clazz = null;
            try {
                clazz = Class.forName("com.thinking.analyselibrary.ThinkingDataTrackEvent");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            if (null == clazz) {
                return;
            }

            final Annotation trackEvent = method.getAnnotation(clazz);

            TDAopUtil.sendTrackEventToSDK("trackEvent", trackEvent);
        }
    }
}
