package cn.thinkingdata.android;

// Debug 模式下的异常
public class TDDebugException extends RuntimeException {
    public TDDebugException(String message) {
        super(message);
    }

    public TDDebugException(Throwable cause) {
        super(cause);
    }
}
