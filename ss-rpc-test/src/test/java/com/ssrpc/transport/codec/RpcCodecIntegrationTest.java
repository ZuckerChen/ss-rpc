package com.ssrpc.transport.codec;

import com.ssrpc.core.rpc.RpcRequest;
import com.ssrpc.core.rpc.RpcResponse;
import com.ssrpc.transport.codec.spi.ProtocolSerializer;
import com.ssrpc.transport.codec.spi.SerializerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RPC编解码器集成测试类
 * 使用Netty的EmbeddedChannel进行端到端测试
 *
 * @author chenzhang
 * @since 1.0.0
 */
public class RpcCodecIntegrationTest extends BaseCodecTest {
    
    @Test
    void testRequestResponseWithJdkSerializer() {
        ProtocolSerializer jdkSerializer = SerializerFactory.getSerializer((byte) 1);
        testRequestResponseFlow(jdkSerializer);
    }
    
    @Test
    void testRequestResponseWithJsonSerializer() {
        ProtocolSerializer jsonSerializer = SerializerFactory.getSerializer((byte) 2);
        testRequestResponseFlow(jsonSerializer);
    }
    
    private void testRequestResponseFlow(ProtocolSerializer serializer) {
        // 测试请求编解码
        testRequestFlow(serializer);
        
        // 测试响应编解码
        testResponseFlow(serializer);
    }
    
    private void testRequestFlow(ProtocolSerializer serializer) {
        // 创建客户端通道用于编码请求
        EmbeddedChannel clientChannel = new EmbeddedChannel(new RpcCodec(serializer));
        
        // 创建服务端通道用于解码请求
        EmbeddedChannel serverChannel = new EmbeddedChannel(new RpcCodec(serializer));
        
        // 客户端编码请求
        RpcRequest request = createTestRequest();
        assertTrue(clientChannel.writeOutbound(request));
        ByteBuf encodedRequest = clientChannel.readOutbound();
        assertNotNull(encodedRequest);
        
        // 服务端解码请求
        assertTrue(serverChannel.writeInbound(encodedRequest));
        RpcRequest decodedRequest = serverChannel.readInbound();
        assertNotNull(decodedRequest);
        assertEquals(request.getRequestId(), decodedRequest.getRequestId());
        assertEquals(request.getInterfaceName(), decodedRequest.getInterfaceName());
        assertEquals(request.getMethodName(), decodedRequest.getMethodName());
        
        // 关闭通道
        clientChannel.finish();
        serverChannel.finish();
    }
    
    private void testResponseFlow(ProtocolSerializer serializer) {
        // 创建服务端通道用于编码响应
        EmbeddedChannel serverChannel = new EmbeddedChannel(new RpcCodec(serializer));
        
        // 创建客户端通道用于解码响应（需要设置客户端标识）
        EmbeddedChannel clientChannel = new EmbeddedChannel(new RpcCodec(serializer));
        // 设置客户端标识
        clientChannel.attr(RpcRequest.ATTRIBUTE_KEY).set(true);
        
        // 服务端编码响应
        RpcResponse response = createTestResponse();
        assertTrue(serverChannel.writeOutbound(response));
        ByteBuf encodedResponse = serverChannel.readOutbound();
        assertNotNull(encodedResponse);
        
        // 客户端解码响应
        assertTrue(clientChannel.writeInbound(encodedResponse));
        RpcResponse decodedResponse = clientChannel.readInbound();
        assertNotNull(decodedResponse);
        assertEquals(response.getRequestId(), decodedResponse.getRequestId());
        assertEquals(response.getStatus(), decodedResponse.getStatus());
        assertEquals(response.getResult(), decodedResponse.getResult());
        
        // 关闭通道
        serverChannel.finish();
        clientChannel.finish();
    }
    
