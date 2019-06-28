package com.thinking.analyselibrary.runtime;


import android.view.View;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Aspect
public class TDViewOnClickListenerAspect {

    @After("execution(* android.view.View.OnClickListener.onClick(android.view.View)) && args(view)")
    public void onViewClickAOP(final JoinPoint joinPoint, android.view.View view) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        if (null == methodSignature) return;

        Method method = methodSignature.getMethod();
        if (null == method) return;

        Annotation annotation = null;

        Class clazz = null;
        try {
            clazz = Class.forName("com.thinking.analyselibrary.ThinkingDataIgnoreTrackOnClick");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (null != clazz) {
            annotation = method.getAnnotation(clazz);
        }
        TDAopUtil.sendTrackEventToSDK("onViewOnClick", view, annotation);
    }

    @After("execution(* android.view.View.OnLongClickListener.onLongClick(android.view.View))")
    public void onViewLongClickAOP(JoinPoint joinPoint) throws Throwable {

    }

    // TODO do test on this advice
    @After("execution(* android.widget.CompoundButton.OnCheckedChangeListener.onCheckedChanged(android.widget.CompoundButton,boolean))")
    public void onCheckedChangedAOP(final JoinPoint joinPoint) throws Throwable {
        TDAopUtil.sendTrackEventToSDK(joinPoint, "onCheckBoxCheckedChanged");
    }

    @After("execution(* android.widget.RadioGroup.OnCheckedChangeListener.onCheckedChanged(android.widget.RadioGroup,int))")
    public void onCheckedChangedAOPR(final JoinPoint joinPoint) throws Throwable {
        onViewClickAOP(joinPoint, (View)joinPoint.getArgs()[0]);
    }

    @Pointcut("execution(@com.thinking.analyselibrary.ThinkingDataTrackViewOnClick * *(..))")
    public void methodAnnotatedWithTrackEvent() {
    }

    @After("methodAnnotatedWithTrackEvent()")
    public void trackOnClickAOP(final JoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        if (null == methodSignature) return;

        Method method = methodSignature.getMethod();
        if (null == method) return;

        Annotation annotation = null;

        Class clazz = null;
        try {
            clazz = Class.forName("com.thinking.analyselibrary.ThinkingDataTrackViewOnClick");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (null != clazz) {
            annotation = method.getAnnotation(clazz);
        }
        TDAopUtil.sendTrackEventToSDK("onViewOnClick", joinPoint.getArgs()[0], annotation);
    }

    @After("execution(* android.widget.SeekBar.OnSeekBarChangeListener.onStartTrackingTouch(android.widget.SeekBar)) && args(view)")
    public void onStartTrackingTouchMethod(JoinPoint joinPoint, View view) throws Throwable {
        onViewClickAOP(joinPoint, view);
    }

    @After("execution(* android.widget.SeekBar.OnSeekBarChangeListener.onStopTrackingTouch(android.widget.SeekBar)) && args(view)")
    public void onStopTrackingTouchMethod(JoinPoint joinPoint, View view) throws Throwable {
        onViewClickAOP(joinPoint, view);
    }

    @After("execution(* android.widget.RatingBar.OnRatingBarChangeListener.onRatingChanged(android.widget.RatingBar,float,boolean))")
    public void onRatingChangedAOP(final JoinPoint joinPoint) throws Throwable {
        try {
            onViewClickAOP(joinPoint, (View) joinPoint.getArgs()[0]);
        } catch (Exception e) {

        }
    }

    @After("execution(* android.widget.AdapterView.OnItemSelectedListener.onItemSelected(android.widget.AdapterView,android.view.View,int,long))")
    public void onItemSelectedAOP(final JoinPoint joinPoint) throws Throwable {
        try {
            onViewClickAOP(joinPoint, (View) joinPoint.getArgs()[0]);
        } catch (Exception e) {

        }
    }

    @After("execution(* android.app.TimePickerDialog.OnTimeSetListener.onTimeSet(android.widget.TimePicker, int, int))")
    public void onTimeSet(final JoinPoint joinPoint) throws Throwable {
        try {
            onViewClickAOP(joinPoint, (View) joinPoint.getArgs()[0]);
        } catch (Exception e) {

        }
    }
}

