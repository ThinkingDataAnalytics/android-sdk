/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.core.network;

import java.util.HashMap;
import java.util.Map;

/**
 * @author liulongbing
 * @since 2023/5/4
 */
public class Request {

    final String url;
    final String method;
    final Map<String, String> headers;
    final String body;
    final boolean gzip;
    final boolean useCache;
    final boolean callBackOnMainThread;

    Request(Builder builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.headers = builder.headers;
        this.body = builder.body;
        this.gzip = builder.gzip;
        this.useCache = builder.useCache;
        this.callBackOnMainThread = builder.callBackOnMainThread;
    }

    public static class Builder {
        String url;
        String method;
        Map<String, String> headers;
        String body;
        boolean gzip;
        boolean useCache;
        boolean callBackOnMainThread;

        public Builder() {
            this.method = "GET";
            this.headers = new HashMap<>();
            this.gzip = false;
            this.useCache = true;
            this.callBackOnMainThread = false;
        }

        public Builder url(String url) {
            if (url == null) throw new NullPointerException("http url == null");
            this.url = url;
            return this;
        }

        public Builder get() {
            return method("GET", null);
        }

        public Builder post(String body) {
            return method("POST", body);
        }

        public Builder method(String method, String body) {
            if (body == null && "POST".equals(method)) {
                throw new IllegalArgumentException("method " + method + " must have a request body.");
            }
            this.method = method;
            this.body = body;
            return this;
        }


        public Builder addHeader(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public Builder removeHeader(String name) {
            headers.remove(name);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }


        public Builder gzip() {
            this.gzip = true;
            return this;
        }

        public Builder useCache(boolean useCache) {
            this.useCache = useCache;
            return this;
        }

        public Builder mainThread() {
            this.callBackOnMainThread = true;
            return this;
        }

        public Request build() {
            if (url == null) throw new IllegalStateException("url == null");
            return new Request(this);
        }
    }

}