    @Test
    void testLargeMessage() {
        // 创建客户端和服务端通道
        EmbeddedChannel clientChannel = new EmbeddedChannel(new RpcCodec());
        EmbeddedChannel serverChannel = new EmbeddedChannel(new RpcCodec());
        
        // 创建大数据量的请求
        RpcRequest request = createTestRequest();
        StringBuilder largeString = new StringBuilder();
        for (int i = 0; i < 10000; i++) { // 减少数据量以提高测试速度
            largeString.append("test data ");
        }
        request.setParameters(new Object[]{largeString.toString()});
        
        // 客户端编码请求
        assertTrue(clientChannel.writeOutbound(request));
        ByteBuf encodedRequest = clientChannel.readOutbound();
        assertNotNull(encodedRequest);
        
        // 服务端解码请求
        assertTrue(serverChannel.writeInbound(encodedRequest));
        RpcRequest decodedRequest = serverChannel.readInbound();
        assertNotNull(decodedRequest);
        assertEquals(request.getRequestId(), decodedRequest.getRequestId());
        assertEquals(request.getParameters()[0], decodedRequest.getParameters()[0]);
        
        // 关闭通道
        clientChannel.finish();
        serverChannel.finish();
    }
    
    @Test
    void testMultipleMessages() {
        // 创建客户端和服务端通道
        EmbeddedChannel clientChannel = new EmbeddedChannel(new RpcCodec());
        EmbeddedChannel serverChannel = new EmbeddedChannel(new RpcCodec());
        // 设置客户端标识
        clientChannel.attr(RpcRequest.ATTRIBUTE_KEY).set(true);
        
        // 发送多个请求和响应
        for (int i = 0; i < 5; i++) { // 减少循环次数以提高测试速度
            // 测试请求：客户端编码 -> 服务端解码
            RpcRequest request = createTestRequest();
            request.setMethodName("testMethod" + i);
            assertTrue(clientChannel.writeOutbound(request));
            ByteBuf encodedRequest = clientChannel.readOutbound();
            assertNotNull(encodedRequest);
            
            assertTrue(serverChannel.writeInbound(encodedRequest));
            RpcRequest decodedRequest = serverChannel.readInbound();
            assertNotNull(decodedRequest);
            assertEquals(request.getRequestId(), decodedRequest.getRequestId());
            assertEquals(request.getMethodName(), decodedRequest.getMethodName());
            
            // 测试响应：服务端编码 -> 客户端解码
            RpcResponse response = createTestResponse();
            response.setResult("result" + i);
            assertTrue(serverChannel.writeOutbound(response));
            ByteBuf encodedResponse = serverChannel.readOutbound();
            assertNotNull(encodedResponse);
            
            assertTrue(clientChannel.writeInbound(encodedResponse));
            RpcResponse decodedResponse = clientChannel.readInbound();
            assertNotNull(decodedResponse);
            assertEquals(response.getRequestId(), decodedResponse.getRequestId());
            assertEquals(response.getResult(), decodedResponse.getResult());
        }
        
        // 关闭通道
        clientChannel.finish();
        serverChannel.finish();
    }
    
    @Test
    void testPartialMessages() {
        // 创建客户端和服务端通道
        EmbeddedChannel clientChannel = new EmbeddedChannel(new RpcCodec());
        EmbeddedChannel serverChannel = new EmbeddedChannel(new RpcCodec());
        
        // 客户端编码请求
        RpcRequest request = createTestRequest();
        assertTrue(clientChannel.writeOutbound(request));
        ByteBuf encodedRequest = clientChannel.readOutbound();
        assertNotNull(encodedRequest);
        
        // 分段发送数据到服务端
        int totalLength = encodedRequest.readableBytes();
        int firstPartLength = totalLength / 2;
        
        // 创建独立的 ByteBuf 副本，避免引用计数问题
        ByteBuf firstPart = encodedRequest.copy(0, firstPartLength);
        ByteBuf secondPart = encodedRequest.copy(firstPartLength, totalLength - firstPartLength);
        
        // 释放原始 ByteBuf
        encodedRequest.release();
        
        // 服务端接收第一部分，应该没有解码结果
        serverChannel.writeInbound(firstPart);
        assertNull(serverChannel.readInbound());
        
        // 服务端接收第二部分，应该得到完整的解码结果
        serverChannel.writeInbound(secondPart);
        RpcRequest decodedRequest = serverChannel.readInbound();
        assertNotNull(decodedRequest);
        assertEquals(request.getRequestId(), decodedRequest.getRequestId());
        
        // 关闭通道
        clientChannel.finish();
        serverChannel.finish();
    }
} 