/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.analytics.utils;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import cn.thinkingdata.analytics.TDConfig;
import cn.thinkingdata.core.network.Request;
import cn.thinkingdata.core.network.TDNetResponse;
import cn.thinkingdata.core.network.TEHttpCallback;
import cn.thinkingdata.core.network.TEHttpClient;


/**
 * @author liulongbing
 * @since 2024/8/20
 */
public class DNSServiceManager {
    private String mHost;
    private List<TDConfig.TDDNSService> dnsList;
    private volatile String mIpUrl;
    private long lastEnableTime;

    public DNSServiceManager(String host) {
        this.mHost = host;
    }

    public void enableDNSService(List<TDConfig.TDDNSService> lists) {
        if (System.currentTimeMillis() - lastEnableTime < 30000) return;
        lastEnableTime = System.currentTimeMillis();
        if (this.dnsList == null) {
            this.dnsList = lists;
        }
        if (this.dnsList == null || this.dnsList.size() == 0) return;
        requestDNSUrl(0);
    }

    private void requestDNSUrl(int index) {
        try {
            if (index < 0 || index >= this.dnsList.size()) {
                return;
            }
            String url = getDNSUrl(this.dnsList.get(index));
            if (TextUtils.isEmpty(url) || TextUtils.isEmpty(this.mHost)) return;
            TEHttpClient client = new TEHttpClient.Builder()
                    .build();
            Request request = new Request.Builder().url(url + this.mHost)
                    .get()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("charset", "utf-8")
                    .addHeader("accept", "application/dns-json")
                    .build();
            final int finalIndex = index + 1;
            client.newCall(request).enqueue(new TEHttpCallback() {
                @Override
                public void onFailure(int errorCode, String errorMsg) {
                    requestDNSUrl(finalIndex);
                }

                @Override
                public void onSuccess(TDNetResponse data) {
                    try {
                        JSONObject retJson = new JSONObject(data.responseData);
                        int retStatus = retJson.optInt("Status");
                        JSONArray array = retJson.optJSONArray("Answer");
                        if (retStatus == 0 && array != null && array.length() > 0) {
                            JSONObject dataJson = array.optJSONObject(array.length() - 1);
                            if (dataJson != null) {
                                mIpUrl = dataJson.optString("data");
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    requestDNSUrl(finalIndex);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getDNSUrl(TDConfig.TDDNSService mode) {
        switch (mode) {
            case CLOUD_FLARE:
            default:
                return "https://cloudflare-dns.com/dns-query?name=";
            case CLOUD_ALI:
                return "https://223.5.5.5/resolve?name=";
            case CLOUD_GOOGLE:
                return "https://8.8.8.8/resolve?name=";
        }
    }

    public String getIPUrl() {
        return mIpUrl;
    }

    public String getHost() {
        return mHost;
    }

}
