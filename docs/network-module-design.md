# SS-RPC 网络模块设计

## 🌐 网络模块概述

网络模块是SS-RPC框架的传输层核心，负责客户端和服务端之间的网络通信。基于Netty实现高性能的异步非阻塞I/O。

## 🏗️ 网络架构设计

### 1. Reactor网络模型

采用Netty的主从Reactor模式：

```
┌─────────────────────────────────────────────────┐
│                Main Reactor                     │
│          (Boss EventLoopGroup)                  │
│                                                 │
│  ┌─────────────┐  ┌─────────────┐              │
│  │  Acceptor   │  │  Acceptor   │  ...         │
│  │  Thread 1   │  │  Thread 2   │              │
│  └─────────────┘  └─────────────┘              │
└─────────────────┬───────────────────────────────┘
                  │ 接受新连接
                  ▼
┌─────────────────────────────────────────────────┐
│               Sub Reactor                       │
│         (Worker EventLoopGroup)                 │
│                                                 │
│  ┌─────────────┐  ┌─────────────┐              │
│  │   I/O       │  │    I/O      │  ...         │
│  │ Handler 1   │  │ Handler 2   │              │
│  └─────────────┘  └─────────────┘              │
└─────────────────┬───────────────────────────────┘
                  │ 处理I/O事件
                  ▼
┌─────────────────────────────────────────────────┐
│            Business Thread Pool                 │
│                                                 │
│  ┌─────────────┐  ┌─────────────┐              │
│  │  Business   │  │  Business   │  ...         │
│  │  Thread 1   │  │  Thread 2   │              │
│  └─────────────┘  └─────────────┘              │
└─────────────────────────────────────────────────┘
```

**优势**：
- 主Reactor专门处理连接建立
- 从Reactor专门处理I/O读写
- 业务线程池处理具体业务逻辑
- 彻底分离I/O操作和业务处理

### 2. 线程模型配置

```java
public class NetworkConfig {
    // Boss线程数：通常设置为1，负责accept新连接
    private int bossThreads = 1;
    
    // Worker线程数：建议设置为CPU核数的2倍
    private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;
    
    // 业务线程池大小：根据业务复杂度调整
    private int businessThreads = 200;
    
    // 线程池类型配置
    private ThreadPoolType businessThreadPoolType = ThreadPoolType.CACHED;
}

public enum ThreadPoolType {
    FIXED,      // 固定大小线程池
    CACHED,     // 缓存线程池
    SCHEDULED,  // 调度线程池
    CUSTOM      // 自定义线程池
}
```

## 🔧 核心组件设计

### 1. 服务端组件

#### NettyServer - 服务端启动器

