/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.core.router.provider.callback;

import org.json.JSONObject;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2023/11/9
 * @since
 */
public interface ISensitivePropertiesCallBack {
    void onSuccess(JSONObject json);
}
