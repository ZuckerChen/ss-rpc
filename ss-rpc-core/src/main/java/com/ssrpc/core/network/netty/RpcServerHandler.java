package com.ssrpc.core.network.netty;

import com.ssrpc.core.config.NetworkConfig;
import com.ssrpc.core.invoker.ServiceInvoker;
import com.ssrpc.core.registry.ServiceInvokerRegistry;
import com.ssrpc.core.rpc.RpcRequest;
import com.ssrpc.core.rpc.RpcResponse;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * RPC服务端处理器.
 * 
 * 处理客户端发送的RPC请求，调用本地服务，返回响应结果
 * 支持心跳检测和异常处理
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@Slf4j
@ChannelHandler.Sharable
public class RpcServerHandler extends ChannelInboundHandlerAdapter {
    
    private final ServiceInvokerRegistry serviceRegistry;
    private final ExecutorService businessThreadPool;
    private final NetworkConfig config;
    
    public RpcServerHandler(ServiceInvokerRegistry serviceRegistry, 
                           ExecutorService businessThreadPool,
                           NetworkConfig config) {
        this.serviceRegistry = serviceRegistry;
        this.businessThreadPool = businessThreadPool;
        this.config = config;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Channel active: {}", ctx.channel());
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Channel inactive: {}", ctx.channel());
        super.channelInactive(ctx);
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof RpcRequest) {
            RpcRequest request = (RpcRequest) msg;
            log.debug("Received RPC request: {}", request);
            
            // 检查是否为心跳请求
            if (request.isHeartbeat()) {
                handleHeartbeat(ctx, request);
                return;
            }
            
            // 提交到业务线程池处理，避免阻塞I/O线程
            try {
                businessThreadPool.submit(() -> processRequest(ctx, request));
            } catch (RejectedExecutionException e) {
                log.error("Business thread pool is full, rejecting request: {}", request.getRequestId(), e);
                sendErrorResponse(ctx, request, "Server is too busy");
            }
            
        } else {
            log.warn("Received unknown message type: {}, closing channel", msg.getClass());
            ctx.close();
        }
    }
    
    /**
     * 处理心跳请求
     */
    private void handleHeartbeat(ChannelHandlerContext ctx, RpcRequest request) {
        RpcResponse response = RpcResponse.heartbeat(request.getRequestId());
        
        ctx.writeAndFlush(response).addListener(future -> {
            if (future.isSuccess()) {
                log.debug("Heartbeat response sent for request: {}", request.getRequestId());
            } else {
                log.warn("Failed to send heartbeat response for request: {}", 
                    request.getRequestId(), future.cause());
            }
        });
    }
    
    /**
     * 处理业务请求
     */
    private void processRequest(ChannelHandlerContext ctx, RpcRequest request) {
        long startTime = System.currentTimeMillis();
        RpcResponse response = new RpcResponse();
        response.setRequestId(request.getRequestId());
        
        try {
            // 参数验证
            if (request.getServiceName() == null || request.getMethodName() == null) {
                throw new IllegalArgumentException("Service name and method name cannot be null");
            }
            
            // 查找服务调用器
            ServiceInvoker invoker = serviceRegistry.getInvoker(
                request.getServiceName(), 
                request.getVersion()
            );
            
            if (invoker == null) {
                log.warn("Service not found: {}:{}", request.getServiceName(), request.getVersion());
                response.setStatusCode(RpcResponse.ResponseStatus.SERVICE_NOT_FOUND.getCode());
                response.setStatusMessage("Service not found: " + request.getServiceName() + ":" + request.getVersion());
            } else {
                // 调用本地服务
                Object result = invoker.invoke(
                    request.getMethodName(),
                    request.getParameterTypes(),
                    request.getParameters()
                );
                
                response.setResult(result);
                response.setStatusCode(RpcResponse.ResponseStatus.SUCCESS.getCode());
                response.setStatusMessage(RpcResponse.ResponseStatus.SUCCESS.getMessage());
                
                log.debug("Service invocation successful: {}.{}", 
                    request.getServiceName(), request.getMethodName());
            }
            
        } catch (Exception e) {
            log.error("Failed to process request: {}", request, e);
            response.setException(e);
            response.setStatusCode(RpcResponse.ResponseStatus.ERROR.getCode());
            response.setStatusMessage(e.getMessage());
        } finally {
            // 设置处理时间和服务端地址
            response.setProcessTime(startTime);
            response.setCreateTime(System.currentTimeMillis());
            
            // 发送响应
            sendResponse(ctx, response);
        }
    }
    
    /**
     * 发送错误响应
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, RpcRequest request, String errorMessage) {
        RpcResponse response = RpcResponse.fail(request.getRequestId(), errorMessage);
        response.setCreateTime(System.currentTimeMillis());
        sendResponse(ctx, response);
    }
    
    /**
     * 发送响应
     */
    private void sendResponse(ChannelHandlerContext ctx, RpcResponse response) {
        // 检查channel是否仍然活跃
        if (!ctx.channel().isActive()) {
            log.warn("Channel is not active, cannot send response: {}", response.getRequestId());
            return;
        }
        
        ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.debug("Response sent successfully: {}", response.getRequestId());
                } else {
                    log.error("Failed to send response: {}", response.getRequestId(), future.cause());
                    
                    // 如果发送失败，关闭连接
                    if (ctx.channel().isActive()) {
                        ctx.close();
                    }
                }
            }
        });
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleEvent = (IdleStateEvent) evt;
            
            if (idleEvent.state() == IdleState.READER_IDLE) {
                log.warn("Client connection idle detected, closing channel: {}", ctx.channel());
                ctx.close();
            }
        }
        
        super.userEventTriggered(ctx, evt);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception caught in server handler for channel: {}", ctx.channel(), cause);
        
        // 关闭连接
        if (ctx.channel().isActive()) {
            ctx.close();
        }
    }
    
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        // 处理背压情况
        if (!ctx.channel().isWritable()) {
            log.warn("Channel is not writable, may be experiencing backpressure: {}", ctx.channel());
        }
        
        super.channelWritabilityChanged(ctx);
    }
} 