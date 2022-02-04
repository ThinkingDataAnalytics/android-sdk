package cn.thinkingdata.android.runtime;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TDOnWidgetsChangeAspect {

    @After("execution(* android.widget.RadioGroup.OnCheckedChangeListener.onCheckedChanged(android.widget.RadioGroup,int)) && args(radioGroup, checkedId)")
    public void onRadioGroupCheckedChanged(final JoinPoint joinPoint, RadioGroup radioGroup, int checkedId) throws Throwable {
        AopUtils.trackViewClickEvent(joinPoint, radioGroup);
    }

    @After("execution(* android.widget.SeekBar.OnSeekBarChangeListener.onStopTrackingTouch(android.widget.SeekBar)) && args(view)")
    public void onStopTrackingTouchMethod(JoinPoint joinPoint, View view) throws Throwable {
        AopUtils.trackViewClickEvent(joinPoint, view);
    }

    @After("execution(* android.widget.RatingBar.OnRatingBarChangeListener.onRatingChanged(android.widget.RatingBar,float, boolean))")
    public void onRatingChangedMethod(final JoinPoint joinPoint) throws Throwable {
        AopUtils.trackViewClickEvent(joinPoint, (View) joinPoint.getArgs()[0]);
    }

    @After("execution(* android.widget.CompoundButton.OnCheckedChangeListener.onCheckedChanged(android.widget.CompoundButton, boolean)) " +
            "&& args(buttonView, isChecked) ")
    public void onCompoundButtonCheckedChanged(final JoinPoint joinPoint, CompoundButton buttonView, boolean isChecked) throws Throwable {
        AopUtils.trackViewClickEvent(joinPoint, buttonView);
    }

    @After("execution(* android.widget.TabHost.OnTabChangeListener.onTabChanged(String)) && args(tabName)")
    public void onTabChanged(final JoinPoint joinPoint, String tabName) throws Throwable {
        AopUtils.sendTrackEventToSDK("onTabHostChanged", tabName);
    }
}
