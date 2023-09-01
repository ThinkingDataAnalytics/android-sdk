/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If you use android:onclick to add a call method to the click event for the control (view), you can add this annotation to the calling method, and when the call method is executed, the SDK will upload the control *
 * Click events. Controls that create views by AppCompatViewInflater don't need to manually add comments to collect click events. These include:
 * AppCompatButton
 * AppCompatImageButton
 * AppCompatImageView
 * AppCompatRadioButton
 * AppCompatCheckBox
 * AppCompatCheckedTextView
 * AppCompatEditText
 * AppCompatMultiAutoCompleteTextView
 * AppCompatAutoCompleteTextView
 * AppCompatRatingBar
 * AppCompatSpinner
 * AppCompatSeekBar
 * AppCompatTextView
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ThinkingDataTrackViewOnClick {
    /**
     * Specifies the valid APP ID. This parameter is valid for all APP ids by default.
     *
     * @return APP ID
     */
    String appId() default "";
}

