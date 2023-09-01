/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adding this annotation to an Activity or Fragment will no longer collect the ta_app_view event for that page.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ThinkingDataIgnoreTrackAppViewScreen {
    /**
     * Specifies the valid APP ID. This parameter is valid for all APP ids by default.
     *
     * @return APP ID
     */
    String appId() default "";
}
