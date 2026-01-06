/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.core.network;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import cn.thinkingdata.core.exception.TDHttpException;
import cn.thinkingdata.core.utils.TDLog;

/**
 * @author liulongbing
 * @since 2023/5/4
 */
public class RealCall implements Call {
    private static final String TAG = "ThinkingAnalytics.RealCall";

    final TEHttpClient client;
    final Request originalRequest;

    static RealCall newRealCall(TEHttpClient client, Request originalRequest) {
        return new RealCall(client, originalRequest);
    }

    private RealCall(TEHttpClient client, Request originalRequest) {
        this.client = client;
        this.originalRequest = originalRequest;
    }

    @Override
    public TDNetResponse execute() {
        try {
            return performRequest();
        } catch (TDHttpException e) {
            TDNetResponse errorResponse = new TDNetResponse();
            errorResponse.statusCode = e.errorCode;
            errorResponse.msg = e.getMessage();
            return errorResponse;
        }
    }

    @Override
    public void enqueue(TEHttpCallback responseCallback) {
        responseCallback.callBackOnMainThread = originalRequest.callBackOnMainThread;
        client.dispatcher.execute(new AsyncCall(responseCallback));
    }

    class AsyncCall implements Runnable {

        private final TEHttpCallback responseCallback;

        public AsyncCall(TEHttpCallback responseCallback) {
            this.responseCallback = responseCallback;
        }

        @Override
        public void run() {
            try {
                TDNetResponse response = performRequest();
                responseCallback.onResponse(response);
            } catch (TDHttpException e) {
                responseCallback.onError(e.errorCode, e.getMessage());
            }
        }
    }

    private TDNetResponse performRequest() throws TDHttpException {
        InputStream in = null;
        OutputStream out = null;
        BufferedOutputStream bout = null;
        BufferedReader br = null;
        HttpURLConnection conn = null;
        try {
            conn = getHttpURLConnection();
            conn.setUseCaches(originalRequest.useCache);
            setHeaders(conn);
            if ("POST".equals(originalRequest.method)) {
                String params = originalRequest.body;
                if (originalRequest.gzip) {
                    params = encodeData(params);
                }
                conn.setFixedLengthStreamingMode(params.getBytes("UTF-8").length);
                out = conn.getOutputStream();
                bout = new BufferedOutputStream(out);
                bout.write(params.getBytes("UTF-8"));
                bout.flush();
                bout.close();
                bout = null;
                out.close();
                out = null;
            }
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                TDNetResponse response = new TDNetResponse();
                response.responseHeaders = conn.getHeaderFields();
                in = conn.getInputStream();
                br = new BufferedReader(new InputStreamReader(in));
                StringBuilder buffer = new StringBuilder();
                String str;
                while ((str = br.readLine()) != null) {
                    buffer.append(str);
                }
                in.close();
                br.close();
                response.statusCode = 200;
                response.msg = "success";
                response.responseData = buffer.toString();
                return response;
            } else {
                throw new TDHttpException(responseCode,
                        "Service unavailable with response code: " + responseCode);
            }

        } catch (SocketTimeoutException e) {
            throw new TDHttpException(TDHttpException.ERROR_CONNECT_TIME_OUT,
                    e.getMessage());
        } catch (Exception e) {
            throw new TDHttpException(TDHttpException.ERROR_EXCEPTION,
                    e.getMessage());
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
            if (null != conn) {
                conn.disconnect();
            }
        }
    }

    private void setHeaders(HttpURLConnection conn) {
        Map<String, String> headerMap = originalRequest.headers;
        if (headerMap.size() > 0) {
            for (String key : headerMap.keySet()) {
                conn.setRequestProperty(key, headerMap.get(key));
            }
        }
    }

    private HttpURLConnection getHttpURLConnection() throws IOException {
        final URL url = new URL(originalRequest.url);
        HttpURLConnection connection = ( HttpURLConnection ) url.openConnection();
        if (null != client.sslSocketFactory && connection instanceof HttpsURLConnection) {
            (( HttpsURLConnection ) connection).setSSLSocketFactory(client.sslSocketFactory);
        }
        connection.setConnectTimeout(client.connectTimeout);
        connection.setReadTimeout(client.readTimeout);
        if ("POST".equals(originalRequest.method)) {
            connection.setDoOutput(true);
        }
        connection.setRequestMethod(originalRequest.method);
        return connection;
    }

    private String encodeData(final String rawMessage) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(rawMessage.getBytes().length);
        GZIPOutputStream gos = new GZIPOutputStream(os);
        gos.write(rawMessage.getBytes());
        gos.close();
        byte[] compressed = os.toByteArray();
        os.close();
        //return new String(Base64Coder.encode(compressed));
        return "";
    }

}
