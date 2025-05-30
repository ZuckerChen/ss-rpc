# SS-RPC 技术架构设计

## 1. 总体架构

### 1.1 系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        服务消费者                                 │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │   业务代码   │  │  代理工厂   │  │  配置管理   │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
├─────────────────────────────────────────────────────────────────┤
│                      SS-RPC Client                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ 服务发现    │  │ 负载均衡    │  │ 容错处理    │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ 网络通信    │  │ 序列化      │  │ 监控统计    │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
└─────────────────────────────────────────────────────────────────┘
                               │
                           网络传输
                               │
┌─────────────────────────────────────────────────────────────────┐
│                      注册中心                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │  ZooKeeper  │  │    Nacos    │  │   Consul    │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
└─────────────────────────────────────────────────────────────────┘
                               │
                           服务注册
                               │
┌─────────────────────────────────────────────────────────────────┐
│                      SS-RPC Server                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ 服务注册    │  │ 请求处理    │  │ 配置管理    │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ 网络通信    │  │ 序列化      │  │ 监控统计    │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
├─────────────────────────────────────────────────────────────────┤
│                        服务提供者                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │   业务代码   │  │  服务暴露   │  │  配置管理   │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 技术选型

| 技术领域 | 选择方案 | 理由 |
|---------|---------|------|
| 网络通信 | Netty | 高性能、成熟稳定、异步非阻塞 |
| 序列化 | JSON/Protobuf/Kryo | 平衡性能和兼容性 |
| 注册中心 | Zookeeper/Nacos | 成熟的服务发现方案 |
| 代理机制 | JDK动态代理/CGLIB | 透明化远程调用 |
| 配置管理 | YAML/Properties | 简单易用的配置方式 |
| 日志框架 | SLF4J + Logback | 业界标准日志方案 |

## 2. 核心模块设计

### 2.1 ss-rpc-core（核心模块）

#### 2.1.1 包结构
```
com.ssrpc.core
├── annotation          // 注解定义
├── config             // 配置管理
├── proxy              // 代理机制
├── client             // 客户端
├── server             // 服务端
├── common             // 公共组件
├── exception          // 异常定义
└── utils              // 工具类
```

#### 2.1.2 核心类设计

**RpcClient - 客户端核心类**
```java
public class RpcClient {
    private final RpcConfig config;
    private final ServiceDiscovery serviceDiscovery;
    private final LoadBalancer loadBalancer;
    private final NettyClient nettyClient;
    
    public <T> T createProxy(Class<T> serviceClass);
    public CompletableFuture<Object> invokeAsync(RpcRequest request);
    public Object invoke(RpcRequest request);
}
```

**RpcServer - 服务端核心类**
```java
public class RpcServer {
    private final RpcConfig config;
    private final ServiceRegistry serviceRegistry;
    private final NettyServer nettyServer;
    private final Map<String, Object> serviceMap;
    
    public void registerService(Object service);
    public void start();
    public void stop();
}
```

### 2.2 ss-rpc-protocol（协议模块）

#### 2.2.1 协议设计
```
SS-RPC协议格式（24字节头部 + 可变长度body）：
┌───────────────────────────────────────────────────────────────┐
│  魔数   │ 版本 │ 消息类型 │ 序列化类型 │ 状态码 │   消息ID    │
│ 4bytes │1byte│  1byte  │   1byte   │ 1byte │   8bytes   │
├───────────────────────────────────────────────────────────────┤
│                     消息长度（4bytes）                         │
├───────────────────────────────────────────────────────────────┤
│                         消息体                                │
│                      （变长）                                 │
└───────────────────────────────────────────────────────────────┘
```

#### 2.2.2 消息类型定义
```java
public class RpcMessage {
    private byte messageType;    // 1-请求, 2-响应, 3-心跳
    private byte serializeType;  // 1-JSON, 2-Protobuf, 3-Kryo
    private byte statusCode;     // 0-成功, 1-失败
    private long messageId;      // 消息唯一标识
    private Object data;         // 消息体
}

public class RpcRequest {
    private String requestId;
    private String serviceName;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] parameters;
    private String version;
}

public class RpcResponse {
    private String requestId;
    private Object result;
    private String errorMessage;
    private Class<?> returnType;
}
```

### 2.3 ss-rpc-registry（注册中心模块）

#### 2.3.1 服务注册发现接口
```java
public interface ServiceRegistry {
    void register(ServiceInfo serviceInfo);
    void unregister(ServiceInfo serviceInfo);
}

public interface ServiceDiscovery {
    List<ServiceInfo> discover(String serviceName);
    void watch(String serviceName, ServiceChangeListener listener);
}

public class ServiceInfo {
    private String serviceName;
    private String host;
    private int port;
    private String version;
    private Map<String, String> metadata;
}
```

