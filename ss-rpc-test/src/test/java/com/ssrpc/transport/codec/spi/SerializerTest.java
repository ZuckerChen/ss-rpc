package com.ssrpc.transport.codec.spi;

import com.ssrpc.core.rpc.RpcRequest;
import com.ssrpc.core.rpc.RpcResponse;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 协议序列化器测试类
 * 
 * 测试传输层的协议序列化器功能
 *
 * @author chenzhang
 * @since 1.0.0
 */
public class SerializerTest {
    
    @Test
    void testJdkProtocolSerializer() throws Exception {
        ProtocolSerializer serializer = SerializerFactory.getSerializer((byte) 1);
        testProtocolSerializer(serializer, (byte) 1);
    }
    
    @Test
    void testJsonProtocolSerializer() throws Exception {
        ProtocolSerializer serializer = SerializerFactory.getSerializer((byte) 2);
        testProtocolSerializer(serializer, (byte) 2);
    }
    
    private void testProtocolSerializer(ProtocolSerializer serializer, byte expectedType) throws Exception {
        // 验证序列化器类型
        assertEquals(expectedType, serializer.getType());
        
        // 测试RpcRequest序列化
        RpcRequest request = createTestRequest();
        byte[] requestData = serializer.serialize(request);
        assertNotNull(requestData);
        assertTrue(requestData.length > 0);
        
        RpcRequest decodedRequest = serializer.deserialize(requestData, RpcRequest.class);
        assertNotNull(decodedRequest);
        assertEquals(request.getRequestId(), decodedRequest.getRequestId());
        assertEquals(request.getInterfaceName(), decodedRequest.getInterfaceName());
        assertEquals(request.getMethodName(), decodedRequest.getMethodName());
        assertEquals(request.getVersion(), decodedRequest.getVersion());
        assertEquals(request.getTimeout(), decodedRequest.getTimeout());
        
        // 测试RpcResponse序列化
        RpcResponse response = createTestResponse();
        byte[] responseData = serializer.serialize(response);
        assertNotNull(responseData);
        assertTrue(responseData.length > 0);
        
        RpcResponse decodedResponse = serializer.deserialize(responseData, RpcResponse.class);
        assertNotNull(decodedResponse);
        assertEquals(response.getRequestId(), decodedResponse.getRequestId());
        assertEquals(response.getStatus(), decodedResponse.getStatus());
        assertEquals(response.getResult(), decodedResponse.getResult());
        
        // 验证计算属性正常工作
        assertTrue(decodedResponse.isSuccess());
        assertFalse(decodedResponse.isError());
    }
    
    private RpcRequest createTestRequest() {
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setInterfaceName("com.ssrpc.test.TestService");
        request.setMethodName("testMethod");
        request.setParameterTypes(new Class<?>[]{String.class, Integer.class});
        request.setParameters(new Object[]{"test", 123});
        request.setVersion("1.0.0");
        request.setTimeout(5000L);
        return request;
    }
    
    private RpcResponse createTestResponse() {
        RpcResponse response = new RpcResponse();
        response.setRequestId(UUID.randomUUID().toString());
        response.setResult("test result");
        response.setStatus(RpcResponse.SUCCESS);
        response.setProcessTime(100L);
        return response;
    }
    
    @Test
    void testProtocolSerializerFactory() {
        // 测试获取默认协议序列化器
        ProtocolSerializer defaultSerializer = SerializerFactory.getDefaultSerializer();
        assertNotNull(defaultSerializer);
        assertEquals((byte) 2, defaultSerializer.getType()); // 默认应该是JSON
        
        // 测试根据类型获取协议序列化器
        ProtocolSerializer jdkSerializer = SerializerFactory.getSerializer((byte) 1);
        assertNotNull(jdkSerializer);
        assertEquals((byte) 1, jdkSerializer.getType());
        
        ProtocolSerializer jsonSerializer = SerializerFactory.getSerializer((byte) 2);
        assertNotNull(jsonSerializer);
        assertEquals((byte) 2, jsonSerializer.getType());
        
        // 测试适配器功能
        assertTrue(jdkSerializer instanceof ProtocolSerializerAdapter);
        assertTrue(jsonSerializer instanceof ProtocolSerializerAdapter);
        
        ProtocolSerializerAdapter jdkAdapter = (ProtocolSerializerAdapter) jdkSerializer;
        assertEquals("jdk", jdkAdapter.getCoreSerializerType());
        
        ProtocolSerializerAdapter jsonAdapter = (ProtocolSerializerAdapter) jsonSerializer;
        assertEquals("json", jsonAdapter.getCoreSerializerType());
    }
    
    @Test
    void testSerializerSupport() {
        // 测试支持的序列化器类型
        assertTrue(SerializerFactory.isSupported((byte) 1)); // JDK
        assertTrue(SerializerFactory.isSupported((byte) 2)); // JSON
        assertFalse(SerializerFactory.isSupported((byte) 99)); // 不存在的类型
    }
    
    @Test
    void testInvalidProtocolSerializer() {
        // 测试获取不存在的协议序列化器类型
        assertThrows(IllegalArgumentException.class, () -> {
            SerializerFactory.getSerializer((byte) 99);
        });
    }
    
    @Test
    void testNullSerialization() throws Exception {
        ProtocolSerializer serializer = SerializerFactory.getDefaultSerializer();
        
        // 测试null对象序列化
        byte[] nullData = serializer.serialize(null);
        assertNotNull(nullData);
        assertEquals(0, nullData.length);
        
        // 测试空数据反序列化
        RpcRequest nullRequest = serializer.deserialize(new byte[0], RpcRequest.class);
        assertNull(nullRequest);
    }
} 