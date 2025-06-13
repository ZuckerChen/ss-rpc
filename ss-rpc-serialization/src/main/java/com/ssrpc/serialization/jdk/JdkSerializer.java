package com.ssrpc.serialization.jdk;

import com.ssrpc.serialization.Serializer;
import com.ssrpc.serialization.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * JDK序列化器实现
 * 
 * 基于Java原生序列化机制的序列化器
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class JdkSerializer implements Serializer {
    
    private static final Logger log = LoggerFactory.getLogger(JdkSerializer.class);
    
    @Override
    public byte[] serialize(Object obj) throws SerializationException {
        if (obj == null) {
            return new byte[0];
        }
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            
            oos.writeObject(obj);
            oos.flush();
            
            byte[] result = baos.toByteArray();
            log.debug("Serialized object {} to {} bytes", obj.getClass().getSimpleName(), result.length);
            return result;
            
        } catch (IOException e) {
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
        
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            
            Object result = ois.readObject();
            log.debug("Deserialized {} bytes to object {}", data.length, clazz.getSimpleName());
            
            return clazz.cast(result);
            
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new SerializationException(
                SerializationException.ErrorCodes.DESERIALIZE_ERROR,
                "Failed to deserialize to class: " + clazz.getName(),
                e
            );
        }
    }
    
    @Override
    public String getType() {
        return "jdk";
    }
    
    @Override
    public boolean supports(Class<?> clazz) {
        // JDK序列化要求类实现Serializable接口
        return clazz != null && Serializable.class.isAssignableFrom(clazz);
    }
} 