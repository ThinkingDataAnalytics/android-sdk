/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Adding this annotation to your Activity will no longer upload the automatic collection event on that page and the control click event under that page.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ThinkingDataIgnoreTrackAppViewScreenAndAppClick {
    /**
     * Specifies the valid APP ID. This parameter is valid for all APP ids by default.
     *
     * @return APP ID
     */
    String appId() default "";
}
