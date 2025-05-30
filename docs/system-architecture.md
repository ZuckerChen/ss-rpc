# SS-RPC 系统架构设计

## 🏗️ 整体架构概览

SS-RPC采用分层模块化架构，每个模块职责单一，通过清晰的接口进行交互，支持灵活的扩展和替换。

```
┌─────────────────────────────────────────────────────────────┐
│                    应用层 (Application Layer)                │
├─────────────────────────────────────────────────────────────┤
│  ss-rpc-spring-boot-starter  │      ss-rpc-examples         │
│  - 自动配置                    │      - 使用示例               │
│  - Spring Boot 集成          │      - 集成测试               │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    RPC核心层 (RPC Core Layer)               │
├─────────────────────────────────────────────────────────────┤
│                    ss-rpc-core                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │   Proxy     │  │  Invoker    │  │ Annotation  │          │
│  │   Factory   │  │   Engine    │  │  Processor  │          │
│  └─────────────┘  └─────────────┘  └─────────────┘          │
│          │               │               │                  │
│          └───────────────┼───────────────┘                  │
│                          │                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │  Network    │  │    RPC      │  │   Config    │          │
│  │  Manager    │  │  Context    │  │  Manager    │          │
│  └─────────────┘  └─────────────┘  └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
         │                  │                 │
         ▼                  ▼                 ▼
┌─────────────────────────────────────────────────────────────┐
│                   基础服务层 (Service Layer)                 │
├─────────────────┬─────────────────┬─────────────────────────┤
│  ss-rpc-registry│ ss-rpc-protocol │  ss-rpc-serialization   │
│  ┌─────────────┐│ ┌─────────────┐ │  ┌─────────────┐        │
│  │   Service   ││ │  Protocol   │ │  │ Serializer  │        │
│  │  Registry   ││ │   Codec     │ │  │   Factory   │        │
│  └─────────────┘│ └─────────────┘ │  └─────────────┘        │
│  ┌─────────────┐│ ┌─────────────┐ │  ┌─────────────┐        │
│  │  Service    ││ │  Message    │ │  │    JSON     │        │
│  │ Discovery   ││ │   Handler   │ │  │ Serializer  │        │
│  └─────────────┘│ └─────────────┘ │  └─────────────┘        │
├─────────────────┼─────────────────┼─────────────────────────┤
│ ss-rpc-loadbalance                │                         │
│  ┌─────────────┐  ┌─────────────┐ │                         │
│  │    Load     │  │   Server    │ │                         │
│  │  Balancer   │  │  Selector   │ │                         │
│  └─────────────┘  └─────────────┘ │                         │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    传输层 (Transport Layer)                 │
├─────────────────────────────────────────────────────────────┤
│                       Netty NIO                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │   Server    │  │   Client    │  │  Connection │          │
│  │  Bootstrap  │  │  Bootstrap  │  │   Manager   │          │
│  └─────────────┘  └─────────────┘  └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

## 📦 模块职责详解

### 1. ss-rpc-core (核心模块)

**职责**: RPC框架的核心逻辑，协调各个模块工作

**主要组件**:
```java
// 网络管理器
public interface NetworkManager {
    void startServer(int port);
    void stopServer();
    <T> T createProxy(Class<T> serviceInterface, String serverAddress);
}

// RPC调用器
public interface RpcInvoker {
    Object invoke(RpcRequest request) throws RpcException;
}

// 代理工厂
public interface ProxyFactory {
    <T> T createProxy(Class<T> serviceInterface, RpcInvoker invoker);
}

// 注解处理器
public interface AnnotationProcessor {
    void processRpcService(Object serviceBean);
    void processRpcReference(Object bean, Field field);
}
```

### 2. ss-rpc-protocol (协议模块)

**职责**: 定义RPC通信协议，处理消息编解码

**协议格式**:
```
+-------+--------+----------+----------+----------+----------+
| Magic | Version| Type     | Status   | Length   | Body     |
| 4bytes| 1byte  | 1byte    | 1byte    | 4bytes   | N bytes  |
+-------+--------+----------+----------+----------+----------+
```

**主要组件**:
```java
// 协议编解码器
public interface ProtocolCodec {
    byte[] encode(Object message) throws ProtocolException;
    Object decode(byte[] data) throws ProtocolException;
}

// 消息类型
public enum MessageType {
    REQUEST(1),
    RESPONSE(2),
    HEARTBEAT(3);
}

// RPC请求消息
public class RpcRequest {
    private String requestId;
    private String serviceName;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] parameters;
}

// RPC响应消息
public class RpcResponse {
    private String requestId;
    private Object result;
    private Throwable exception;
}
```

### 3. ss-rpc-serialization (序列化模块)

**职责**: 提供多种序列化方式，支持扩展

**主要组件**:
```java
// 序列化器接口
public interface Serializer {
    byte[] serialize(Object obj) throws SerializationException;
    <T> T deserialize(byte[] data, Class<T> clazz) throws SerializationException;
}

// 序列化器工厂
public class SerializerFactory {
    private static final Map<String, Serializer> serializers = new HashMap<>();
    
