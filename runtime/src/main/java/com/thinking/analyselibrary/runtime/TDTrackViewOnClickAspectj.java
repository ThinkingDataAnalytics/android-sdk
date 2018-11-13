package com.thinking.analyselibrary.runtime;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class TDTrackViewOnClickAspectj {
    private final static String TAG = TDTrackViewOnClickAspectj.class.getCanonicalName();

    @Pointcut("execution(@com.thinking.analyselibrary.ThinkingDataTrackViewOnClick * *(..))")
    public void methodAnnotatedWithTrackEvent() {
    }

    @After("methodAnnotatedWithTrackEvent()")
    public void trackOnClickAOP(final JoinPoint joinPoint) throws Throwable {
        TDAopUtil.sendTrackEventToSDK(joinPoint, "trackViewOnClick");
    }
}
