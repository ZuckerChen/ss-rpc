package com.ssrpc.core.network.netty;

import com.ssrpc.core.invoker.ServiceInvoker;
import com.ssrpc.core.registry.ServiceInvokerRegistry;
import com.ssrpc.core.rpc.RpcRequest;
import com.ssrpc.core.rpc.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;

/**
 * Netty服务端业务处理Handler
 * 
 * 负责处理客户端发送的RPC请求，包括：
 * 1. 心跳请求处理
 * 2. 业务RPC请求分发到业务线程池
 * 3. 连接空闲检测和处理
 * 4. 异常处理和连接管理
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@Slf4j
public class NettyServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
    
    private final ServiceInvokerRegistry serviceRegistry;
    private final ExecutorService businessThreadPool;
    
    public NettyServerHandler(ServiceInvokerRegistry serviceRegistry, 
                             ExecutorService businessThreadPool) {
        this.serviceRegistry = serviceRegistry;
        this.businessThreadPool = businessThreadPool;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Client connected: {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Client disconnected: {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {
        // 处理心跳请求
        if (request.isHeartbeat()) {
            handleHeartbeat(ctx, request);
            return;
        }
        
        // 处理业务RPC请求
        handleRpcRequest(ctx, request);
    }
    
    /**
     * 处理心跳请求
     */
    private void handleHeartbeat(ChannelHandlerContext ctx, RpcRequest request) {
        log.debug("Received heartbeat from {}", ctx.channel().remoteAddress());
        
        // 创建心跳响应 - 使用正确的心跳响应静态方法
        RpcResponse response = RpcResponse.heartbeat(request.getRequestId());
        
        // 发送心跳响应
        ctx.writeAndFlush(response).addListener(future -> {
            if (!future.isSuccess()) {
                log.warn("Failed to send heartbeat response to {}", 
                    ctx.channel().remoteAddress(), future.cause());
            }
        });
    }
    
    /**
     * 处理RPC业务请求
     */
    private void handleRpcRequest(ChannelHandlerContext ctx, RpcRequest request) {
        log.debug("Received RPC request: {} from {}", 
            request.getRequestId(), ctx.channel().remoteAddress());
        
        // 异步处理业务请求，避免阻塞I/O线程
        businessThreadPool.execute(() -> {
            try {
                processRpcRequest(ctx, request);
            } catch (Exception e) {
                log.error("Error processing RPC request: {}", request.getRequestId(), e);
                sendErrorResponse(ctx, request, e);
            }
        });
    }
    
    /**
     * 处理RPC请求的具体业务逻辑
     */
    private void processRpcRequest(ChannelHandlerContext ctx, RpcRequest request) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(request.getRequestId());
        
        try {
            // 获取服务调用器
            ServiceInvoker invoker = serviceRegistry.getInvoker(request.getServiceName(), request.getVersion());
            
            if (invoker == null) {
                // 服务未找到
                response.setSuccess(false);
                response.setStatusCode(RpcResponse.ResponseStatus.SERVICE_NOT_FOUND.getCode());
                response.setStatusMessage("服务未找到: " + request.getServiceKey());
                log.warn("Service not found: {}", request.getServiceKey());
            } else {
                // 调用服务方法
                Object result = invoker.invoke(
                    request.getMethodName(), 
                    request.getParameterTypes(), 
                    request.getParameters()
                );
                
                response.setSuccess(true);
                response.setResult(result);
                log.debug("RPC request processed successfully: {}", request.getRequestId());
            }
            
        } catch (Exception e) {
            log.error("Error invoking service method: {} {}", request.getServiceKey(), request.getMethodName(), e);
            response.setSuccess(false);
            response.setStatusCode(RpcResponse.ResponseStatus.ERROR.getCode());
            response.setStatusMessage("服务调用异常: " + e.getMessage());
        }
        
        // 发送响应
        ctx.writeAndFlush(response).addListener(future -> {
            if (!future.isSuccess()) {
                log.error("Failed to send RPC response: {}", 
                    request.getRequestId(), future.cause());
            } else {
                log.debug("RPC response sent: {}", request.getRequestId());
            }
        });
    }
    
    /**
     * 发送错误响应
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, RpcRequest request, Exception e) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(request.getRequestId());
        response.setSuccess(false);
        response.setErrorMessage("Server error: " + e.getMessage());
        
        ctx.writeAndFlush(response).addListener(future -> {
            if (!future.isSuccess()) {
                log.error("Failed to send error response: {}", 
                    request.getRequestId(), future.cause());
            }
        });
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            
            if (event.state() == IdleState.READER_IDLE) {
                // 读空闲：客户端长时间未发送数据，可能已断开
                log.warn("Client read idle timeout, closing connection: {}", 
                    ctx.channel().remoteAddress());
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception caught in server handler for {}: {}", 
            ctx.channel().remoteAddress(), cause.getMessage(), cause);
        
        // 关闭连接
        ctx.close();
    }
} 