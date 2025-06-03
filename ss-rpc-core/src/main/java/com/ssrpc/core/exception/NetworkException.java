package com.ssrpc.core.exception;

/**
 * 网络通信异常.
 * 
 * 封装网络通信过程中可能发生的各种异常
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class NetworkException extends RpcException {
    
    private static final long serialVersionUID = 1L;
    
    public NetworkException() {
        super();
    }
    
    public NetworkException(String message) {
        super(message);
    }
    
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public NetworkException(Throwable cause) {
        super(cause);
    }
} 