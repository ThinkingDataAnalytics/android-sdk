package com.thinking.analyselibrary.runtime;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;


@Aspect
public class TDMenuItemSelectedAspectj {
    private final static String TAG = TDMenuItemSelectedAspectj.class.getCanonicalName();

    @After("execution(* android.app.Activity.onOptionsItemSelected(android.view.MenuItem))")
    public void onOptionsItemSelectedAOP(JoinPoint joinPoint) throws Throwable {
        onMenuClick(joinPoint, 0, "onOptionsItemSelected");
    }

    @After("execution(* android.app.Activity.onContextItemSelected(android.view.MenuItem))")
    public void onContextItemSelectedAOP(JoinPoint joinPoint) throws Throwable {
        onMenuClick(joinPoint, 0, "onContextItemSelected");
    }

    @After("execution(* android.app.Activity.onMenuItemSelected(int, android.view.MenuItem))")
    public void onMenuItemSelectedAOP(JoinPoint joinPoint) throws Throwable {
        onMenuClick(joinPoint, 1, "onMenuItemSelected");
    }

    private void onMenuClick(final JoinPoint joinPoint, final int menuItemIndex, final String action) {
        TDAopUtil.sendTrackEventToSDK(joinPoint, "onMenuClick", menuItemIndex);
    }
}
