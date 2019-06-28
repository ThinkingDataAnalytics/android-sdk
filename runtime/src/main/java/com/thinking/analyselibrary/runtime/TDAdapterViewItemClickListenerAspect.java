package com.thinking.analyselibrary.runtime;

import android.view.View;
import android.widget.AdapterView;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TDAdapterViewItemClickListenerAspect {

    @After("execution(* android.widget.AdapterView.OnItemClickListener.onItemClick(android.widget.AdapterView, android.view.View, int, long)) && args(parent, view, position, id)")
    public void onAdapterViewItemClick(final JoinPoint joinPoint, AdapterView<?> parent, View view, int position, long id) throws Throwable {
        TDAopUtil.sendTrackEventToSDK("onAdapterViewItemClick", parent, view, position);

    }

    @After("execution(* android.widget.AdapterView.OnItemLongClickListener.onItemLongClick(..))")
    public void onItemLongClickMethod(JoinPoint joinPoint) throws Throwable {

    }
}
