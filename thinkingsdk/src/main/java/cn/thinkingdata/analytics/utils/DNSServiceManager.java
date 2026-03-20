/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.analytics.utils;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * URL 信息封装
     */
    public static class UrlInfo {
        public String ip;      // 解析后的 IP
        public String host;    // 原始域名

        public UrlInfo(String host, String ip) {
            this.host = host;
            this.ip = ip;
        }
    }

    private final Map<String, UrlInfo> mUrlIpMap = new ConcurrentHashMap<>();
    private List<TDConfig.TDDNSService> dnsList;
    private long lastEnableTime;

    public void enableDNSService(List<String> mUrls,List<TDConfig.TDDNSService> lists) {
        if (System.currentTimeMillis() - lastEnableTime < 30000) return;
        lastEnableTime = System.currentTimeMillis();
        if (this.dnsList == null) {
            this.dnsList = lists;
        }
        if (this.dnsList == null || this.dnsList.isEmpty()) return;
        if (mUrls == null || mUrls.isEmpty()) return;
        // 遍历所有 URLs，解析 IP
        for (String url : mUrls) {
            requestDNSUrl(0, url);
        }
    }

    private void requestDNSUrl(int index, String url) {
        try {
            if (index < 0 || index >= this.dnsList.size()) {
                return;
            }
            // 从 URL 提取 host
            String host = null;
            try {
                URL parsedUrl = new URL(url);
                host = parsedUrl.getHost();
            } catch (Exception e) {
                return;
            }
            if (TextUtils.isEmpty(host)) return;

            String dnsUrl = getDNSUrl(this.dnsList.get(index));
            if (TextUtils.isEmpty(dnsUrl)) return;

            TEHttpClient client = new TEHttpClient.Builder()
                    .build();
            Request request = new Request.Builder().url(dnsUrl + host)
                    .get()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("charset", "utf-8")
                    .addHeader("accept", "application/dns-json")
                    .build();
            final int finalIndex = index + 1;
            final String finalUrl = url;
            final String finalHost = host;
            client.newCall(request).enqueue(new TEHttpCallback() {
                @Override
                public void onFailure(int errorCode, String errorMsg) {
                    requestDNSUrl(finalIndex, finalUrl);
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
                                String ip = dataJson.optString("data");
                                if (!TextUtils.isEmpty(ip)) {
                                    mUrlIpMap.put(finalUrl, new UrlInfo(finalHost, ip));
                                }
                                return;
                            }
                        }
                    } catch (JSONException ignore) {
                    }
                    requestDNSUrl(finalIndex, finalUrl);
                }
            });
        } catch (Exception ignore) {
        }
    }

    private String getDNSUrl(TDConfig.TDDNSService mode) {
        switch (mode) {
            case CLOUD_FLARE:
                return "https://cloudflare-dns.com/dns-query?name=";
            case CLOUD_ALI:
                return "https://223.5.5.5/resolve?name=";
            case CLOUD_GOOGLE:
                return "https://8.8.8.8/resolve?name=";
        }
        return "https://cloudflare-dns.com/dns-query?name=";
    }

    /**
     * 根据 URL 获取对应的 IP
     * @param url 完整 URL（带 /sync 后缀）
     * @return IP 地址，如果未解析则返回 null
     */
    public String getIpByUrl(String url) {
        UrlInfo info = mUrlIpMap.get(url);
        return info != null ? info.ip : null;
    }

    /**
     * 根据 URL 获取对应的域名
     * @param url 完整 URL（带 /sync 后缀）
     * @return 域名
     */
    public String getHostByUrl(String url) {
        UrlInfo info = mUrlIpMap.get(url);
        return info != null ? info.host : null;
    }

}
