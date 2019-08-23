package cn.thinkingdata.android.runtime;

import android.content.DialogInterface;
import android.view.View;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TDDialogOnClickAspect {

    @After("execution(* android.content.DialogInterface.OnClickListener.onClick(android.content.DialogInterface, int)) &&" +
            "args(dialog, which)")
    public void onDialogClick(DialogInterface dialog, int which) {
        AopUtils.sendTrackEventToSDK("onDialogClick", dialog, which);
    }

    @After("execution(* android.content.DialogInterface.OnMultiChoiceClickListener.onClick(android.content.DialogInterface, int, boolean)) &&" +
            "args(dialog, which, isChecked)")
    public void onMultiChoiceClick(DialogInterface dialog, int which, boolean isChecked) {
        AopUtils.sendTrackEventToSDK("onDialogClick", dialog, which);
    }

    @After("execution(* android.app.TimePickerDialog.OnTimeSetListener.onTimeSet(android.widget.TimePicker, int, int))")
    public void onTimeSet(final JoinPoint joinPoint) throws Throwable {
        AopUtils.trackViewClickEvent(joinPoint, (View) joinPoint.getArgs()[0]);
    }

    @After("execution(* android.app.DatePickerDialog.OnDateSetListener.onDateSet(android.widget.DatePicker, int, int, int))")
    public void onDateSet(final JoinPoint joinPoint) {
        AopUtils.trackViewClickEvent(joinPoint, (View) joinPoint.getArgs()[0]);
    }

}
