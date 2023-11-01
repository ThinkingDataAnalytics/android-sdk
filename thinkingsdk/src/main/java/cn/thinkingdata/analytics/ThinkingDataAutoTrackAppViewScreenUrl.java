/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add this annotation for your Activity or Fragment to customize the #url attribute in the ta_app_view event.
 */
@Inherited
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ThinkingDataAutoTrackAppViewScreenUrl {
    /**
     * Gets the url property value.
     *
     * @return url
     */
    String url() default "";

    /**
     * Specifies the valid APP ID. This parameter is valid for all APP ids by default.
     *
         * @return APP ID
     */
    String appId() default "";
}
