/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.aop.push;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2022/5/31
 * @since
 */
public class TAPushUtils {
    /**
     * 获取极光推送的手机厂商
     */
    public static String getJPushSource(int sdk) {
        String name;
        switch (sdk) {
            case 1:
                name = "Xiaomi";
                break;
            case 2:
                name = "HUAWEI";
                break;
            case 3:
                name = "Meizu";
                break;
            case 4:
                name = "OPPO";
                break;
            case 5:
                name = "vivo";
                break;
            default:
                name = null;
        }
        return name;
    }
}
