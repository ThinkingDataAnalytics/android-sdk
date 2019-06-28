package com.thinking.analyselibrary.runtime;

import android.view.MenuItem;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TDMenuItemSelectedAspect {

    @After("execution(* android.app.Activity.onOptionsItemSelected(android.view.MenuItem)) && args(menuItem)")
    public void onOptionsItemSelectedAOP(JoinPoint joinPoint, MenuItem menuItem) throws Throwable {
        TDAopUtil.sendTrackEventToSDK("onMenuItemSelected", joinPoint.getTarget(), menuItem);
    }

    @After("execution(* android.app.Activity.onContextItemSelected(android.view.MenuItem)) && args(menuItem)")
    public void onContextItemSelectedAOP(JoinPoint joinPoint, MenuItem menuItem) throws Throwable {
        TDAopUtil.sendTrackEventToSDK("onMenuItemSelected", joinPoint.getTarget(), menuItem);
    }
}