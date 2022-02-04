package cn.thinkingdata.android.runtime;

import android.view.View;
import android.widget.AdapterView;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TDAdapterViewItemAspect {

    @After("execution(* android.widget.AdapterView.OnItemClickListener.onItemClick(android.widget.AdapterView, android.view.View, int, long)) " +
            "&& args(parent, view, position, id)")
    public void onAdapterViewItemClick(AdapterView<?> parent, View view, int position, long id) throws Throwable {
        AopUtils.sendTrackEventToSDK("onAdapterViewItemClick", parent, view, position);

    }

    @After("execution(* android.widget.AdapterView.OnItemSelectedListener.onItemSelected(android.widget.AdapterView,android.view.View,int,long)) " +
            "&& args(parent, view, position, id)")
    public void onAdapterViewItemSelected(AdapterView<?> parent, View view, int position, long id) throws Throwable {
        AopUtils.sendTrackEventToSDK("onAdapterViewItemClick", parent, view, position);
    }
}
