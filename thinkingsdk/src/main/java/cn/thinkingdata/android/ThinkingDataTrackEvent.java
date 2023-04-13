/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate the method with @ThinkingDataTrackEvent to upload custom events when the method is called.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ThinkingDataTrackEvent {
    /**
     * Gets the event name.
     *
     * @return event name
     */
    String eventName() default "";

    /**
     * Gets the event properties.
     *
     * @return event properties
     */
    String properties() default "{}";

    /**
     * Optional, specify the project APP ID. By default, this event is reported for all instances.
     *
     * @return APP ID
     */
    String appId() default "";
}

