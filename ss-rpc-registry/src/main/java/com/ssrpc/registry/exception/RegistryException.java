package com.ssrpc.registry.exception;

/**
 * 注册中心异常.
 * 
 * 封装注册中心操作过程中的各种异常情况
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class RegistryException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 错误代码
     */
    private final String errorCode;
    
    /**
     * 构造方法
     */
    public RegistryException(String message) {
        super(message);
        this.errorCode = ErrorCodes.UNKNOWN_ERROR;
    }
    
    /**
     * 构造方法
     */
    public RegistryException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 构造方法
     */
    public RegistryException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCodes.UNKNOWN_ERROR;
    }
    
    /**
     * 构造方法
     */
    public RegistryException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * 获取错误代码
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * 错误代码常量
     */
    public static class ErrorCodes {
        public static final String UNKNOWN_ERROR = "UNKNOWN_ERROR";
        public static final String CONNECTION_FAILED = "CONNECTION_FAILED";
        public static final String REGISTRATION_FAILED = "REGISTRATION_FAILED";
        public static final String UNREGISTRATION_FAILED = "UNREGISTRATION_FAILED";
        public static final String DISCOVERY_FAILED = "DISCOVERY_FAILED";
        public static final String CONFIGURATION_ERROR = "CONFIGURATION_ERROR";
        public static final String TIMEOUT_ERROR = "TIMEOUT_ERROR";
        public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
        public static final String AUTHORIZATION_FAILED = "AUTHORIZATION_FAILED";
        public static final String SERVICE_NOT_FOUND = "SERVICE_NOT_FOUND";
        public static final String INSTANCE_NOT_FOUND = "INSTANCE_NOT_FOUND";
        public static final String INVALID_PARAMETER = "INVALID_PARAMETER";
        public static final String REGISTRY_UNAVAILABLE = "REGISTRY_UNAVAILABLE";
    }
    
    @Override
    public String toString() {
        return "RegistryException{" +
                "errorCode='" + errorCode + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';
    }
} 