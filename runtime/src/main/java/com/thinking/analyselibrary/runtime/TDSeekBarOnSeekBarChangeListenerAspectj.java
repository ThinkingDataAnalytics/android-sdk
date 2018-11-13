package com.thinking.analyselibrary.runtime;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TDSeekBarOnSeekBarChangeListenerAspectj {
    private final static String TAG = TDSeekBarOnSeekBarChangeListenerAspectj.class.getCanonicalName();

    @After("execution(* android.widget.SeekBar.OnSeekBarChangeListener.onStartTrackingTouch(android.widget.SeekBar))")
    public void onStartTrackingTouchMethod(JoinPoint joinPoint) throws Throwable {
        actionAOP(joinPoint, "onStartTrackingTouch");
    }

    @After("execution(* android.widget.SeekBar.OnSeekBarChangeListener.onStopTrackingTouch(android.widget.SeekBar))")
    public void onStopTrackingTouchMethod(JoinPoint joinPoint) throws Throwable {
        actionAOP(joinPoint, "onStopTrackingTouch");
    }

    private void actionAOP(final JoinPoint joinPoint, final String action) {
        TDAopUtil.sendTrackEventToSDK(joinPoint, "onSeekBarChange");
    }
}

