package com.thinking.analyselibrary.runtime;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TDCheckBoxOnCheckedChangedAspectj {
    private final static String TAG = TDCheckBoxOnCheckedChangedAspectj.class.getCanonicalName();

    @After("execution(* android.widget.CompoundButton.OnCheckedChangeListener.onCheckedChanged(android.widget.CompoundButton,boolean))")
    public void onCheckedChangedAOP(final JoinPoint joinPoint) throws Throwable {
        TDAopUtil.sendTrackEventToSDK(joinPoint, "onCheckBoxCheckedChanged");
    }
}
