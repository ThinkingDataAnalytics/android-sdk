package com.thinking.analyselibrary.runtime;

import android.content.DialogInterface;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TDDialogOnClickAspect {

    @After("execution(* android.content.DialogInterface.OnClickListener.onClick(android.content.DialogInterface, int)) &&" +
            "args(dialog, which)")
    public void onClickAOP(final JoinPoint joinPoint, DialogInterface dialog, int which) throws Throwable {
        TDAopUtil.sendTrackEventToSDK("onDialogClick", dialog, which);
    }

    @After("execution(* android.content.DialogInterface.OnMultiChoiceClickListener.onClick(android.content.DialogInterface, int, boolean)) &&" +
            "args(dialog, which, isChecked)")
    public void onMultiChoiceClickAOP(final JoinPoint joinPoint, DialogInterface dialog, int which, boolean isChecked) throws Throwable {
        TDAopUtil.sendTrackEventToSDK("onDialogClick", dialog, which);
    }
}