    static {
        register("json", new JsonSerializer());
        register("kryo", new KryoSerializer());
        register("protobuf", new ProtobufSerializer());
    }
    
    public static Serializer getSerializer(String type) {
        return serializers.get(type);
    }
}
```

### 4. ss-rpc-registry (注册中心模块)

**职责**: 服务注册与发现，支持多种注册中心

**主要组件**:
```java
// 服务注册接口
public interface ServiceRegistry {
    void register(ServiceMetadata service) throws RegistryException;
    void unregister(ServiceMetadata service) throws RegistryException;
}

// 服务发现接口
public interface ServiceDiscovery {
    List<ServiceInstance> discover(String serviceName) throws RegistryException;
    void subscribe(String serviceName, ServiceChangeListener listener);
}

// 服务元数据
public class ServiceMetadata {
    private String serviceName;
    private String version;
    private String address;
    private int port;
    private Map<String, String> metadata;
}
```

### 5. ss-rpc-loadbalance (负载均衡模块)

**职责**: 提供多种负载均衡策略

**主要组件**:
```java
// 负载均衡器接口
public interface LoadBalancer {
    ServiceInstance select(List<ServiceInstance> instances, RpcRequest request);
}

// 负载均衡策略
public enum LoadBalanceType {
    RANDOM,
    ROUND_ROBIN,
    WEIGHTED_RANDOM,
    CONSISTENT_HASH,
    LEAST_ACTIVE
}
```

## 🔄 模块交互流程

### 服务提供方启动流程

```
1. Spring Boot 启动
   ↓
2. RpcAutoConfiguration 自动配置
   ↓
3. AnnotationProcessor 扫描 @RpcService 注解
   ↓
4. ServiceRegistry 注册服务到注册中心
   ↓
5. NetworkManager 启动 Netty 服务器
   ↓
6. 等待客户端连接
```

### 服务消费方调用流程

```
1. ProxyFactory 创建服务代理对象
   ↓
2. 客户端调用代理方法
   ↓
3. 代理对象拦截调用，创建 RpcRequest
   ↓
4. ServiceDiscovery 从注册中心获取服务实例
   ↓
5. LoadBalancer 选择目标服务实例
   ↓
6. ProtocolCodec 编码请求消息
   ↓
7. NetworkManager 发送请求到服务端
   ↓
8. 等待响应，解码并返回结果
```

### RPC调用详细时序图

```
Client                 Proxy              Registry           LoadBalancer        Server
  │                     │                     │                   │                │
  │──────call method───▶│                     │                   │                │
  │                     │────discover service──▶│                   │                │
  │                     │◄──service instances───│                   │                │
  │                     │────select instance──────────────────────▶│                │
  │                     │◄──selected instance─────────────────────│                │
  │                     │────────────────────send request───────────────────────▶│
  │                     │                     │                   │        ┌─────▶│ invoke
  │                     │                     │                   │        │      │ method
  │                     │                     │                   │        └─────◄│
  │                     │◄───────────────────send response──────────────────────│
  │◄─────return────────│                     │                   │                │
```

## 🌐 网络模型设计

### 1. 网络架构

SS-RPC采用**Reactor模式**的异步非阻塞网络模型：

```
                              Netty Server
┌─────────────────────────────────────────────────────────┐
│                     Boss Group                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │   Accept    │  │   Accept    │  │   Accept    │     │
│  │  Thread 1   │  │  Thread 2   │  │  Thread N   │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│                    Worker Group                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │   I/O       │  │    I/O      │  │    I/O      │     │
│  │  Thread 1   │  │  Thread 2   │  │  Thread N   │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│                 Business Thread Pool                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │  Business   │  │  Business   │  │  Business   │     │
│  │  Thread 1   │  │  Thread 2   │  │  Thread N   │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
└─────────────────────────────────────────────────────────┘
```

### 2. 网络组件设计

#### NettyServer (服务端)

```java
public class NettyServer implements RpcServer {
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final ExecutorService businessThreadPool;
    private Channel serverChannel;
    
    @Override
    public void start(int port) throws NetworkException {
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 协议编解码器
                            pipeline.addLast(new ProtocolDecoder());
                            pipeline.addLast(new ProtocolEncoder());
                            // 心跳检测
                            pipeline.addLast(new IdleStateHandler(60, 0, 0));
                            // 业务处理器
                            pipeline.addLast(new RpcServerHandler(businessThreadPool));
                        }
                    });
            
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            log.info("SS-RPC server started on port {}", port);
            
        } catch (Exception e) {
            throw new NetworkException("Failed to start server", e);
        }
    }
}
```

#### NettyClient (客户端)

```java
public class NettyClient implements RpcClient {
    private final EventLoopGroup workerGroup;
    private final ConcurrentHashMap<String, Channel> channelPool;
    private final ConcurrentHashMap<String, CompletableFuture<RpcResponse>> pendingRequests;
    
