package com.ssrpc.transport.exception;

import com.ssrpc.core.exception.RpcException;

/**
 * 传输层异常
 * 
 * 封装网络传输过程中的各种异常情况
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class TransportException extends RpcException {
    
    // 定义错误码常量
    public static final int TRANSPORT_ERROR_CODE = 1001;
    public static final int CONNECTION_ERROR_CODE = 1002;
    public static final int TIMEOUT_ERROR_CODE = 1003;
    public static final int CODEC_ERROR_CODE = 1004;
    
    public TransportException(String message) {
        super(TRANSPORT_ERROR_CODE, message);
    }
    
    public TransportException(String message, Throwable cause) {
        super(TRANSPORT_ERROR_CODE, message, cause);
    }
    
    public TransportException(int errorCode, String message) {
        super(errorCode, message);
    }
    
    public TransportException(int errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    /**
     * 连接异常
     */
    public static TransportException connectionError(String message) {
        return new TransportException(CONNECTION_ERROR_CODE, message);
    }
    
    public static TransportException connectionError(String message, Throwable cause) {
        return new TransportException(CONNECTION_ERROR_CODE, message, cause);
    }
    
    /**
     * 超时异常
     */
    public static TransportException timeoutError(String message) {
        return new TransportException(TIMEOUT_ERROR_CODE, message);
    }
    
    public static TransportException timeoutError(String message, Throwable cause) {
        return new TransportException(TIMEOUT_ERROR_CODE, message, cause);
    }
    
    /**
     * 编解码异常
     */
    public static TransportException codecError(String message) {
        return new TransportException(CODEC_ERROR_CODE, message);
    }
    
    public static TransportException codecError(String message, Throwable cause) {
        return new TransportException(CODEC_ERROR_CODE, message, cause);
    }
    
    /**
     * 获取错误码对应的错误类型描述
     */
    public String getErrorType() {
        switch (getErrorCode()) {
            case CONNECTION_ERROR_CODE:
                return "CONNECTION_ERROR";
            case TIMEOUT_ERROR_CODE:
                return "TIMEOUT_ERROR";
            case CODEC_ERROR_CODE:
                return "CODEC_ERROR";
            case TRANSPORT_ERROR_CODE:
            default:
                return "TRANSPORT_ERROR";
        }
    }
} 