package com.ssrpc.core.exception;

/**
 * RPC异常类.
 * 
 * 所有RPC相关异常的基类
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class RpcException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 错误代码
     */
    private int errorCode;
    
    public RpcException() {
        super();
    }
    
    public RpcException(String message) {
        super(message);
    }
    
    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public RpcException(Throwable cause) {
        super(cause);
    }
    
    public RpcException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public RpcException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
} 