package com.ssrpc.transport.netty;

import com.ssrpc.core.rpc.RpcRequest;
import com.ssrpc.core.rpc.RpcResponse;
import com.ssrpc.transport.heartbeat.HeartbeatManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Netty客户端业务处理Handler
 * 
 * 负责处理服务端发送的RPC响应，包括：
 * 1. RPC响应处理和Future完成
 * 2. 心跳响应处理
 * 3. 连接空闲检测和心跳发送
 * 4. 异常处理和连接管理
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class NettyClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
    
    private static final Logger log = LoggerFactory.getLogger(NettyClientHandler.class);
    
    private final NettyClient nettyClient;
    
    public NettyClientHandler(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Connected to server: {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Disconnected from server: {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        // 处理心跳响应
        if (isHeartbeatResponse(response)) {
            handleHeartbeatResponse(ctx, response);
            // 注意：心跳响应也需要传递给NettyClient处理，完成对应的Future
        }
        
        // 处理所有响应（包括心跳响应和业务响应）
        handleRpcResponse(response);
    }
    
    /**
     * 判断是否为心跳响应
     */
    private boolean isHeartbeatResponse(RpcResponse response) {
        // 简单判断：心跳响应的result为"pong"
        return "pong".equals(response.getResult());
    }
    
    /**
     * 处理心跳响应
     */
    private void handleHeartbeatResponse(ChannelHandlerContext ctx, RpcResponse response) {
        log.debug("Received heartbeat response from {}", ctx.channel().remoteAddress());
        // 心跳响应不需要特殊处理，只是确认连接正常
    }
    
    /**
     * 处理RPC业务响应
     */
    private void handleRpcResponse(RpcResponse response) {
        log.debug("Received RPC response: {}", response.getRequestId());
        
        // 将响应传递给NettyClient进行处理
        nettyClient.handleResponse(response);
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            
            if (event.state() == IdleState.WRITER_IDLE) {
                // 写空闲：发送心跳保持连接
                sendHeartbeat(ctx);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
    
    /**
     * 发送心跳请求
     */
    private void sendHeartbeat(ChannelHandlerContext ctx) {
        log.debug("Sending heartbeat to {}", ctx.channel().remoteAddress());
        
        // 创建心跳请求
        RpcRequest heartbeat = new RpcRequest().setAsHeartbeat();
        
        // 发送心跳
        ctx.writeAndFlush(heartbeat).addListener(future -> {
            if (future.isSuccess()) {
                log.debug("Heartbeat sent to {}", ctx.channel().remoteAddress());
            } else {
                log.warn("Failed to send heartbeat to {}", 
                    ctx.channel().remoteAddress(), future.cause());
            }
        });
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception caught in client handler for {}: {}", 
            ctx.channel().remoteAddress(), cause.getMessage(), cause);
        
        // 关闭连接
        ctx.close();
    }
} 