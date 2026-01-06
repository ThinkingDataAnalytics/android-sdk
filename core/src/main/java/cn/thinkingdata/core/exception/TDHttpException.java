/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.core.exception;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2023/12/26
 * @since
 */
public class TDHttpException extends Exception {

    public static final int ERROR_CONNECT_TIME_OUT = -10001;
    public static final int ERROR_EXCEPTION = -10002;

    public int errorCode;

    public TDHttpException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

}