### 2.4 ss-rpc-serialization（序列化模块）

#### 2.4.1 序列化接口设计
```java
public interface Serializer {
    <T> byte[] serialize(T object);
    <T> T deserialize(byte[] bytes, Class<T> clazz);
    SerializeType getType();
}

public enum SerializeType {
    JSON((byte) 1, "json"),
    PROTOBUF((byte) 2, "protobuf"),
    KRYO((byte) 3, "kryo");
}
```

### 2.5 ss-rpc-loadbalance（负载均衡模块）

#### 2.5.1 负载均衡接口
```java
public interface LoadBalancer {
    ServiceInfo select(List<ServiceInfo> services, RpcRequest request);
}

public class RandomLoadBalancer implements LoadBalancer;
public class RoundRobinLoadBalancer implements LoadBalancer;
public class WeightedRoundRobinLoadBalancer implements LoadBalancer;
public class ConsistentHashLoadBalancer implements LoadBalancer;
```

## 3. 网络通信设计

### 3.1 Netty服务端设计
```java
public class NettyServer {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap bootstrap;
    
    public void start(int port) {
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                          .addLast(new RpcDecoder())
                          .addLast(new RpcEncoder())
                          .addLast(new RpcServerHandler());
                    }
                });
    }
}
```

### 3.2 Netty客户端设计
```java
public class NettyClient {
    private EventLoopGroup eventLoopGroup;
    private Bootstrap bootstrap;
    private final Map<String, Channel> channelMap = new ConcurrentHashMap<>();
    
    public Channel getChannel(String address) {
        return channelMap.computeIfAbsent(address, this::createChannel);
    }
    
    public CompletableFuture<RpcResponse> send(RpcRequest request, String address);
}
```

## 4. 代理机制设计

### 4.1 JDK动态代理实现
```java
public class JdkProxyFactory implements ProxyFactory {
    @Override
    public <T> T createProxy(Class<T> serviceClass, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
            serviceClass.getClassLoader(),
            new Class[]{serviceClass},
            handler
        );
    }
}

public class RpcInvocationHandler implements InvocationHandler {
    private final RpcClient rpcClient;
    private final String serviceName;
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcRequest request = buildRequest(method, args);
        return rpcClient.invoke(request);
    }
}
```

## 5. 配置管理设计

### 5.1 配置类设计
```java
@Data
public class RpcConfig {
    private String registryAddress;
    private String registryType = "zookeeper";
    private SerializeType serializeType = SerializeType.JSON;
    private LoadBalanceType loadBalanceType = LoadBalanceType.RANDOM;
    private int timeout = 5000;
    private int retryTimes = 3;
    private boolean enableHeartbeat = true;
    private int heartbeatInterval = 30000;
}
```

### 5.2 配置文件示例
```yaml
ss-rpc:
  registry:
    address: "127.0.0.1:2181"
    type: "zookeeper"
  serialize:
    type: "json"
  loadbalance:
    type: "random"
  timeout: 5000
  retry-times: 3
  heartbeat:
    enable: true
    interval: 30000
```

## 6. 注解设计

### 6.1 核心注解定义
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcService {
    String value() default "";
    String version() default "1.0.0";
    int weight() default 100;
}

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcReference {
    String value() default "";
    String version() default "1.0.0";
    long timeout() default 5000;
    int retryTimes() default 3;
    LoadBalanceType loadBalance() default LoadBalanceType.RANDOM;
}
```

## 7. 异常处理设计

### 7.1 异常体系
```java
public class RpcException extends RuntimeException {
    private final RpcErrorCode errorCode;
}

public enum RpcErrorCode {
    SERVICE_NOT_FOUND(404, "服务未找到"),
    NETWORK_ERROR(500, "网络异常"),
    SERIALIZE_ERROR(501, "序列化异常"),
    TIMEOUT_ERROR(502, "调用超时"),
    LOAD_BALANCE_ERROR(503, "负载均衡异常");
}
```

## 8. 监控统计设计

### 8.1 监控指标
```java
public class RpcMetrics {
    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong successRequests = new AtomicLong();
    private final AtomicLong failedRequests = new AtomicLong();
    private final ConcurrentHashMap<String, MethodMetrics> methodMetrics;
    
    public void recordRequest(String method, long responseTime, boolean success);
    public MetricsSnapshot getSnapshot();
}
```

这个架构设计为SS-RPC框架提供了清晰的技术路线和实现指导，既保证了功能的完整性，又考虑了代码的可维护性和可扩展性。 