package cn.thinkingdata.android.utils.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 为 Activity 添加此注解，将不再上传该页面的自动采集事件与该页面下的控件点击事件
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ThinkingDataIgnoreTrackAppViewScreenAndAppClick {
    /**
     * 指定生效的 APP ID, 默认对所有 APP ID 生效
     * @return 项目 APP ID
     */
    String appId() default "";
}
