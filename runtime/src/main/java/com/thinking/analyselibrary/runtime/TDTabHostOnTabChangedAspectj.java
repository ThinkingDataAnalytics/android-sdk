package com.thinking.analyselibrary.runtime;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TDTabHostOnTabChangedAspectj {
    private final static String TAG = TDTabHostOnTabChangedAspectj.class.getCanonicalName();

    @After("execution(* android.widget.TabHost.OnTabChangeListener.onTabChanged(String))")
    public void onTabChangedAOP(final JoinPoint joinPoint) throws Throwable {
        TDAopUtil.sendTrackEventToSDK(joinPoint, "onTabHostChanged");
    }
}
