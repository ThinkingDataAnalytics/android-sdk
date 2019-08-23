package cn.thinkingdata.android.runtime;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TDFragmentAspect {

    @Around("(execution(* android.support.v4.app.Fragment.onCreateView(..))||" +
            "execution(* androidx.fragment.app.Fragment.onCreateView(..))||" +
            "execution(* android.app.Fragment.onCreateView(..))) && target(fragment)")
    public Object onCreateViewMethod(ProceedingJoinPoint joinPoint, Object fragment) throws Throwable {
        Object result = joinPoint.proceed();
        if (null != result) {
            AopUtils.sendTrackEventToSDK("onFragmentCreateView", fragment, result);
        }
        return result;
    }

    @After("(call(* android.support.v4.app.Fragment.onResume())||" +
            "call(* androidx.fragment.app.Fragment.onResume())) && target(fragment)")
    public void onResumeMethod(JoinPoint joinPoint, Object fragment) {
        AopUtils.sendTrackEventToSDK("onFragmentOnResume", fragment);
    }

    @After("(execution(* android.support.v4.app.Fragment.onHiddenChanged(boolean))||" +
            "execution(* androidx.fragment.app.Fragment.onHiddenChanged(boolean))) && target(fragment) && args(hidden)")
    public void onHiddenChangedMethod(JoinPoint joinPoint, Object fragment, boolean hidden) {
        AopUtils.sendTrackEventToSDK("onFragmentHiddenChanged", fragment, hidden);
    }

    @After("(execution(* android.support.v4.app.Fragment.setUserVisibleHint(boolean))||" +
            "execution(* androidx.fragment.app.Fragment.setUserVisibleHint(boolean))) && target(fragment) && args(isVisibleToUser)")
    public void setUserVisibleHintMethod(JoinPoint joinPoint, Object fragment, boolean isVisibleToUser) {
        AopUtils.sendTrackEventToSDK("onFragmentSetUserVisibleHint", fragment, isVisibleToUser);
    }
}

