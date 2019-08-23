package cn.thinkingdata.android.utils;

import java.io.IOException;

public interface RemoteService {
    String performRequest(String endpointUrl, String params) throws IOException, ServiceUnavailableException;

    class ServiceUnavailableException extends Exception {
        ServiceUnavailableException(String message) {
            super(message);
        }
    }
}
