package cn.thinkingdata.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 为 onClick 的方法添加此注解，将不再采集此空间的点击事件
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ThinkingDataIgnoreTrackOnClick {
    /**
     * 指定生效的 APP ID, 默认对所有 APP ID 生效
     * @return 项目 APP ID
     */
    String appId() default "";
}

