package com.thinking.analyselibrary.runtime;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TDAdapterViewOnItemClickListenerAspectj {
    private final static String TAG = TDAdapterViewOnItemClickListenerAspectj.class.getCanonicalName();

    @After("execution(* android.widget.AdapterView.OnItemClickListener.onItemClick(android.widget.AdapterView,android.view.View,int,long))")
    public void onItemClickAOP(final JoinPoint joinPoint) throws Throwable {
        TDAopUtil.sendTrackEventToSDK(joinPoint, "onAdapterViewItemClick");
    }

    @After("execution(* android.widget.AdapterView.OnItemLongClickListener.onItemLongClick(..))")
    public void onItemLongClickMethod(JoinPoint joinPoint) throws Throwable {

    }
}
