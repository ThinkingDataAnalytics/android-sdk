/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.core.network;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

/**
 * @author liulongbing
 * @since 2023/5/4
 */
public class TEHttpClient implements Call.Factory {

    final ExecutorService dispatcher;
    final int connectTimeout;
    final int readTimeout;
    final SSLSocketFactory sslSocketFactory;

    public TEHttpClient() {
        this(new Builder());
    }

    TEHttpClient(Builder builder) {
        this.dispatcher = builder.dispatcher;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.sslSocketFactory = builder.sslSocketFactory;
    }

    @Override
    public Call newCall(Request request) {
        return RealCall.newRealCall(this, request);
    }

    public static final class Builder {

        int connectTimeout;
        int readTimeout;
        SSLSocketFactory sslSocketFactory;
        ExecutorService dispatcher;

        public Builder() {
            dispatcher = TEHttpTaskManager.getExecutor();
            connectTimeout = 15000;
            readTimeout = 20000;
        }

        public Builder connectTimeout(int timeout) {
            connectTimeout = timeout;
            return this;
        }

        public Builder readTimeout(int timeout) {
            readTimeout = timeout;
            return this;
        }

        public Builder sslSocketFactory(SSLSocketFactory sslSocketFactory) {
            this.sslSocketFactory = sslSocketFactory;
            return this;
        }

        public Builder dispatcher(ExecutorService dispatcher) {
            if (dispatcher == null) throw new IllegalArgumentException("dispatcher == null");
            this.dispatcher = dispatcher;
            return this;
        }

        public TEHttpClient build() {
            return new TEHttpClient(this);
        }
    }

}
