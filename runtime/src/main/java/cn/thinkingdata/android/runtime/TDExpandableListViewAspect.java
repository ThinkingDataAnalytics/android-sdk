package cn.thinkingdata.android.runtime;

import android.view.View;
import android.widget.ExpandableListView;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TDExpandableListViewAspect {

    @After("execution(* android.widget.ExpandableListView.OnChildClickListener.onChildClick(android.widget.ExpandableListView, android.view.View, int, int, long)) && " +
            "args(expandableListView, view, groupPosition, childPosition, id) ")
    public void onChildClick(final JoinPoint joinPoint, ExpandableListView expandableListView, View view, int groupPosition, int childPosition, long id) {
        AopUtils.sendTrackEventToSDK("onExpandableListViewOnChildClick", expandableListView, view, groupPosition, childPosition);
    }

    @After("execution(* android.widget.ExpandableListView.OnGroupClickListener.onGroupClick(android.widget.ExpandableListView, android.view.View, int, long)) && " +
            "args(expandableListView, view, groupPosition, id)")
    public void onGroupClick(final JoinPoint joinPoint, ExpandableListView expandableListView, View view, int groupPosition, long id) {
        AopUtils.sendTrackEventToSDK("onExpandableListViewOnGroupClick", expandableListView, view, groupPosition);
    }
}
