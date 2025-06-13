package com.ssrpc.transport.codec;

import java.util.List;

import com.ssrpc.core.rpc.RpcRequest;
import com.ssrpc.core.rpc.RpcResponse;
import com.ssrpc.transport.codec.spi.ProtocolSerializer;
import com.ssrpc.transport.codec.spi.SerializerFactory;
import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RPC编解码器测试类
 *
 * @author chenzhang
 * @since 1.0.0
 */
public class RpcCodecTest extends BaseCodecTest {
    
    private RpcCodec codec;
    private ProtocolSerializer serializer;
    
    @BeforeEach
    void setUp() {
        serializer = SerializerFactory.getDefaultSerializer();
        codec = new RpcCodec(serializer);
    }
    
    @Test
    void testEncodeRequest() throws Exception {
        // 准备测试数据
        RpcRequest request = createTestRequest();
        ByteBuf out = createByteBuf(new byte[0]);
        
        // 执行编码
        codec.encode(createMockContext(true), request, out);
        
        // 验证编码结果
        assertTrue(out.readableBytes() > 0);
        assertEquals(0x12345678, out.readInt()); // 验证魔数
        assertEquals(serializer.getType(), out.readByte()); // 验证序列化类型
        int length = out.readInt(); // 读取数据长度
        assertTrue(length > 0);
        byte[] data = new byte[length];
        out.readBytes(data);
        
        // 验证序列化后的数据可以被反序列化
        RpcRequest decoded = serializer.deserialize(data, RpcRequest.class);
        assertEquals(request.getRequestId(), decoded.getRequestId());
        assertEquals(request.getInterfaceName(), decoded.getInterfaceName());
        assertEquals(request.getMethodName(), decoded.getMethodName());
        
        // 释放资源
        out.release();
    }
    
    @Test
    void testEncodeResponse() throws Exception {
        // 准备测试数据
        RpcResponse response = createTestResponse();
        ByteBuf out = createByteBuf(new byte[0]);
        
        // 执行编码
        codec.encode(createMockContext(false), response, out);
        
        // 验证编码结果
        assertTrue(out.readableBytes() > 0);
        assertEquals(0x12345678, out.readInt()); // 验证魔数
        assertEquals(serializer.getType(), out.readByte()); // 验证序列化类型
        int length = out.readInt(); // 读取数据长度
        assertTrue(length > 0);
        byte[] data = new byte[length];
        out.readBytes(data);
        
        // 验证序列化后的数据可以被反序列化
        RpcResponse decoded = serializer.deserialize(data, RpcResponse.class);
        assertEquals(response.getRequestId(), decoded.getRequestId());
        assertEquals(response.getStatus(), decoded.getStatus());
        assertEquals(response.getResult(), decoded.getResult());
        
        // 释放资源
        out.release();
    }
    
    @Test
    void testDecodeRequest() throws Exception {
        // 准备测试数据
        RpcRequest request = createTestRequest();
        byte[] data = serializer.serialize(request);
        
        // 创建输入ByteBuf
        ByteBuf in = createByteBuf(new byte[0]);
        in.writeInt(0x12345678); // 写入魔数
        in.writeByte(serializer.getType()); // 写入序列化类型
        in.writeInt(data.length); // 写入数据长度
        in.writeBytes(data); // 写入数据
        
        // 执行解码
        List<Object> out = createOutList();
        codec.decode(createMockContext(false), in, out);
        
        // 验证解码结果
        assertEquals(1, out.size());
        assertTrue(out.get(0) instanceof RpcRequest);
        RpcRequest decoded = (RpcRequest) out.get(0);
        assertEquals(request.getRequestId(), decoded.getRequestId());
        assertEquals(request.getInterfaceName(), decoded.getInterfaceName());
        assertEquals(request.getMethodName(), decoded.getMethodName());
        
        // 释放资源
        in.release();
    }
    
    @Test
    void testDecodeResponse() throws Exception {
        // 准备测试数据
        RpcResponse response = createTestResponse();
        byte[] data = serializer.serialize(response);
        
        // 创建输入ByteBuf
        ByteBuf in = createByteBuf(new byte[0]);
        in.writeInt(0x12345678); // 写入魔数
        in.writeByte(serializer.getType()); // 写入序列化类型
        in.writeInt(data.length); // 写入数据长度
        in.writeBytes(data); // 写入数据
        
        // 执行解码
        List<Object> out = createOutList();
        codec.decode(createMockContext(true), in, out);
        
        // 验证解码结果
        assertEquals(1, out.size());
        assertTrue(out.get(0) instanceof RpcResponse);
        RpcResponse decoded = (RpcResponse) out.get(0);
        assertEquals(response.getRequestId(), decoded.getRequestId());
        assertEquals(response.getStatus(), decoded.getStatus());
        assertEquals(response.getResult(), decoded.getResult());
        
        // 释放资源
        in.release();
    }
    
    @Test
    void testDecodeInvalidMagic() throws Exception {
        // 创建输入ByteBuf，使用错误的魔数
        ByteBuf in = createByteBuf(new byte[0]);
        in.writeInt(0x87654321); // 写入错误的魔数
        
        // 执行解码
        List<Object> out = createOutList();
        codec.decode(createMockContext(true), in, out);
        
        // 验证解码结果
        assertTrue(out.isEmpty());
        
        // 释放资源
        in.release();
    }
    
    @Test
    void testDecodeInvalidLength() throws Exception {
        // 创建输入ByteBuf，使用非法的数据长度
        ByteBuf in = createByteBuf(new byte[0]);
        in.writeInt(0x12345678); // 写入魔数
        in.writeByte(serializer.getType()); // 写入序列化类型
        in.writeInt(-1); // 写入非法的数据长度
        
        // 执行解码
        List<Object> out = createOutList();
        codec.decode(createMockContext(true), in, out);
        
        // 验证解码结果
        assertTrue(out.isEmpty());
        
        // 释放资源
        in.release();
    }
    
    @Test
    void testDecodeInsufficientData() throws Exception {
        // 创建输入ByteBuf，数据不完整
        ByteBuf in = createByteBuf(new byte[0]);
        in.writeInt(0x12345678); // 写入魔数
        in.writeByte(serializer.getType()); // 写入序列化类型
        in.writeInt(100); // 写入数据长度
        // 不写入实际数据
        
        // 执行解码
        List<Object> out = createOutList();
        codec.decode(createMockContext(true), in, out);
        
        // 验证解码结果
        assertTrue(out.isEmpty());
        
        // 释放资源
        in.release();
    }
} 