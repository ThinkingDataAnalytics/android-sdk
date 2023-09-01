/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom Fragment name, #title in ta_app_view.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ThinkingDataFragmentTitle {
    /**
     * Fetch the #title attribute value in the fragment ta_app_view event.
     *
     * @return fragment name
     */
    String title() default "";

    /**
     * Specifies the valid APP ID. This parameter is valid for all APP ids by default.
     *
     * @return Project APP ID
     */
    String appId() default "";
}