```java
/**
 * Netty服务端实现.
 * 
 * 负责启动RPC服务端，监听指定端口，处理客户端连接
 */
public class NettyServer implements RpcServer {
    
    private final NetworkConfig config;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final ExecutorService businessThreadPool;
    private final ServiceInvokerRegistry serviceRegistry;
    
    private Channel serverChannel;
    private volatile boolean started = false;
    
    public NettyServer(NetworkConfig config, ServiceInvokerRegistry serviceRegistry) {
        this.config = config;
        this.serviceRegistry = serviceRegistry;
        this.bossGroup = new NioEventLoopGroup(config.getBossThreads(), 
            new DefaultThreadFactory("ss-rpc-boss"));
        this.workerGroup = new NioEventLoopGroup(config.getWorkerThreads(), 
            new DefaultThreadFactory("ss-rpc-worker"));
        this.businessThreadPool = createBusinessThreadPool();
    }
    
    @Override
    public void start(int port) throws NetworkException {
        if (started) {
            throw new NetworkException("Server already started");
        }
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // 服务端配置
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    // 客户端连接配置
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_RCVBUF, 65536)
                    .childOption(ChannelOption.SO_SNDBUF, 65536)
                    .childHandler(new ServerChannelInitializer());
            
            ChannelFuture future = bootstrap.bind(port).sync();
            if (future.isSuccess()) {
                serverChannel = future.channel();
                started = true;
                log.info("SS-RPC server started on port {}", port);
            } else {
                throw new NetworkException("Failed to bind port " + port);
            }
            
        } catch (Exception e) {
            shutdown();
            throw new NetworkException("Failed to start server", e);
        }
    }
    
    @Override
    public void shutdown() {
        if (!started) {
            return;
        }
        
        try {
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while closing server channel", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            businessThreadPool.shutdown();
            started = false;
            log.info("SS-RPC server shutdown completed");
        }
    }
    
    /**
     * 服务端Channel初始化器
     */
    private class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            
            // 协议编解码器
            pipeline.addLast("decoder", new RpcProtocolDecoder());
            pipeline.addLast("encoder", new RpcProtocolEncoder());
            
            // 心跳检测：60秒没有读操作则触发心跳检测
            pipeline.addLast("idleState", new IdleStateHandler(60, 0, 0));
            
            // 业务处理器
            pipeline.addLast("handler", new RpcServerHandler(serviceRegistry, businessThreadPool));
        }
    }
    
    private ExecutorService createBusinessThreadPool() {
        switch (config.getBusinessThreadPoolType()) {
            case FIXED:
                return Executors.newFixedThreadPool(config.getBusinessThreads(),
                    new DefaultThreadFactory("ss-rpc-business"));
            case CACHED:
                return Executors.newCachedThreadPool(
                    new DefaultThreadFactory("ss-rpc-business"));
            case SCHEDULED:
                return Executors.newScheduledThreadPool(config.getBusinessThreads(),
                    new DefaultThreadFactory("ss-rpc-business"));
            default:
                return new ThreadPoolExecutor(
                    config.getBusinessThreads() / 4,
                    config.getBusinessThreads(),
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(1024),
                    new DefaultThreadFactory("ss-rpc-business"),
                    new ThreadPoolExecutor.CallerRunsPolicy());
        }
    }
}
```

#### RpcServerHandler - 服务端业务处理器

```java
/**
 * RPC服务端业务处理器.
 * 
 * 处理客户端请求，调用本地服务，返回响应结果
 */
@ChannelHandler.Sharable
public class RpcServerHandler extends ChannelInboundHandlerAdapter {
    
    private final ServiceInvokerRegistry serviceRegistry;
    private final ExecutorService businessThreadPool;
    
    public RpcServerHandler(ServiceInvokerRegistry serviceRegistry, 
                           ExecutorService businessThreadPool) {
        this.serviceRegistry = serviceRegistry;
        this.businessThreadPool = businessThreadPool;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof RpcRequest) {
            RpcRequest request = (RpcRequest) msg;
            
            // 提交到业务线程池处理，避免阻塞I/O线程
            businessThreadPool.submit(() -> processRequest(ctx, request));
        } else if (msg instanceof HeartbeatMessage) {
            // 处理心跳消息
            handleHeartbeat(ctx, (HeartbeatMessage) msg);
        } else {
            log.warn("Received unknown message type: {}", msg.getClass());
            ctx.close();
        }
    }
    
    private void processRequest(ChannelHandlerContext ctx, RpcRequest request) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(request.getRequestId());
        
        try {
            // 查找服务调用器
            ServiceInvoker invoker = serviceRegistry.getInvoker(
                request.getServiceName(), request.getVersion());
            
            if (invoker == null) {
                throw new ServiceNotFoundException(
                    "Service not found: " + request.getServiceName());
            }
            
            // 调用本地服务
            Object result = invoker.invoke(
                request.getMethodName(),
                request.getParameterTypes(),
                request.getParameters());
            
            response.setResult(result);
            
        } catch (Exception e) {
            log.error("Failed to process request: {}", request, e);
            response.setException(e);
        }
        
        // 发送响应
        ctx.writeAndFlush(response).addListener(future -> {
            if (!future.isSuccess()) {
                log.error("Failed to send response for request: {}", 
                    request.getRequestId(), future.cause());
            }
        });
    }
    
    private void handleHeartbeat(ChannelHandlerContext ctx, HeartbeatMessage heartbeat) {
        // 响应心跳
        HeartbeatMessage response = new HeartbeatMessage();
        response.setType(HeartbeatType.PONG);
        ctx.writeAndFlush(response);
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleEvent = (IdleStateEvent) evt;
            if (idleEvent.state() == IdleState.READER_IDLE) {
                log.warn("Client connection idle, closing channel: {}", ctx.channel());
                ctx.close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception caught in server handler", cause);
        ctx.close();
    }
}
```

