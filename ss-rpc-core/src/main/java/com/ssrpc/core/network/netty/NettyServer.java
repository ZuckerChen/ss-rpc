package com.ssrpc.core.network.netty;

import com.ssrpc.core.config.NetworkConfig;
import com.ssrpc.core.exception.NetworkException;
import com.ssrpc.core.network.RpcServer;
import com.ssrpc.core.registry.ServiceInvokerRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 基于Netty的RPC服务端实现.
 * 
 * 使用Reactor模式提供高性能的网络通信服务
 * 采用主从Reactor架构，支持多种配置选项
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@Slf4j
public class NettyServer implements RpcServer {
    
    private final NetworkConfig config;
    private final ServiceInvokerRegistry serviceRegistry;
    
    // Netty组件
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final ExecutorService businessThreadPool;
    
    // 服务端状态
    private Channel serverChannel;
    private volatile boolean started = false;
    private int actualPort = -1;
    
    public NettyServer(NetworkConfig config, ServiceInvokerRegistry serviceRegistry) {
        this.config = config;
        this.serviceRegistry = serviceRegistry;
        
        // 验证配置
        config.validate();
        
        // 初始化线程组
        this.bossGroup = new NioEventLoopGroup(
            config.getBossThreads(),
            new DefaultThreadFactory("ss-rpc-boss", true)
        );
        
        this.workerGroup = new NioEventLoopGroup(
            config.getWorkerThreads(),
            new DefaultThreadFactory("ss-rpc-worker", true)
        );
        
        // 初始化业务线程池
        this.businessThreadPool = createBusinessThreadPool();
        
        log.info("NettyServer initialized with config: {}", config);
    }
    
    @Override
    public void start(int port) throws NetworkException {
        if (started) {
            log.warn("NettyServer is already started on port {}", actualPort);
            return;
        }
        
        synchronized (this) {
            if (started) {
                return;
            }
            
            try {
                doStart(port);
                started = true;
                actualPort = port;
                log.info("SS-RPC NettyServer started successfully on port {}", port);
                
            } catch (Exception e) {
                log.error("Failed to start NettyServer on port {}", port, e);
                // 启动失败时清理资源
                cleanup();
                throw new NetworkException("Failed to start server on port " + port, e);
            }
        }
    }
    
