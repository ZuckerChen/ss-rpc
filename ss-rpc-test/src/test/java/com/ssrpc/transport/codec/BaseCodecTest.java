package com.ssrpc.transport.codec;

import com.ssrpc.core.rpc.RpcRequest;
import com.ssrpc.core.rpc.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 编解码器测试基类
 *
 * @author chenzhang
 * @since 1.0.0
 */
public abstract class BaseCodecTest {
    
    /**
     * 创建模拟的ChannelHandlerContext
     */
    protected ChannelHandlerContext createMockContext(boolean isClient) {
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        Channel channel = Mockito.mock(Channel.class);
        @SuppressWarnings("unchecked")
        Attribute<Boolean> attr = Mockito.mock(Attribute.class);
        
        Mockito.when(ctx.channel()).thenReturn(channel);
        Mockito.when(channel.attr(RpcRequest.ATTRIBUTE_KEY)).thenReturn(attr);
        Mockito.when(attr.get()).thenReturn(isClient);
        
        return ctx;
    }
    
    /**
     * 创建测试用的RPC请求对象
     */
    protected RpcRequest createTestRequest() {
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
    
    /**
     * 创建测试用的RPC响应对象
     */
    protected RpcResponse createTestResponse() {
        RpcResponse response = new RpcResponse();
        response.setRequestId(UUID.randomUUID().toString());
        response.setResult("test result");
        response.setStatus(RpcResponse.SUCCESS);
        return response;
    }
    
    /**
     * 创建ByteBuf并写入数据
     */
    protected ByteBuf createByteBuf(byte[] data) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(data);
        return buf;
    }
    
    /**
     * 创建用于接收解码结果的列表
     */
    protected List<Object> createOutList() {
        return new ArrayList<>();
    }
} 