package com.thinking.analyselibrary;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 2.0.0 版本之前，需要在 BaseFragment 前添加此注释，采集 Fragment 浏览事件
 */
@Deprecated
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface ThinkingDataTrackFragmentAppViewScreen {
    String appId() default "";
}

