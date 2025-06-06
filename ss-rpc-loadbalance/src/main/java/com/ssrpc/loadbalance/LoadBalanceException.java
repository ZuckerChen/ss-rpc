package com.ssrpc.loadbalance;

/**
 * 负载均衡异常.
 * 
 * 在负载均衡过程中出现错误时抛出此异常
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class LoadBalanceException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 错误码
     */
    private final int errorCode;
    
    /**
     * 构造方法
     */
    public LoadBalanceException(String message) {
        super(message);
        this.errorCode = -1;
    }
    
    /**
     * 构造方法
     */
    public LoadBalanceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = -1;
    }
    
    /**
     * 构造方法
     */
    public LoadBalanceException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 构造方法
     */
    public LoadBalanceException(int errorCode, String message, Throwable cause) {
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
        /** 没有可用的服务实例 */
        public static final int NO_AVAILABLE_INSTANCE = 3001;
        
        /** 无效的负载均衡策略 */
        public static final int INVALID_STRATEGY = 3002;
        
        /** 权重配置错误 */
        public static final int INVALID_WEIGHT = 3003;
        
        /** 一致性哈希环异常 */
        public static final int HASH_RING_ERROR = 3004;
        
        /** 配置错误 */
        public static final int CONFIGURATION_ERROR = 3005;
        
        /** 未知错误 */
        public static final int UNKNOWN_ERROR = 9999;
    }
} 