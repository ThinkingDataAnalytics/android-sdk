/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Adding this annotation to the onClick method will no longer collect click events for this space.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ThinkingDataIgnoreTrackOnClick {
    /**
     * Specifies the valid APP ID. This parameter is valid for all APP ids by default.
     *
     * @return Project APP ID
     */
    String appId() default "";
}