    @Override
    public CompletableFuture<RpcResponse> sendRequest(String address, RpcRequest request) {
        Channel channel = getOrCreateChannel(address);
        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        
        // 存储待响应的请求
        pendingRequests.put(request.getRequestId(), future);
        
        // 发送请求
        channel.writeAndFlush(request).addListener((ChannelFutureListener) channelFuture -> {
            if (!channelFuture.isSuccess()) {
                pendingRequests.remove(request.getRequestId());
                future.completeExceptionally(channelFuture.cause());
            }
        });
        
        return future;
    }
    
    private Channel getOrCreateChannel(String address) {
        return channelPool.computeIfAbsent(address, this::createChannel);
    }
}
```

### 3. 连接管理

#### 连接池设计

```java
public class ConnectionManager {
    private final Map<String, ChannelPool> channelPools;
    private final ChannelPoolFactory poolFactory;
    
    public class ChannelPoolConfig {
        private int maxConnections = 10;
        private int minConnections = 2;
        private long maxIdleTime = 300_000; // 5分钟
        private long connectTimeout = 5_000; // 5秒
    }
    
    public Channel acquireChannel(String address) throws NetworkException {
        ChannelPool pool = channelPools.computeIfAbsent(address, 
            addr -> poolFactory.createPool(addr, config));
        return pool.acquire().get();
    }
    
    public void releaseChannel(String address, Channel channel) {
        ChannelPool pool = channelPools.get(address);
        if (pool != null) {
            pool.release(channel);
        }
    }
}
```

## 🔌 扩展性设计

### 1. SPI (Service Provider Interface) 机制

SS-RPC使用SPI机制支持组件的灵活扩展：

```java
// SPI接口定义
public interface ExtensionFactory {
    <T> T getExtension(Class<T> type, String name);
    <T> List<T> getExtensions(Class<T> type);
}

// 扩展点注解
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SPI {
    String value() default "";
}

// 自适应扩展
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Adaptive {
    String[] value() default {};
}
```

### 2. 主要扩展点

#### 序列化扩展

```java
@SPI("json")
public interface Serializer {
    byte[] serialize(Object obj) throws SerializationException;
    <T> T deserialize(byte[] data, Class<T> clazz) throws SerializationException;
}

// META-INF/services/com.ssrpc.serialization.Serializer
json=com.ssrpc.serialization.JsonSerializer
kryo=com.ssrpc.serialization.KryoSerializer
protobuf=com.ssrpc.serialization.ProtobufSerializer
```

#### 注册中心扩展

```java
@SPI("memory")
public interface ServiceRegistry {
    void register(ServiceMetadata service) throws RegistryException;
    void unregister(ServiceMetadata service) throws RegistryException;
}

// META-INF/services/com.ssrpc.registry.ServiceRegistry
memory=com.ssrpc.registry.MemoryServiceRegistry
zookeeper=com.ssrpc.registry.ZookeeperServiceRegistry
nacos=com.ssrpc.registry.NacosServiceRegistry
```

#### 负载均衡扩展

```java
@SPI("random")
public interface LoadBalancer {
    @Adaptive
    ServiceInstance select(List<ServiceInstance> instances, RpcRequest request);
}

// META-INF/services/com.ssrpc.loadbalance.LoadBalancer  
random=com.ssrpc.loadbalance.RandomLoadBalancer
roundRobin=com.ssrpc.loadbalance.RoundRobinLoadBalancer
consistentHash=com.ssrpc.loadbalance.ConsistentHashLoadBalancer
```

### 3. 插件化架构

```java
// 插件接口
public interface Plugin {
    void initialize(PluginContext context);
    void start();
    void stop();
    String getName();
    String getVersion();
}

// 插件管理器
public class PluginManager {
    private final Map<String, Plugin> plugins = new HashMap<>();
    
    public void loadPlugin(String pluginPath) {
        // 动态加载插件
    }
    
    public void enablePlugin(String pluginName) {
        Plugin plugin = plugins.get(pluginName);
        if (plugin != null) {
            plugin.start();
        }
    }
}
```

### 4. 配置化扩展

```yaml
ss-rpc:
  # 网络配置
  network:
    server:
      port: 8080
      boss-threads: 1
      worker-threads: 4
      business-threads: 200
    client:
      connect-timeout: 5000
      max-connections: 10
  
  # 序列化配置
  serialization:
    type: json
    
  # 注册中心配置
  registry:
    type: zookeeper
    address: localhost:2181
    
  # 负载均衡配置
  loadbalance:
    type: consistentHash
    
  # 插件配置
  plugins:
    enabled:
      - monitoring
      - tracing
```

## 🎯 关键设计原则

### 1. 单一职责原则
每个模块只负责一个特定的功能领域

### 2. 开闭原则  
对扩展开放，对修改关闭，通过SPI机制支持扩展

### 3. 依赖倒置原则
高层模块不依赖低层模块，都依赖于抽象

### 4. 接口隔离原则
使用多个专门的接口，而不是单一的总接口

### 5. 最小化依赖
每个模块只依赖必要的外部组件

这种架构设计确保了SS-RPC具有良好的可扩展性、可维护性和性能特征。现在您对整体架构有了清晰的了解，我们可以开始第一个网络模块的开发了！ 