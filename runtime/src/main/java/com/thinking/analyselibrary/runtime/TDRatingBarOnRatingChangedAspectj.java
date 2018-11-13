package com.thinking.analyselibrary.runtime;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TDRatingBarOnRatingChangedAspectj {
    private final static String TAG = TDRatingBarOnRatingChangedAspectj.class.getCanonicalName();

    @After("execution(* android.widget.RatingBar.OnRatingBarChangeListener.onRatingChanged(android.widget.RatingBar,float,boolean))")
    public void onRatingChangedAOP(final JoinPoint joinPoint) throws Throwable {
        TDAopUtil.sendTrackEventToSDK(joinPoint, "onRatingBarChanged");
    }
}

