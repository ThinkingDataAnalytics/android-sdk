package com.thinking.analyselibrary.runtime;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TDTabHostOnTabChangedAspect {
    @After("execution(* android.widget.TabHost.OnTabChangeListener.onTabChanged(String)) && args(tabName)")
    public void onTabChanged(final JoinPoint joinPoint, String tabName) throws Throwable {
        TDAopUtil.sendTrackEventToSDK("onTabHostChanged", tabName);
    }
}
