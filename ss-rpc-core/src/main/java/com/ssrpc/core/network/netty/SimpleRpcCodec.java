package com.ssrpc.core.network.netty;

import com.ssrpc.core.rpc.RpcRequest;
import com.ssrpc.core.rpc.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.List;

/**
 * 简单的RPC编解码器.
 * 
 * 基于Java原生序列化的临时实现，用于快速验证功能
 * 生产环境建议使用更高效的序列化方案
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@Slf4j
public class SimpleRpcCodec extends ByteToMessageCodec<Object> {
    
    // 魔数，用于识别协议
    private static final int MAGIC = 0x12345678;
    
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (msg instanceof RpcRequest || msg instanceof RpcResponse) {
            // 序列化对象
            byte[] data = serialize(msg);
            
            // 写入魔数
            out.writeInt(MAGIC);
            // 写入数据长度
            out.writeInt(data.length);
            // 写入数据
            out.writeBytes(data);
            
            log.debug("Encoded message: {} bytes", data.length);
        } else {
            log.warn("Unknown message type: {}", msg.getClass());
        }
    }
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 检查是否有足够的数据
        if (in.readableBytes() < 8) { // 至少需要8字节（魔数4字节 + 长度4字节）
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
        
        // 反序列化对象
        Object obj = deserialize(data);
        if (obj != null) {
            out.add(obj);
            log.debug("Decoded message: {}", obj.getClass().getSimpleName());
        }
    }
    
    /**
     * 序列化对象
     */
    private byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            return bos.toByteArray();
        }
    }
    
    /**
     * 反序列化对象
     */
    private Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return ois.readObject();
        }
    }
} 