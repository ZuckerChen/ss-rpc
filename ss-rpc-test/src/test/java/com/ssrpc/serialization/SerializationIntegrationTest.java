package com.ssrpc.serialization;

import com.ssrpc.core.rpc.RpcRequest;
import com.ssrpc.core.rpc.RpcResponse;
import com.ssrpc.core.spi.ExtensionLoader;
import com.ssrpc.transport.codec.spi.ProtocolSerializer;
import com.ssrpc.transport.codec.spi.SerializerFactory;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 序列化集成测试
 * 
 * 验证核心序列化模块与传输层的集成
 *
 * @author chenzhang
 * @since 1.0.0
 */
public class SerializationIntegrationTest {
    
    @Test
    void testCoreSerializationModuleLoading() {
        // 测试核心序列化模块的 SPI 加载
        ExtensionLoader<Serializer> extensionLoader = ExtensionLoader.getExtensionLoader(Serializer.class);
        
        // 测试 JSON 序列化器
        Serializer jsonSerializer = extensionLoader.getExtension("json");
        assertNotNull(jsonSerializer);
        assertEquals("json", jsonSerializer.getType());
        
        // 测试 JDK 序列化器
        Serializer jdkSerializer = extensionLoader.getExtension("jdk");
        assertNotNull(jdkSerializer);
        assertEquals("jdk", jdkSerializer.getType());
    }
    
    @Test
    void testProtocolSerializerFactory() {
        // 测试协议序列化器工厂
        ProtocolSerializer jsonProtocolSerializer = SerializerFactory.getSerializer((byte) 2);
        assertNotNull(jsonProtocolSerializer);
        assertEquals((byte) 2, jsonProtocolSerializer.getType());
        
        ProtocolSerializer jdkProtocolSerializer = SerializerFactory.getSerializer((byte) 1);
        assertNotNull(jdkProtocolSerializer);
        assertEquals((byte) 1, jdkProtocolSerializer.getType());
    }
    
    @Test
    void testRpcRequestSerialization() throws Exception {
        // 创建测试请求
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setInterfaceName("com.ssrpc.test.TestService");
        request.setMethodName("testMethod");
        request.setParameterTypes(new Class<?>[]{String.class, Integer.class});
        request.setParameters(new Object[]{"test", 123});
        request.setVersion("1.0.0");
        request.setTimeout(5000L);
        
        // 测试 JSON 序列化
        ProtocolSerializer jsonSerializer = SerializerFactory.getSerializer((byte) 2);
        byte[] jsonData = jsonSerializer.serialize(request);
        assertNotNull(jsonData);
        assertTrue(jsonData.length > 0);
        
        RpcRequest deserializedRequest = jsonSerializer.deserialize(jsonData, RpcRequest.class);
        assertNotNull(deserializedRequest);
        assertEquals(request.getRequestId(), deserializedRequest.getRequestId());
        assertEquals(request.getInterfaceName(), deserializedRequest.getInterfaceName());
        assertEquals(request.getMethodName(), deserializedRequest.getMethodName());
        
        // 测试 JDK 序列化
        ProtocolSerializer jdkSerializer = SerializerFactory.getSerializer((byte) 1);
        byte[] jdkData = jdkSerializer.serialize(request);
        assertNotNull(jdkData);
        assertTrue(jdkData.length > 0);
        
        RpcRequest jdkDeserializedRequest = jdkSerializer.deserialize(jdkData, RpcRequest.class);
        assertNotNull(jdkDeserializedRequest);
        assertEquals(request.getRequestId(), jdkDeserializedRequest.getRequestId());
        assertEquals(request.getInterfaceName(), jdkDeserializedRequest.getInterfaceName());
        assertEquals(request.getMethodName(), jdkDeserializedRequest.getMethodName());
    }
    
    @Test
    void testRpcResponseSerialization() throws Exception {
        // 创建测试响应
        RpcResponse response = new RpcResponse();
        response.setRequestId(UUID.randomUUID().toString());
        response.setResult("test result");
        response.setStatus(RpcResponse.SUCCESS);
        response.setProcessTime(100L);
        
        // 测试 JSON 序列化（重点测试 isSuccess/isError 字段的处理）
        ProtocolSerializer jsonSerializer = SerializerFactory.getSerializer((byte) 2);
        byte[] jsonData = jsonSerializer.serialize(response);
        assertNotNull(jsonData);
        assertTrue(jsonData.length > 0);
        
        RpcResponse deserializedResponse = jsonSerializer.deserialize(jsonData, RpcResponse.class);
        assertNotNull(deserializedResponse);
        assertEquals(response.getRequestId(), deserializedResponse.getRequestId());
        assertEquals(response.getResult(), deserializedResponse.getResult());
        assertEquals(response.getStatus(), deserializedResponse.getStatus());
        assertEquals(response.getProcessTime(), deserializedResponse.getProcessTime());
        
        // 验证计算属性正常工作
        assertTrue(deserializedResponse.isSuccess());
        assertFalse(deserializedResponse.isError());
    }
    
    @Test
    void testDefaultSerializer() {
        // 测试默认序列化器
        ProtocolSerializer defaultSerializer = SerializerFactory.getDefaultSerializer();
        assertNotNull(defaultSerializer);
        assertEquals((byte) 2, defaultSerializer.getType()); // 默认应该是 JSON
    }
    
    @Test
    void testSerializerSupport() {
        // 测试序列化器类型支持检查
        assertTrue(SerializerFactory.isSupported((byte) 1)); // JDK
        assertTrue(SerializerFactory.isSupported((byte) 2)); // JSON
        assertFalse(SerializerFactory.isSupported((byte) 99)); // 不存在的类型
    }
} 