package com.ssrpc.serialization.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssrpc.serialization.Serializer;
import com.ssrpc.serialization.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON序列化器实现.
 * 
 * 基于Jackson实现的JSON序列化器
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class JsonSerializer implements Serializer {
    
    private static final Logger log = LoggerFactory.getLogger(JsonSerializer.class);
    
    private final ObjectMapper objectMapper;
    
    public JsonSerializer() {
        this.objectMapper = new ObjectMapper();
        // 配置ObjectMapper
        objectMapper.findAndRegisterModules();
    }
    
    @Override
    public byte[] serialize(Object obj) throws SerializationException {
        if (obj == null) {
            return new byte[0];
        }
        
        try {
            byte[] result = objectMapper.writeValueAsBytes(obj);
            log.debug("Serialized object {} to {} bytes", obj.getClass().getSimpleName(), result.length);
            return result;
        } catch (Exception e) {
            throw new SerializationException(
                SerializationException.ErrorCodes.SERIALIZE_ERROR,
                "Failed to serialize object: " + obj.getClass().getName(),
                e
            );
        }
    }
    
    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) throws SerializationException {
        if (data == null || data.length == 0) {
            return null;
        }
        
        try {
            T result = objectMapper.readValue(data, clazz);
            log.debug("Deserialized {} bytes to object {}", data.length, clazz.getSimpleName());
            return result;
        } catch (Exception e) {
            throw new SerializationException(
                SerializationException.ErrorCodes.DESERIALIZE_ERROR,
                "Failed to deserialize to class: " + clazz.getName(),
                e
            );
        }
    }
    
    @Override
    public String getType() {
        return "json";
    }
    
    @Override
    public boolean supports(Class<?> clazz) {
        // JSON序列化支持大部分Java对象
        return clazz != null && !clazz.isInterface();
    }
} 