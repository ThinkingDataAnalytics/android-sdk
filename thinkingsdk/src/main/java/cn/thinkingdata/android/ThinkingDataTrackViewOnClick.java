package cn.thinkingdata.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 如果是以android:onclick为控件（view）添加点击事件的调用方法，则可以在调用方法上添加此注解, 该调用方法被执行时，SDK将会上传控件
 * 点击事件. 通过 AppCompatViewInflater 创建视图的控件不需要手动添加注解，也可以采集到点击事件. 包括:
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
     * 指定生效的 APP ID, 默认对所有 APP ID 生效
     * @return 项目 APP ID
     */
    String appId() default "";
}

