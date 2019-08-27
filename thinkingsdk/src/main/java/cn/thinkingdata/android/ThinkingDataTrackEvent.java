package cn.thinkingdata.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 在方法上加上注解 @ThinkingDataTrackEvent，可在方法调用时上传自定义事件.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ThinkingDataTrackEvent {
    /**
     * 获取事件名称
     * @return 事件名称
     */
    String eventName() default "";

    /**
     * 获取事件属性
     * @return 事件属性
     */
    String properties() default "{}";

    /**
     * 可选，指定项目 APP ID. 默认会为所有实例上报此事件.
     * @return 项目 APP ID
     */
    String appId() default "";
}

