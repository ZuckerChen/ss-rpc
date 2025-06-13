package com.ssrpc.transport.codec.spi;

/**
 * 协议序列化接口，专门为传输层协议服务
 * 
 * 与核心序列化模块的区别：
 * 1. 使用 byte 类型标识序列化器类型（协议需求）
 * 2. 简化的异常处理（传输层需求）
 *
 * @author chenzhang
 * @since 1.0.0
 */
public interface ProtocolSerializer {
    
    /**
     * 获取序列化器类型
     *
     * @return 序列化器类型
     */
    byte getType();
    
    /**
     * 序列化对象
     *
     * @param obj 待序列化的对象
     * @return 序列化后的字节数组
     * @throws Exception 序列化异常
     */
    byte[] serialize(Object obj) throws Exception;
    
    /**
     * 反序列化对象
     *
     * @param data 待反序列化的字节数组
     * @param clazz 目标类型
     * @return 反序列化后的对象
     * @throws Exception 反序列化异常
     */
    <T> T deserialize(byte[] data, Class<T> clazz) throws Exception;
} 