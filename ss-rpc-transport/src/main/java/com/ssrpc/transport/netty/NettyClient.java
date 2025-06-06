package com.ssrpc.transport.netty;

import com.ssrpc.transport.config.NetworkConfig;
import com.ssrpc.transport.exception.TransportException;
import com.ssrpc.transport.api.RpcClient;
import com.ssrpc.core.rpc.RpcRequest;
import com.ssrpc.core.rpc.RpcResponse;
import com.ssrpc.core.util.CompletableFutureUtils;
import com.ssrpc.transport.codec.SimpleRpcCodec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * 基于Netty的RPC客户端实现.
 * 
 * 提供高性能的网络通信能力，支持连接池和心跳检测
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class NettyClient implements RpcClient {
    
    private static final Logger log = LoggerFactory.getLogger(NettyClient.class);
    
    private final NetworkConfig config;
    
    // Netty组件
    private final EventLoopGroup workerGroup;
    private final Bootstrap bootstrap;
    
    // 连接管理
    private final ConcurrentHashMap<String, Channel> channelPool;
    private final ConcurrentHashMap<String, CompletableFuture<RpcResponse>> pendingRequests;
    
    // 心跳管理
    private final ScheduledExecutorService heartbeatExecutor;
    
    // 客户端状态
    private volatile boolean started = false;
    
    public NettyClient(NetworkConfig config) {
        this.config = config;
        
        // 验证配置
        config.validate();
        
        // 初始化Netty组件
        this.workerGroup = new NioEventLoopGroup(
            config.getWorkerThreads(),
            new DefaultThreadFactory("ss-rpc-client", true)
        );
        
        this.bootstrap = new Bootstrap();
        configureBootstrap();
        
        // 初始化连接和请求管理
        this.channelPool = new ConcurrentHashMap<>();
        this.pendingRequests = new ConcurrentHashMap<>();
        
        // 初始化心跳执行器
        this.heartbeatExecutor = Executors.newScheduledThreadPool(2,
            new DefaultThreadFactory("ss-rpc-heartbeat", true));
        
        log.info("NettyClient initialized with config: {}", config);
    }
    
    /**
     * 配置Netty Bootstrap
     * 
     * 设置客户端连接的各项Netty参数，这些参数直接影响连接性能：
     * 
     * 连接优化参数：
     * - SO_KEEPALIVE: TCP层心跳保活，检测死连接
     * - TCP_NODELAY: 禁用Nagle算法，降低小数据包延迟
     * - CONNECT_TIMEOUT_MILLIS: 连接建立超时时间
     * - SO_RCVBUF/SO_SNDBUF: 接收/发送缓冲区大小，影响吞吐量
     * 
     * 内存优化：
     * - PooledByteBufAllocator: 使用内存池减少GC压力
     * - DirectBuffer: 使用堆外内存提升I/O性能
     */
    private void configureBootstrap() {
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                // 启用TCP层面的连接保活机制
                .option(ChannelOption.SO_KEEPALIVE, config.isKeepAlive())
                // 禁用Nagle算法，立即发送数据包，适合低延迟场景
                .option(ChannelOption.TCP_NODELAY, config.isTcpNoDelay())
                // 连接建立超时时间，防止连接hang住
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeout())
                // 接收缓冲区大小，影响接收性能
                .option(ChannelOption.SO_RCVBUF, config.getReceiveBufferSize())
                // 发送缓冲区大小，影响发送性能
                .option(ChannelOption.SO_SNDBUF, config.getSendBufferSize());
        
        // 配置内存分配器 - 使用池化内存分配器提升性能
        if (config.isUsePooledAllocator()) {
            bootstrap.option(ChannelOption.ALLOCATOR, io.netty.buffer.PooledByteBufAllocator.DEFAULT);
            log.debug("Enabled pooled ByteBuf allocator for better memory management");
        }
        
        // 配置直接内存使用
        if (config.isUseDirectBuffer()) {
            log.debug("Direct buffer usage enabled for improved I/O performance");
        }
    }
    
    @Override
    public void start() throws TransportException {
        if (started) {
            log.warn("NettyClient is already started");
            return;
        }
        
        synchronized (this) {
            if (started) {
                return;
            }
            
            try {
                // 启动心跳检测
                if (config.isHeartbeatEnabled()) {
                    startHeartbeat();
                }
                
                // 启动请求超时检查
                startTimeoutChecker();
                
                started = true;
                log.info("SS-RPC NettyClient started successfully");
                
            } catch (Exception e) {
                log.error("Failed to start NettyClient", e);
                cleanup();
                throw new TransportException("Failed to start client", e);
            }
        }
    }
    
    @Override
    public void shutdown() {
        if (!started) {
            log.warn("NettyClient is not started, shutdown ignored");
            return;
        }
        
        synchronized (this) {
            if (!started) {
                return;
            }
            
            started = false;
            log.info("Shutting down NettyClient...");
            
            cleanup();
            log.info("NettyClient shutdown completed");
        }
    }
    
    private void cleanup() {
        try {
            // 关闭所有连接
            for (Channel channel : channelPool.values()) {
                if (channel != null && channel.isActive()) {
                    channel.close();
                }
            }
            channelPool.clear();
            
            // 取消所有待处理的请求
            for (CompletableFuture<RpcResponse> future : pendingRequests.values()) {
                future.completeExceptionally(new TransportException("Client is shutting down"));
            }
            pendingRequests.clear();
            
            // 关闭心跳执行器
            if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) {
                heartbeatExecutor.shutdown();
                try {
                    if (!heartbeatExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                        heartbeatExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    heartbeatExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            // 关闭Netty线程组
            workerGroup.shutdownGracefully(2, 10, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            log.error("Error during cleanup", e);
        }
    }
    
    @Override
    public CompletableFuture<RpcResponse> sendRequest(String address, RpcRequest request) {
        if (!started) {
            return CompletableFutureUtils.failedFuture(new TransportException("Client not started"));
        }
        
        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        
        try {
            // 获取或创建连接
            Channel channel = getOrCreateChannel(address);
            
            // 注册待响应请求
            pendingRequests.put(request.getRequestId(), future);
            
            // 设置请求超时
            setupRequestTimeout(request.getRequestId(), future);
            
            // 发送请求
            channel.writeAndFlush(request).addListener((ChannelFutureListener) channelFuture -> {
                if (!channelFuture.isSuccess()) {
                    // 发送失败，移除待处理请求
                    pendingRequests.remove(request.getRequestId());
                    future.completeExceptionally(new TransportException(
                        "Failed to send request", channelFuture.cause()));
                    log.error("Failed to send request: {}", request.getRequestId(), channelFuture.cause());
                } else {
                    log.debug("Request sent successfully: {}", request.getRequestId());
                }
            });
            
        } catch (Exception e) {
            pendingRequests.remove(request.getRequestId());
            future.completeExceptionally(e);
            log.error("Error sending request: {}", request.getRequestId(), e);
        }
        
        return future;
    }
    
    @Override
    public RpcResponse sendRequestSync(String address, RpcRequest request) throws TransportException {
        try {
            return sendRequest(address, request).get(config.getRequestTimeout(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new TransportException("Request timeout", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TransportException) {
                throw (TransportException) cause;
            } else {
                throw new TransportException("Request execution error", cause);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransportException("Request interrupted", e);
        }
    }
    
    @Override
    public boolean isConnected(String address) {
        Channel channel = channelPool.get(address);
        return channel != null && channel.isActive();
    }
    
    @Override
    public boolean isStarted() {
        return started;
    }
    
    /**
     * 获取或创建到指定地址的连接
     */
    private Channel getOrCreateChannel(String address) throws TransportException {
        Channel channel = channelPool.get(address);
        
        if (channel != null && channel.isActive()) {
            return channel;
        }
        
        // 需要创建新连接
        synchronized (channelPool) {
            // 双重检查
            channel = channelPool.get(address);
            if (channel != null && channel.isActive()) {
                return channel;
            }
            
            // 创建新连接
            channel = createChannel(address);
            channelPool.put(address, channel);
            
            return channel;
        }
    }
    
    /**
     * 创建到指定地址的连接
     */
    private Channel createChannel(String address) throws TransportException {
        String[] parts = address.split(":");
        if (parts.length != 2) {
            throw new TransportException("Invalid address format: " + address);
        }
        
        String host = parts[0];
        int port;
        try {
            port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new TransportException("Invalid port in address: " + address, e);
        }
        
        // 设置Channel初始化器
        bootstrap.handler(new ClientChannelInitializer());
        
        try {
            ChannelFuture future = bootstrap.connect(host, port).sync();
            
            if (future.isSuccess()) {
                Channel channel = future.channel();
                log.info("Connected to server: {}", address);
                return channel;
            } else {
                throw new TransportException("Failed to connect to " + address, future.cause());
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransportException("Connection interrupted: " + address, e);
        }
    }
    
    /**
     * 设置请求超时
     */
    private void setupRequestTimeout(String requestId, CompletableFuture<RpcResponse> future) {
        heartbeatExecutor.schedule(() -> {
            CompletableFuture<RpcResponse> removed = pendingRequests.remove(requestId);
            if (removed != null && !removed.isDone()) {
                removed.completeExceptionally(new TimeoutException("Request timeout: " + requestId));
                log.warn("Request timeout: {}", requestId);
            }
        }, config.getRequestTimeout(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * 启动心跳检测
     */
    private void startHeartbeat() {
        // 心跳现在由NettyClientHandler处理，这里不需要额外的定时任务
        log.debug("Heartbeat mechanism enabled in NettyClientHandler");
    }
    
    /**
     * 启动超时检查器
     */
    private void startTimeoutChecker() {
        heartbeatExecutor.scheduleWithFixedDelay(() -> {
            long currentTime = System.currentTimeMillis();
            
            pendingRequests.entrySet().removeIf(entry -> {
                String requestId = entry.getKey();
                CompletableFuture<RpcResponse> future = entry.getValue();
                
                // 检查是否超时（这里可以根据请求的创建时间来判断）
                // 简化处理：如果future已经完成，则移除
                return future.isDone();
            });
            
        }, 10, 10, TimeUnit.SECONDS);
    }
    
    /**
     * 处理响应（由ClientHandler调用）
     * 
     * 当收到服务端响应时，根据requestId找到对应的CompletableFuture并完成它。
     * 这是异步RPC调用的关键环节，将网络响应转换为业务层的Future结果。
     * 
     * @param response RPC响应对象
     */
    public void handleResponse(RpcResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<RpcResponse> future = pendingRequests.remove(requestId);
        
        if (future != null) {
            future.complete(response);
            log.debug("Response handled: {}", requestId);
        } else {
            log.warn("No pending request found for response: {}", requestId);
        }
    }
    
    /**
     * 客户端Channel初始化器
     * 
     * 配置客户端连接的Channel Pipeline，设置必要的Handler来处理：
     * 1. 心跳检测 - 监控连接状态，主动发送心跳保持连接活跃
     * 2. 编解码 - 处理RPC消息的序列化和反序列化
     * 3. 响应处理 - 接收服务端响应并完成相应的Future
     * 
     * 客户端Pipeline特点：
     * - 写空闲检测：定期发送心跳保持连接
     * - 异步响应处理：基于requestId匹配请求和响应
     * - 异常处理：连接断开时清理资源和失败请求
     */
    private class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {
        
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            
            // 1. 空闲状态检测Handler - 客户端心跳机制
            // 客户端主要关注写空闲，即定期发送心跳到服务端
            // writerIdleTime: 写空闲时间，超时未发送数据则触发WRITER_IDLE事件
            if (config.isHeartbeatEnabled()) {
                pipeline.addLast("idle-state-handler", new IdleStateHandler(
                    0,                               // 读空闲时间，客户端不检测读空闲
                    config.getHeartbeatInterval(),   // 写空闲时间，触发心跳发送
                    0,                               // 读写空闲时间，不使用
                    TimeUnit.SECONDS
                ));
                log.debug("Added IdleStateHandler with writerIdle={}s for heartbeat", 
                    config.getHeartbeatInterval());
            }
            
            // 2. 添加编解码器
            pipeline.addLast("rpc-codec", new SimpleRpcCodec());
            
            // 3. 客户端业务处理Handler - 处理响应和连接事件
            pipeline.addLast("rpc-handler", new NettyClientHandler(NettyClient.this));
            
            log.debug("Client channel pipeline initialized for {}", ch.remoteAddress());
        }
    }
} 