### 2. 客户端组件

#### NettyClient - 客户端连接器

```java
/**
 * Netty客户端实现.
 * 
 * 负责建立与服务端的连接，发送RPC请求，接收响应
 */
public class NettyClient implements RpcClient {
    
    private final NetworkConfig config;
    private final EventLoopGroup workerGroup;
    private final ConnectionManager connectionManager;
    private final PendingRequestManager pendingRequestManager;
    
    private volatile boolean started = false;
    
    public NettyClient(NetworkConfig config) {
        this.config = config;
        this.workerGroup = new NioEventLoopGroup(config.getWorkerThreads(),
            new DefaultThreadFactory("ss-rpc-client"));
        this.connectionManager = new ConnectionManager(config, workerGroup);
        this.pendingRequestManager = new PendingRequestManager();
    }
    
    @Override
    public void start() throws NetworkException {
        if (started) {
            return;
        }
        
        try {
            connectionManager.start();
            pendingRequestManager.start();
            started = true;
            log.info("SS-RPC client started");
        } catch (Exception e) {
            throw new NetworkException("Failed to start client", e);
        }
    }
    
    @Override
    public CompletableFuture<RpcResponse> sendRequest(String address, RpcRequest request) {
        if (!started) {
            return CompletableFuture.failedFuture(
                new NetworkException("Client not started"));
        }
        
        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        
        try {
            // 获取连接
            Channel channel = connectionManager.getChannel(address);
            
            // 注册待响应请求
            pendingRequestManager.addPendingRequest(request.getRequestId(), future);
            
            // 设置超时
            setupRequestTimeout(request.getRequestId(), future);
            
            // 发送请求
            channel.writeAndFlush(request).addListener((ChannelFutureListener) channelFuture -> {
                if (!channelFuture.isSuccess()) {
                    pendingRequestManager.removePendingRequest(request.getRequestId());
                    future.completeExceptionally(channelFuture.cause());
                }
            });
            
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    private void setupRequestTimeout(String requestId, CompletableFuture<RpcResponse> future) {
        ScheduledExecutorService scheduler = 
            (ScheduledExecutorService) workerGroup.next().parent().scheduledExecutorService();
        
        scheduler.schedule(() -> {
            if (pendingRequestManager.removePendingRequest(requestId) != null) {
                future.completeExceptionally(
                    new TimeoutException("Request timeout: " + requestId));
            }
        }, config.getRequestTimeout(), TimeUnit.MILLISECONDS);
    }
    
    @Override
    public void shutdown() {
        if (!started) {
            return;
        }
        
        try {
            connectionManager.shutdown();
            pendingRequestManager.shutdown();
            workerGroup.shutdownGracefully();
            started = false;
            log.info("SS-RPC client shutdown completed");
        } catch (Exception e) {
            log.error("Error during client shutdown", e);
        }
    }
}
```

### 3. 连接管理

#### ConnectionManager - 连接管理器

