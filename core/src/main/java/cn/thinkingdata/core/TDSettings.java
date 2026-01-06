/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core;

import org.json.JSONObject;

import java.util.Map;
import java.util.TimeZone;

import javax.net.ssl.SSLSocketFactory;

/**
 * @author liulongbing
 * @since 2024/5/17
 */
public class TDSettings {

    public String appId;
    public String serverUrl;
    public String instanceName;
    public TDMode mode;
    public TimeZone defaultTimeZone;
    public int encryptVersion;
    public String encryptKey;
    public boolean enableAutoPush;
    public boolean enableAutoCalibrated;
    public boolean enableLog;
    public SSLSocketFactory sslSocketFactory;
    public JSONObject rccFetchParams;

    public enum TDMode {
        NORMAL,
        DEBUG,
        DEBUG_ONLY
    }
}
