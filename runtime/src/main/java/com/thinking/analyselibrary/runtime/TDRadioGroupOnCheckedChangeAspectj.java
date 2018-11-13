package com.thinking.analyselibrary.runtime;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TDRadioGroupOnCheckedChangeAspectj {
    private final static String TAG = TDRadioGroupOnCheckedChangeAspectj.class.getCanonicalName();

    @After("execution(* android.widget.RadioGroup.OnCheckedChangeListener.onCheckedChanged(android.widget.RadioGroup,int))")
    public void onCheckedChangedAOP(final JoinPoint joinPoint) throws Throwable {
        TDAopUtil.sendTrackEventToSDK(joinPoint, "onRadioGroupCheckedChanged");
    }
}
