/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.utils;

import android.text.TextUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

import cn.thinkingdata.analytics.TDConfig;
import cn.thinkingdata.core.utils.Base64Coder;
import cn.thinkingdata.core.utils.TDLog;

/**
 * HttpService send data
 */
public class HttpService implements RemoteService {
    private static final String TAG = "ThinkingAnalytics.HttpService";

    @Override
    public String performRequest(final TDConfig config, String params,
                                 Map<String, String> extraHeaders)
            throws ServiceUnavailableException, IOException {
        InputStream in = null;
        OutputStream out = null;
        BufferedOutputStream bout = null;
        BufferedReader br = null;
        HttpURLConnection connection = null;


        try {
            boolean debug = !config.isNormal();
            String clientUrl = getClientUrl(config);
            TDLog.i(TAG,"[ThinkingData] Info: post url: "+clientUrl);
            final URL url = new URL(clientUrl);
            connection = ( HttpURLConnection ) url.openConnection();
            if (null != config.getSSLSocketFactory() && connection instanceof HttpsURLConnection) {
                (( HttpsURLConnection ) connection).setSSLSocketFactory(config.getSSLSocketFactory());
            }
            // 从当前 URL 获取 host 用于 SSL 验证
            String currentUrl = config.getCurrentServerUrl();
            String host = config.mDnsServiceManager.getHostByUrl(currentUrl);
            if (TextUtils.isEmpty(host)) {
                try {
                    URL originalUrl = new URL(currentUrl);
                    host = originalUrl.getHost();
                } catch (Exception e) {
                    // ignore
                }
            }
            final String finalHost = host;
            if (config.mEnableDNS && finalHost != null && connection instanceof HttpsURLConnection) {
                (( HttpsURLConnection ) connection).setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return HttpsURLConnection.getDefaultHostnameVerifier().verify(finalHost, session);
                    }
                });
            }
            if(config.mEnableDNS && finalHost != null && !finalHost.isEmpty()) {
                connection.setRequestProperty("Host", finalHost);
            }
            if (null != params) {
                String query;

                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                if (debug) {
                    query = params;
                    connection.setRequestProperty("Content-Type",
                            "application/x-www-form-urlencoded");
                    connection.setUseCaches(false);
                    connection.setRequestProperty("charset", "utf-8");
                } else {
                    connection.setRequestProperty("Content-Type", "text/plain");
                    try {
                        query = encodeData(params);
                    } catch (Exception e) {
                        throw new InvalidParameterException(e.getMessage());
                    }
                }
                if (extraHeaders != null) {
                    for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
                        connection.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }

                connection.setFixedLengthStreamingMode(query.getBytes("UTF-8").length);
                out = connection.getOutputStream();
                bout = new BufferedOutputStream(out);
                bout.write(query.getBytes("UTF-8"));

                bout.flush();
                bout.close();
                bout = null;
                out.close();
                out = null;

                int responseCode = connection.getResponseCode();
                TDLog.d(TAG, "ret_code:" + responseCode);
                if (responseCode == 200) {
                    in = connection.getInputStream();
                    br = new BufferedReader(new InputStreamReader(in));
                    StringBuilder buffer = new StringBuilder();
                    String str;
                    while ((str = br.readLine()) != null) {
                        buffer.append(str);
                    }
                    in.close();
                    br.close();
                    return buffer.toString();
                } else {
                    if (config.mEnableDNS) {
                        config.mEnableDNS = false;
                        return performRequest(config, params, extraHeaders);
                    }
                    throw new ServiceUnavailableException(
                            "Service unavailable with response code: " + responseCode);
                }
            } else {
                throw new InvalidParameterException("Content is null");
            }

        } finally {
            if (null != bout) {
                try {
                    bout.close();
                } catch (final IOException e) {
                    //ignored
                }
            }

            if (null != out) {
                try {
                    out.close();
                } catch (final IOException e) {
                    //ignored
                }
            }
            if (null != in) {
                try {
                    in.close();
                } catch (final IOException ignored) {
                    //ignored
                }
            }
            if (null != br) {
                try {
                    br.close();
                } catch (final IOException ignored) {
                    //ignored
                }
            }
            if (null != connection) {
                connection.disconnect();
            }
        }
    }

    private String encodeData(final String rawMessage) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream(rawMessage.getBytes().length);
        GZIPOutputStream gos = new GZIPOutputStream(os);
        gos.write(rawMessage.getBytes());
        gos.close();
        byte[] compressed = os.toByteArray();
        os.close();
        return new String(Base64Coder.encode(compressed));
    }

    private String getClientUrl(TDConfig config) {
        String clientUrl = config.getCurrentServerUrl();
        if (!config.isNormal()) {
            clientUrl = config.getDebugUrl();
        }
        if (config.mEnableDNS) {
            // 直接根据 URL 获取 IP
            String ipUrl = config.mDnsServiceManager.getIpByUrl(clientUrl);
            if (!TextUtils.isEmpty(ipUrl)) {
                String host = config.mDnsServiceManager.getHostByUrl(clientUrl);
                if (!TextUtils.isEmpty(host)) {
                    clientUrl = clientUrl.replace(host, ipUrl);
                }
            } else {
                config.mDnsServiceManager.enableDNSService(config.getAllServerUrlList(),null);
            }
        }
        return clientUrl;
    }

}
