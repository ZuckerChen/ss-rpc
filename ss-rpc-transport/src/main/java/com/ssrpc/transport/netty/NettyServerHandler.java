package com.ssrpc.transport.netty;

import com.ssrpc.core.invoker.ServiceInvoker;
import com.ssrpc.registry.ServiceInvokerRegistry;
import com.ssrpc.core.rpc.RpcRequest;
import com.ssrpc.core.rpc.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
public class NettyServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
    
    private static final Logger log = LoggerFactory.getLogger(NettyServerHandler.class);
    
    private final ServiceInvokerRegistry serviceRegistry;
    private final ExecutorService businessThreadPool;
    
    public NettyServerHandler(ServiceInvokerRegistry serviceRegistry) {
        this(serviceRegistry, Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "rpc-server-handler");
            t.setDaemon(true);
            return t;
        }));
    }
    
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
        // 处理业务RPC请求
        handleRpcRequest(ctx, request);
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
        long startTime = System.currentTimeMillis();
        
        try {
            // 检查是否为心跳请求
            if (request.isHeartbeat()) {
                handleHeartbeatRequest(ctx, request);
                return;
            }
            
            // 获取服务调用器
            com.ssrpc.protocol.RpcInvoker invoker = serviceRegistry.getInvoker(request.getInterfaceName(), request.getVersion());
            
            if (invoker == null) {
                // 服务未找到
                RpcResponse response = RpcResponse.error(request.getRequestId(), 
                    "服务未找到: " + request.getInterfaceName());
                sendResponse(ctx, response);
                log.warn("Service not found: {} version: {}", request.getInterfaceName(), request.getVersion());
                return;
            }
            
            // 转换请求格式
            com.ssrpc.protocol.RpcRequest protocolRequest = convertRequest(request);
            
            // 调用服务方法
            com.ssrpc.protocol.RpcResponse protocolResponse = invoker.invoke(protocolRequest);
            
            // 转换响应格式
            RpcResponse response = convertResponse(protocolResponse);
            response.setProcessTime(System.currentTimeMillis() - startTime);
            
            sendResponse(ctx, response);
            log.debug("RPC request processed successfully: {}", request.getRequestId());
            
        } catch (Exception e) {
            log.error("Error invoking service method: {} {}", request.getInterfaceName(), request.getMethodName(), e);
            
            RpcResponse response = RpcResponse.error(request.getRequestId(), e);
            response.setProcessTime(System.currentTimeMillis() - startTime);
            
            sendResponse(ctx, response);
        }
    }
    
    /**
     * 处理心跳请求
     */
    private void handleHeartbeatRequest(ChannelHandlerContext ctx, RpcRequest request) {
        RpcResponse response = RpcResponse.success(request.getRequestId(), "pong");
        sendResponse(ctx, response);
        log.debug("Heartbeat response sent: {}", request.getRequestId());
    }
    
    /**
     * 转换请求格式
     */
    private com.ssrpc.protocol.RpcRequest convertRequest(RpcRequest request) {
        com.ssrpc.protocol.RpcRequest protocolRequest = new com.ssrpc.protocol.RpcRequest();
        protocolRequest.setRequestId(request.getRequestId());
        protocolRequest.setInterfaceName(request.getInterfaceName());
        protocolRequest.setMethodName(request.getMethodName());
        protocolRequest.setParameterTypes(request.getParameterTypes());
        protocolRequest.setParameters(request.getParameters());
        protocolRequest.setVersion(request.getVersion());
        protocolRequest.setTimeout(request.getTimeout());
        return protocolRequest;
    }
    
    /**
     * 转换响应格式
     */
    private RpcResponse convertResponse(com.ssrpc.protocol.RpcResponse protocolResponse) {
        if (protocolResponse.isSuccess()) {
            return RpcResponse.success(protocolResponse.getRequestId(), protocolResponse.getResult());
        } else {
            RpcResponse response = RpcResponse.error(protocolResponse.getRequestId(), protocolResponse.getErrorMessage());
            // 如果有堆栈跟踪信息，也设置到core的响应中
            if (protocolResponse.getStackTrace() != null) {
                response.setStackTrace(protocolResponse.getStackTrace());
            }
            return response;
        }
    }
    
    /**
     * 发送响应
     */
    private void sendResponse(ChannelHandlerContext ctx, RpcResponse response) {
        ctx.writeAndFlush(response).addListener(future -> {
            if (!future.isSuccess()) {
                log.error("Failed to send RPC response: {}", 
                    response.getRequestId(), future.cause());
            } else {
                log.debug("RPC response sent: {}", response.getRequestId());
            }
        });
    }
    
    /**
     * 发送错误响应
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, RpcRequest request, Exception e) {
        RpcResponse response = RpcResponse.error(request.getRequestId(), e);
        sendResponse(ctx, response);
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