package com.ssrpc.transport.codec.spi;

import com.ssrpc.serialization.Serializer;
import com.ssrpc.serialization.SerializationException;

/**
 * 协议序列化适配器
 * 
 * 将核心序列化模块的接口适配到传输层的协议需求
 * 主要解决类型标识的差异：核心模块使用String，协议层使用byte
 *
 * @author chenzhang
 * @since 1.0.0
 */
public class ProtocolSerializerAdapter implements ProtocolSerializer {
    
    private final Serializer coreSerializer;
    private final byte protocolType;
    
    public ProtocolSerializerAdapter(Serializer coreSerializer, byte protocolType) {
        this.coreSerializer = coreSerializer;
        this.protocolType = protocolType;
    }
    
    @Override
    public byte getType() {
        return protocolType;
    }
    
    @Override
    public byte[] serialize(Object obj) throws Exception {
        try {
            return coreSerializer.serialize(obj);
        } catch (SerializationException e) {
            throw new Exception("Protocol serialization failed", e);
        }
    }
    
    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) throws Exception {
        try {
            return coreSerializer.deserialize(data, clazz);
        } catch (SerializationException e) {
            throw new Exception("Protocol deserialization failed", e);
        }
    }
    
    /**
     * 获取核心序列化器类型名称
     */
    public String getCoreSerializerType() {
        return coreSerializer.getType();
    }
    
    /**
     * 获取核心序列化器
     */
    public Serializer getCoreSerializer() {
        return coreSerializer;
    }
} 