package com.thinking.analyselibrary.runtime;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class TDTrackEventAspectj {
    private final static String TAG = TDTrackEventAspectj.class.getCanonicalName();

    @Pointcut("execution(@com.thinking.analyselibrary.ThinkingDataTrackEvent * *(..))")
    public void methodAnnotatedWithTrackEvent() {
    }

    @After("methodAnnotatedWithTrackEvent()")
    public void trackEventAOP(final JoinPoint joinPoint) throws Throwable {
        TDAopUtil.sendTrackEventToSDK(joinPoint, "trackEventAOP");
    }
}
