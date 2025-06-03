package com.ssrpc.core.network.netty;

import com.ssrpc.core.config.NetworkConfig;
import com.ssrpc.core.rpc.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC客户端处理器.
 * 
 * 处理服务端返回的响应消息
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@Slf4j
public class RpcClientHandler extends ChannelInboundHandlerAdapter {
    
    private final NettyClient client;
    private final NetworkConfig config;
    
    public RpcClientHandler(NettyClient client, NetworkConfig config) {
        this.client = client;
        this.config = config;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Client channel active: {}", ctx.channel());
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Client channel inactive: {}", ctx.channel());
        super.channelInactive(ctx);
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof RpcResponse) {
            RpcResponse response = (RpcResponse) msg;
            log.debug("Received RPC response: {}", response);
            
            // 处理响应
            client.handleResponse(response);
            
        } else {
            log.warn("Received unknown message type: {}", msg.getClass());
        }
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleEvent = (IdleStateEvent) evt;
            
            if (idleEvent.state() == IdleState.WRITER_IDLE) {
                // 写空闲时发送心跳
                log.debug("Writer idle detected, should send heartbeat");
                // 心跳发送逻辑由客户端的定时任务处理
            }
        }
        
        super.userEventTriggered(ctx, evt);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception caught in client handler for channel: {}", ctx.channel(), cause);
        
        // 关闭连接
        if (ctx.channel().isActive()) {
            ctx.close();
        }
    }
} 