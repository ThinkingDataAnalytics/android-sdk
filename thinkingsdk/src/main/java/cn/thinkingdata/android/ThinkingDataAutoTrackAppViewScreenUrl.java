package cn.thinkingdata.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 为 Activity 或者 Fragment 添加此注解，用于自定义 ta_app_view 事件中的 #url 属性
 */
@Inherited
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ThinkingDataAutoTrackAppViewScreenUrl {
    /**
     * 获取 url 属性值
     * @return url 属性值
     */
    String url() default "";

    /**
     * 指定生效的 APP ID, 默认对所有 APP ID 生效
     * @return 项目 APP ID
     */
    String appId() default "";
}
