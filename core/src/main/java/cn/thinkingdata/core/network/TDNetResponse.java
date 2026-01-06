/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.network;

import java.util.List;
import java.util.Map;

/**
 * @author liulongbing
 * @since 2024/5/22
 */
public class TDNetResponse {
    public int statusCode;
    public String msg;
    public String responseData;
    public Map<String, List<String>> responseHeaders;
}
