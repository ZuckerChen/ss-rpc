package com.ssrpc.serialization;

import com.ssrpc.core.spi.SPI;

/**
 * 序列化器接口.
 * 
 * 提供对象序列化和反序列化功能
 * 支持多种序列化方式：JSON、Kryo、Protobuf等
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@SPI("json")
public interface Serializer {
    
    /**
     * 序列化对象
     * 
     * @param obj 待序列化的对象
     * @return 序列化后的字节数组
     * @throws SerializationException 序列化异常
     */
    byte[] serialize(Object obj) throws SerializationException;
    
    /**
     * 反序列化对象
     * 
     * @param data 序列化的字节数组
     * @param clazz 目标类型
     * @param <T> 目标类型
     * @return 反序列化后的对象
     * @throws SerializationException 序列化异常
     */
    <T> T deserialize(byte[] data, Class<T> clazz) throws SerializationException;
    
    /**
     * 获取序列化器类型
     * 
     * @return 序列化器类型
     */
    String getType();
    
    /**
     * 获取序列化器名称
     * 
     * @return 序列化器名称
     */
    default String getName() {
        return getType();
    }
    
    /**
     * 检查是否支持指定类型的序列化
     * 
     * @param clazz 待检查的类型
     * @return true表示支持，false表示不支持
     */
    default boolean supports(Class<?> clazz) {
        return true; // 默认支持所有类型
    }
} 