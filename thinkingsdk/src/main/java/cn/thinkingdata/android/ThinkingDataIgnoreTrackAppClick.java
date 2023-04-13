/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adding this annotation to an Activity will no longer collect click events on that page.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ThinkingDataIgnoreTrackAppClick {
    /**
     * Specifies the valid APP ID. This parameter is valid for all APP ids by default.
     *
     * @return APP ID
     */
    String appId() default "";
}

