/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.core.router.provider;

import android.content.Context;

import org.json.JSONObject;

import cn.thinkingdata.core.router.provider.callback.ISensitivePropertiesCallBack;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2023/11/9
 * @since
 */
public interface ISensitiveProvider extends IProvider {
    void getSensitiveProperties(Context context,ISensitivePropertiesCallBack callBack);
}
