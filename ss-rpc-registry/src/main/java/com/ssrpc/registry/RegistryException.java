package com.ssrpc.registry;

/**
 * 服务注册异常.
 * 
 * 在服务注册过程中出现错误时抛出此异常
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class RegistryException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 错误码
     */
    private final int errorCode;
    
    /**
     * 构造方法
     */
    public RegistryException(String message) {
        super(message);
        this.errorCode = -1;
    }
    
    /**
     * 构造方法
     */
    public RegistryException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = -1;
    }
    
    /**
     * 构造方法
     */
    public RegistryException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 构造方法
     */
    public RegistryException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * 获取错误码
     */
    public int getErrorCode() {
        return errorCode;
    }
    
    /**
     * 常见错误码定义
     */
    public static class ErrorCodes {
        /** 服务已存在 */
        public static final int SERVICE_ALREADY_EXISTS = 2001;
        
        /** 服务不存在 */
        public static final int SERVICE_NOT_FOUND = 2002;
        
        /** 实例已存在 */
        public static final int INSTANCE_ALREADY_EXISTS = 2003;
        
        /** 实例不存在 */
        public static final int INSTANCE_NOT_FOUND = 2004;
        
        /** 注册失败 */
        public static final int REGISTRATION_FAILED = 2005;
        
        /** 注销失败 */
        public static final int UNREGISTRATION_FAILED = 2006;
        
        /** 心跳失败 */
        public static final int HEARTBEAT_FAILED = 2007;
        
        /** 连接失败 */
        public static final int CONNECTION_FAILED = 2008;
        
        /** 权限不足 */
        public static final int PERMISSION_DENIED = 2009;
        
        /** 配置错误 */
        public static final int CONFIGURATION_ERROR = 2010;
        
        /** 网络超时 */
        public static final int TIMEOUT = 2011;
        
        /** 未知错误 */
        public static final int UNKNOWN_ERROR = 9999;
    }
} 