```java
/**
 * 连接管理器.
 * 
 * 负责管理客户端到服务端的连接，包括连接池、连接复用、故障恢复
 */
public class ConnectionManager {
    
    private final NetworkConfig config;
    private final EventLoopGroup workerGroup;
    private final ConcurrentHashMap<String, ChannelPool> channelPools;
    private final ScheduledExecutorService scheduler;
    
    public ConnectionManager(NetworkConfig config, EventLoopGroup workerGroup) {
        this.config = config;
        this.workerGroup = workerGroup;
        this.channelPools = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2, 
            new DefaultThreadFactory("ss-rpc-connection-manager"));
    }
    
    public void start() {
        // 启动连接健康检查
        startHealthCheck();
    }
    
    public Channel getChannel(String address) throws NetworkException {
        ChannelPool pool = getOrCreateChannelPool(address);
        
        try {
            Future<Channel> future = pool.acquire();
            Channel channel = future.get(config.getConnectTimeout(), TimeUnit.MILLISECONDS);
            
            if (channel == null || !channel.isActive()) {
                throw new NetworkException("Failed to acquire active channel for " + address);
            }
            
            return channel;
            
        } catch (Exception e) {
            throw new NetworkException("Failed to get channel for " + address, e);
        }
    }
    
    public void releaseChannel(String address, Channel channel) {
        ChannelPool pool = channelPools.get(address);
        if (pool != null && channel != null) {
            pool.release(channel);
        }
    }
    
    private ChannelPool getOrCreateChannelPool(String address) {
        return channelPools.computeIfAbsent(address, this::createChannelPool);
    }
    
    private ChannelPool createChannelPool(String address) {
        String[] parts = address.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeout())
                .handler(new ClientChannelInitializer());
        
        return new SimpleChannelPool(
            bootstrap.remoteAddress(host, port),
            new ChannelPoolHandler() {
                @Override
                public void channelReleased(Channel ch) throws Exception {
                    // 连接释放时的清理工作
                }
                
                @Override
                public void channelAcquired(Channel ch) throws Exception {
                    // 连接获取时的初始化工作
                }
                
                @Override
                public void channelCreated(Channel ch) throws Exception {
                    // 新连接创建时的初始化工作
                    log.debug("New channel created for {}: {}", address, ch);
                }
            },
            ChannelHealthChecker.ACTIVE
        );
    }
    
    private void startHealthCheck() {
        scheduler.scheduleWithFixedDelay(() -> {
            for (Map.Entry<String, ChannelPool> entry : channelPools.entrySet()) {
                String address = entry.getKey();
                ChannelPool pool = entry.getValue();
                
                // 检查连接池健康状态
                checkPoolHealth(address, pool);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    private void checkPoolHealth(String address, ChannelPool pool) {
        try {
            Future<Channel> future = pool.acquire();
            Channel channel = future.get(1, TimeUnit.SECONDS);
            
            if (channel != null) {
                if (channel.isActive()) {
                    pool.release(channel);
                } else {
                    log.warn("Inactive channel detected for {}, closing", address);
                    channel.close();
                }
            }
            
        } catch (Exception e) {
            log.warn("Health check failed for {}: {}", address, e.getMessage());
        }
    }
    
    public void shutdown() {
        scheduler.shutdown();
        
        for (ChannelPool pool : channelPools.values()) {
            pool.close();
        }
        
        channelPools.clear();
    }
}
```

## 🔌 扩展性设计

### 1. 传输协议扩展

支持多种传输协议：

```java
/**
 * 传输协议工厂
 */
@SPI("netty")
public interface TransportFactory {
    
    RpcServer createServer(NetworkConfig config);
    
    RpcClient createClient(NetworkConfig config);
}

// 实现类
public class NettyTransportFactory implements TransportFactory {
    @Override
    public RpcServer createServer(NetworkConfig config) {
        return new NettyServer(config);
    }
    
    @Override
    public RpcClient createClient(NetworkConfig config) {
        return new NettyClient(config);
    }
}

// 未来可扩展其他传输实现
public class Http2TransportFactory implements TransportFactory {
    // HTTP/2 传输实现
}

public class QuicTransportFactory implements TransportFactory {
    // QUIC 传输实现
}
```

