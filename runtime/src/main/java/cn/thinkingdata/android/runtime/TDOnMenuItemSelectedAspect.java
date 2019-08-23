package cn.thinkingdata.android.runtime;

import android.view.MenuItem;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TDOnMenuItemSelectedAspect {

    @After("execution(* android.app.Activity.onOptionsItemSelected(android.view.MenuItem)) && target(context) && args(menuItem)")
    public void onOptionsItemSelected(Object context, MenuItem menuItem) throws Throwable {
        AopUtils.sendTrackEventToSDK("onMenuItemSelected", context, menuItem);
    }

    @After("execution(* android.app.Activity.onContextItemSelected(android.view.MenuItem)) && target(context) && args(menuItem)")
    public void onContextItemSelected(Object context, MenuItem menuItem) throws Throwable {
        AopUtils.sendTrackEventToSDK("onMenuItemSelected", context, menuItem);
    }
}