    private void doStart(int port) throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, config.getServerBacklog())
                .option(ChannelOption.SO_REUSEADDR, config.isServerReuseAddress())
                .childOption(ChannelOption.SO_KEEPALIVE, config.isKeepAlive())
                .childOption(ChannelOption.TCP_NODELAY, config.isTcpNoDelay())
                .childOption(ChannelOption.SO_RCVBUF, config.getReceiveBufferSize())
                .childOption(ChannelOption.SO_SNDBUF, config.getSendBufferSize());
        
        // 配置内存分配器
        if (config.isUsePooledAllocator()) {
            bootstrap.childOption(ChannelOption.ALLOCATOR, io.netty.buffer.PooledByteBufAllocator.DEFAULT);
        }
        
        // 添加日志处理器（仅在DEBUG模式下）
        if (log.isDebugEnabled()) {
            bootstrap.handler(new LoggingHandler(LogLevel.DEBUG));
        }
        
        // 设置子channel初始化器
        bootstrap.childHandler(new ServerChannelInitializer());
        
        // 绑定端口并等待完成
        ChannelFuture future = bootstrap.bind(port).sync();
        
        if (future.isSuccess()) {
            serverChannel = future.channel();
            log.info("NettyServer bound to port {} successfully", port);
        } else {
            throw new RuntimeException("Failed to bind to port " + port, future.cause());
        }
    }
    
    @Override
    public void shutdown() {
        if (!started) {
            log.warn("NettyServer is not started, shutdown ignored");
            return;
        }
        
        synchronized (this) {
            if (!started) {
                return;
            }
            
            started = false;
            actualPort = -1;  // 重置端口号
            log.info("Shutting down NettyServer...");
            
            try {
                // 关闭服务端channel
                if (serverChannel != null) {
                    serverChannel.close().sync();
                    log.info("Server channel closed");
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while closing server channel", e);
                Thread.currentThread().interrupt();
            } finally {
                cleanup();
                log.info("NettyServer shutdown completed");
            }
        }
    }
    
    private void cleanup() {
        try {
            // 优雅关闭线程池
            shutdownThreadPool(businessThreadPool, "business");
            
            // 优雅关闭Netty线程组
            bossGroup.shutdownGracefully(2, 10, TimeUnit.SECONDS);
            workerGroup.shutdownGracefully(2, 10, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            log.error("Error during cleanup", e);
        }
    }
    
    private void shutdownThreadPool(ExecutorService executor, String name) {
        if (executor != null && !executor.isShutdown()) {
            try {
                executor.shutdown();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("{} thread pool did not terminate gracefully, forcing shutdown", name);
                    executor.shutdownNow();
                }
                log.info("{} thread pool shutdown completed", name);
            } catch (InterruptedException e) {
                log.warn("Interrupted while shutting down {} thread pool", name, e);
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    @Override
    public boolean isStarted() {
        return started;
    }
    
    @Override
    public int getPort() {
        return actualPort;
    }
    
    /**
     * 创建业务线程池
     * 
     * 根据配置创建不同类型的业务线程池，用于处理RPC业务逻辑，
     * 避免在Netty的I/O线程中执行重计算任务。
     * 
     * 线程池配置说明：
     * - FIXED: 固定大小线程池，适合负载稳定的场景
     * - CACHED: 缓存线程池，适合负载波动的场景
     * - SCHEDULED: 定时调度线程池，支持延迟任务
     * - CUSTOM: 自定义线程池，需要额外配置
     * 
     * @return 配置的业务线程池
     */
    private ExecutorService createBusinessThreadPool() {
        String threadNamePrefix = "ss-rpc-business";
        
        switch (config.getBusinessThreadPoolType()) {
            case FIXED:
                log.info("Creating fixed thread pool with {} threads", config.getBusinessThreads());
                return new ThreadPoolExecutor(
                    config.getBusinessThreads(),
                    config.getBusinessThreads(),
                    0L,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    new DefaultThreadFactory(threadNamePrefix, true)
                );
                
            case CACHED:
                log.info("Creating cached thread pool");
                return Executors.newCachedThreadPool(
                    new DefaultThreadFactory(threadNamePrefix, true)
                );
                
            case SCHEDULED:
                log.info("Creating scheduled thread pool with {} threads", config.getBusinessThreads());
                return Executors.newScheduledThreadPool(
                    config.getBusinessThreads(),
                    new DefaultThreadFactory(threadNamePrefix, true)
                );
                
            case CUSTOM:
                log.warn("Custom thread pool type specified but not implemented, falling back to fixed");
                return new ThreadPoolExecutor(
                    config.getBusinessThreads(),
                    config.getBusinessThreads(),
                    0L,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    new DefaultThreadFactory(threadNamePrefix, true)
                );
                
            default:
                throw new IllegalArgumentException("Unsupported thread pool type: " + 
                    config.getBusinessThreadPoolType());
        }
    }
    
    /**
     * 服务端Channel初始化器
     * 
     * 配置每个新连接的Channel Pipeline，设置必要的Handler来处理：
     * 1. 空闲检测 - 基于配置的心跳参数检测连接健康状态
     * 2. 编解码 - 处理RPC消息的序列化和反序列化  
     * 3. 业务处理 - 将请求分发到业务线程池进行处理
     * 
     * Pipeline设计原则：
     * - 入站：IdleStateHandler -> Decoder -> BusinessHandler
     * - 出站：Encoder -> 网络传输
     * - 异常：统一异常处理确保连接稳定性
     */
    private class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {
        
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            
            // 1. 空闲状态检测Handler - 用于心跳机制
            // readerIdleTime: 读空闲时间，超时未收到数据则触发READER_IDLE事件
            // writerIdleTime: 写空闲时间，超时未发送数据则触发WRITER_IDLE事件  
            // allIdleTime: 读写空闲时间，超时未读写则触发ALL_IDLE事件
            if (config.isHeartbeatEnabled()) {
                pipeline.addLast("idle-state-handler", new IdleStateHandler(
                    config.getReaderIdleTime(),   // 读空闲超时，用于检测客户端异常
                    config.getWriterIdleTime(),   // 写空闲超时，一般服务端不主动发送
                    config.getAllIdleTime(),      // 读写空闲超时
                    TimeUnit.SECONDS
                ));
                log.debug("Added IdleStateHandler with readerIdle={}s, writerIdle={}s, allIdle={}s", 
                    config.getReaderIdleTime(), config.getWriterIdleTime(), config.getAllIdleTime());
            }
            
            // 2. 添加编解码器
            pipeline.addLast("rpc-codec", new SimpleRpcCodec());
            
            // 3. 业务处理Handler - 处理RPC请求并分发到业务线程池
            pipeline.addLast("rpc-handler", new NettyServerHandler(serviceRegistry, businessThreadPool));
            
            log.debug("Channel pipeline initialized for {}", ch.remoteAddress());
        }
    }
    
    /**
     * 获取业务线程池统计信息（用于监控）
     */
    public String getThreadPoolStats() {
        if (businessThreadPool instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) businessThreadPool;
            return String.format(
                "ThreadPool[active=%d, pool=%d, queue=%d, completed=%d]",
                executor.getActiveCount(),
                executor.getPoolSize(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount()
            );
        }
        return "ThreadPool stats not available";
    }
} 