### 2. 网络编解码扩展

支持自定义编解码器：

```java
/**
 * 编解码器工厂
 */
@SPI("default")
public interface CodecFactory {
    
    ChannelHandler createEncoder();
    
    ChannelHandler createDecoder();
}

public class ProtocolCodecFactory implements CodecFactory {
    @Override
    public ChannelHandler createEncoder() {
        return new RpcProtocolEncoder();
    }
    
    @Override
    public ChannelHandler createDecoder() {
        return new RpcProtocolDecoder();
    }
}
```

### 3. 连接策略扩展

支持不同的连接策略：

```java
/**
 * 连接策略
 */
@SPI("pool")
public interface ConnectionStrategy {
    
    Channel getConnection(String address) throws NetworkException;
    
    void releaseConnection(String address, Channel channel);
}

// 连接池策略
public class PooledConnectionStrategy implements ConnectionStrategy {
    // 使用连接池
}

// 短连接策略
public class ShortConnectionStrategy implements ConnectionStrategy {
    // 每次请求创建新连接
}

// 长连接策略
public class LongConnectionStrategy implements ConnectionStrategy {
    // 保持长连接
}
```

## 📊 性能优化

### 1. 零拷贝优化

```java
// 使用Netty的零拷贝机制
public class ZeroCopyOptimization {
    
    // 使用CompositeByteBuf避免内存拷贝
    public ByteBuf encodeMessage(Object message) {
        CompositeByteBuf composite = Unpooled.compositeBuffer();
        
        // 添加头部
        ByteBuf header = encodeHeader(message);
        composite.addComponent(true, header);
        
        // 添加body
        ByteBuf body = encodeBody(message);
        composite.addComponent(true, body);
        
        return composite;
    }
    
    // 使用FileRegion进行文件传输
    public void sendFile(Channel channel, File file) {
        FileRegion region = new DefaultFileRegion(
            file, 0, file.length());
        channel.writeAndFlush(region);
    }
}
```

### 2. 内存池化

```java
// 使用Netty的池化内存分配器
public class MemoryOptimization {
    
    // 配置池化内存分配器
    public void configureAllocator(Bootstrap bootstrap) {
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    }
    
    // 及时释放ByteBuf
    public void handleMessage(ByteBuf buf) {
        try {
            // 处理消息
            processMessage(buf);
        } finally {
            // 确保释放内存
            ReferenceCountUtil.release(buf);
        }
    }
}
```

### 3. 批量操作

```java
// 批量发送优化
public class BatchOptimization {
    
    private final List<RpcRequest> batchRequests = new ArrayList<>();
    private final ScheduledExecutorService scheduler;
    
    public void addRequest(RpcRequest request) {
        synchronized (batchRequests) {
            batchRequests.add(request);
            
            if (batchRequests.size() >= BATCH_SIZE) {
                flushBatch();
            }
        }
    }
    
    private void flushBatch() {
        if (!batchRequests.isEmpty()) {
            List<RpcRequest> batch = new ArrayList<>(batchRequests);
            batchRequests.clear();
            
            // 批量发送
            sendBatch(batch);
        }
    }
}
```

## 🎯 下一步开发

基于这个设计，我们可以开始实现第一个网络模块：

1. **先实现基础接口和数据结构**
2. **再实现NettyServer和NettyClient**
3. **添加连接管理和处理器**
4. **编写单元测试验证功能**

这个设计确保了网络模块具有：
- **高性能**: 基于Netty的异步非阻塞I/O
- **高可用**: 连接池、心跳检测、故障恢复
- **可扩展**: SPI机制支持协议和策略扩展
- **易维护**: 清晰的模块划分和接口设计

现在您对网络模块有了全面的了解，我们可以开始具体的编码实现了！ 