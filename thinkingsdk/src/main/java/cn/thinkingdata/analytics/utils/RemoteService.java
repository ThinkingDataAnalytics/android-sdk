/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.utils;

import java.io.IOException;
import java.util.Map;
import javax.net.ssl.SSLSocketFactory;

/**
 * RemoteService.
 * */
public interface RemoteService {
    String performRequest(String endpointUrl, String params,
                          boolean debug, SSLSocketFactory sslSocketFactory,
                          final Map<String, String> extraHeaders)
            throws IOException, ServiceUnavailableException;

    /**
     * ServiceUnavailableException.
     * */
    class ServiceUnavailableException extends Exception {
        ServiceUnavailableException(String message) {
            super(message);
        }
    }
}
