package com.thinking.analyselibrary.runtime;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TDDialogOnClickAspectj {
    private final static String TAG = TDDialogOnClickAspectj.class.getCanonicalName();

    @After("execution(* android.content.DialogInterface.OnClickListener.onClick(android.content.DialogInterface, int))")
    public void onClickAOP(final JoinPoint joinPoint) throws Throwable {
        TDAopUtil.sendTrackEventToSDK(joinPoint, "onDialogClick");
    }

    @After("execution(* android.content.DialogInterface.OnMultiChoiceClickListener.onClick(android.content.DialogInterface, int, boolean))")
    public void onMultiChoiceClickAOP(final JoinPoint joinPoint) throws Throwable {
        TDAopUtil.sendTrackEventToSDK(joinPoint, "onMultiChoiceClick");
    }
}
