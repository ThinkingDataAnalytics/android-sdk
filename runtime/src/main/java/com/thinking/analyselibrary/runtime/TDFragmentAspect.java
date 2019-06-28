package com.thinking.analyselibrary.runtime;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TDFragmentAspect {

    @Around("execution(* android.support.v4.app.Fragment.onCreateView(..))||" +
            "execution(* androidx.fragment.app.Fragment.onCreateView(..))||" +
            "execution(* android.app.Fragment.onCreateView(..))")
    public Object onCreateViewMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        if (null != result) {
            TDAopUtil.sendTrackEventToSDK("onFragmentCreateView", joinPoint.getTarget(), result);
        }
        return result;
    }

    @After("call(* android.support.v4.app.Fragment.onResume())||" +
            "call(* androidx.fragment.app.Fragment.onResume())")
    public void onResumeMethod(JoinPoint joinPoint) {
        TDAopUtil.sendTrackEventToSDK("onFragmentOnResume", joinPoint.getTarget());
    }

    @After("execution(* android.support.v4.app.Fragment.onHiddenChanged(boolean))||" +
            "execution(* androidx.fragment.app.Fragment.onHiddenChanged(boolean))")
    public void onHiddenChangedMethod(JoinPoint joinPoint) {
        TDAopUtil.sendTrackEventToSDK("onFragmentHiddenChanged", joinPoint.getTarget(), joinPoint.getArgs()[0]);
    }

    @After("execution(* android.support.v4.app.Fragment.setUserVisibleHint(boolean))||" +
            "execution(* androidx.fragment.app.Fragment.setUserVisibleHint(boolean))")
    public void setUserVisibleHintMethod(JoinPoint joinPoint) {
        TDAopUtil.sendTrackEventToSDK("onFragmentSetUserVisibleHint", joinPoint.getTarget(), joinPoint.getArgs()[0]);
    }
}

