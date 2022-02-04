package cn.thinkingdata.android.runtime;

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
        AopUtils.trackViewClickEvent(joinPoint, view);
    }

    @Pointcut("execution(@cn.thinkingdata.android.ThinkingDataTrackViewOnClick * *(..))")
    public void methodAnnotatedWithTrackEvent() {
    }

    @After("methodAnnotatedWithTrackEvent()")
    public void trackOnClickAOP(final JoinPoint joinPoint) throws Throwable {
        try {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            if (null == methodSignature) return;

            Method method = methodSignature.getMethod();
            if (null == method) return;

            Annotation annotation = null;

            Class clazz = null;
            try {
                clazz = Class.forName("cn.thinkingdata.android.ThinkingDataTrackViewOnClick");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            if (null != clazz) {
                annotation = method.getAnnotation(clazz);
            }
            AopUtils.sendTrackEventToSDK("onViewOnClick", joinPoint.getArgs()[0], annotation);
        } catch (Exception e ) {
           // ignore the exception
        }
    }
}

