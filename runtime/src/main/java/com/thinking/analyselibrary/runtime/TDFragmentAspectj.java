package com.thinking.analyselibrary.runtime;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TDFragmentAspectj {
    private final static String TAG = TDFragmentAspectj.class.getCanonicalName();

    @Around("execution(* android.support.v4.app.Fragment.onCreateView(..))")
    public Object fragmentOnCreateViewMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        return trackFragmentView(joinPoint);
    }

    @Around("execution(* android.app.Fragment.onCreateView(..))")
    public Object fragmentOnCreateViewMethod2(ProceedingJoinPoint joinPoint) throws Throwable {
        return trackFragmentView(joinPoint);
    }

    private Object trackFragmentView(final ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        TDAopUtil.sendTrackEventToSDK3(joinPoint, "trackFragmentView", result);
        return result;
    }

    @After("execution(* android.support.v4.app.Fragment.onHiddenChanged(boolean))")
    public void onHiddenChangedMethod(JoinPoint joinPoint) throws Throwable {
        TDAopUtil.sendTrackEventToSDK(joinPoint, "onFragmentHiddenChangedMethod");
    }

    @After("execution(* android.support.v4.app.Fragment.setUserVisibleHint(boolean))")
    public void setUserVisibleHintMethod(JoinPoint joinPoint) throws Throwable {
        TDAopUtil.sendTrackEventToSDK(joinPoint, "onFragmentSetUserVisibleHintMethod");
    }

    @After("execution(* android.support.v4.app.Fragment.onResume())")
    public void onResumeMethod(JoinPoint joinPoint) throws Throwable {
        TDAopUtil.sendTrackEventToSDK(joinPoint, "onFragmentOnResumeMethod");
    }
}

