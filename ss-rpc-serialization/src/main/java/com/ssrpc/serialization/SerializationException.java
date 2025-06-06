package com.ssrpc.serialization;

/**
 * 序列化异常.
 * 
 * 在序列化或反序列化过程中出现错误时抛出此异常
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class SerializationException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 错误码
     */
    private final int errorCode;
    
    /**
     * 构造方法
     */
    public SerializationException(String message) {
        super(message);
        this.errorCode = -1;
    }
    
    /**
     * 构造方法
     */
    public SerializationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = -1;
    }
    
    /**
     * 构造方法
     */
    public SerializationException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 构造方法
     */
    public SerializationException(int errorCode, String message, Throwable cause) {
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
        /** 序列化失败 */
        public static final int SERIALIZE_ERROR = 2001;
        
        /** 反序列化失败 */
        public static final int DESERIALIZE_ERROR = 2002;
        
        /** 不支持的类型 */
        public static final int UNSUPPORTED_TYPE = 2003;
        
        /** 数据格式错误 */
        public static final int INVALID_DATA_FORMAT = 2004;
        
        /** 配置错误 */
        public static final int CONFIGURATION_ERROR = 2005;
        
        /** 未知错误 */
        public static final int UNKNOWN_ERROR = 9999;
    }
} 