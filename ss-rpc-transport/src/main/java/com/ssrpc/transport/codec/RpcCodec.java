package com.ssrpc.transport.codec;

import com.ssrpc.core.rpc.RpcRequest;
import com.ssrpc.core.rpc.RpcResponse;
import com.ssrpc.transport.codec.spi.ProtocolSerializer;
import com.ssrpc.transport.codec.spi.SerializerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * RPC编解码器，支持多种序列化方式
 * 
 * 协议格式：
 * +--------+----------------+----------------+----------------+
 * | 魔数   | 序列化类型     | 数据长度        | 数据内容        |
 * | 4字节  | 1字节          | 4字节          | N字节          |
 * +--------+----------------+----------------+----------------+
 *
 * @author chenzhang
 * @since 1.0.0
 */
public class RpcCodec extends ByteToMessageCodec<Object> {
    
    private static final Logger log = LoggerFactory.getLogger(RpcCodec.class);
    
    // 魔数，用于识别协议
    private static final int MAGIC = 0x12345678;
    
    // 序列化器
    private final ProtocolSerializer serializer;
    
    public RpcCodec() {
        this(SerializerFactory.getDefaultSerializer());
    }
    
    public RpcCodec(ProtocolSerializer serializer) {
        this.serializer = serializer;
    }
    
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (msg instanceof RpcRequest || msg instanceof RpcResponse) {
            // 序列化对象
            byte[] data = serializer.serialize(msg);
            
            // 写入魔数
            out.writeInt(MAGIC);
            // 写入序列化类型
            out.writeByte(serializer.getType());
            // 写入数据长度
            out.writeInt(data.length);
            // 写入数据
            out.writeBytes(data);
            
            log.debug("Encoded message: type={}, length={}", msg.getClass().getSimpleName(), data.length);
        } else {
            log.warn("Unknown message type: {}", msg.getClass());
        }
    }
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 检查是否有足够的数据
        if (in.readableBytes() < 9) { // 魔数(4) + 序列化类型(1) + 数据长度(4)
            return;
        }
        
        // 标记读位置
        in.markReaderIndex();
        
        // 读取魔数
        int magic = in.readInt();
        if (magic != MAGIC) {
            log.error("Invalid magic number: {}", Integer.toHexString(magic));
            in.resetReaderIndex();
            return;
        }
        
        // 读取序列化类型
        byte serializerType = in.readByte();
        
        // 读取数据长度
        int dataLength = in.readInt();
        if (dataLength <= 0 || dataLength > 1024 * 1024) { // 限制最大1MB
            log.error("Invalid data length: {}", dataLength);
            in.resetReaderIndex();
            return;
        }
        
        // 检查是否有足够的数据
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }
        
        // 读取数据
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        
        try {
            // 获取对应的序列化器
            ProtocolSerializer deserializer = SerializerFactory.getSerializer(serializerType);
            
            // 根据消息类型进行反序列化
            Object obj;
            Boolean isClient = ctx.channel().attr(RpcRequest.ATTRIBUTE_KEY).get();
            if (Boolean.TRUE.equals(isClient)) {
                // 客户端接收响应
                obj = deserializer.deserialize(data, RpcResponse.class);
            } else {
                // 服务端接收请求
                obj = deserializer.deserialize(data, RpcRequest.class);
            }
            
            if (obj != null) {
                out.add(obj);
                log.debug("Decoded message: type={}, serializer={}", obj.getClass().getSimpleName(), 
                    deserializer.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.error("Failed to decode message", e);
        }
    }
} 