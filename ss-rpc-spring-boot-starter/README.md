# SS-RPC Spring Boot Starter

SS-RPC的Spring Boot集成模块，提供开箱即用的RPC功能。

## 功能特性

- 🚀 **自动配置**: 零配置启动，开箱即用
- 🔧 **灵活配置**: 支持丰富的配置选项
- 📝 **注解驱动**: 基于`@RpcService`和`@RpcReference`注解
- 🔄 **自动注册**: 自动扫描和注册RPC服务
- 💉 **依赖注入**: 自动注入RPC服务代理
- 🎯 **IDE支持**: 完整的配置提示和自动补全

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.ssrpc</groupId>
    <artifactId>ss-rpc-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 启用RPC

在Spring Boot主类上添加`@EnableRpc`注解：

```java
@SpringBootApplication
@EnableRpc
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 定义服务接口

```java
public interface UserService {
    User getUserById(Long id);
    List<User> getAllUsers();
}
```

### 4. 实现服务提供者

```java
@RpcService(version = "1.0.0", weight = 100)
@Service
public class UserServiceImpl implements UserService {
    
    @Override
    public User getUserById(Long id) {
        // 业务逻辑
        return new User(id, "张三");
    }
    
    @Override
    public List<User> getAllUsers() {
        // 业务逻辑
        return Arrays.asList(
            new User(1L, "张三"),
            new User(2L, "李四")
        );
    }
}
```

### 5. 使用服务消费者

```java
@RestController
public class UserController {
    
    @RpcReference(version = "1.0.0", timeout = 5000)
    private UserService userService;
    
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }
    
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }
}
```

## 配置选项

### 基础配置

```yaml
ss-rpc:
  enabled: true  # 是否启用RPC，默认true
```

### 服务端配置

```yaml
ss-rpc:
  server:
    port: 9999              # 服务端口，默认9999
    host: localhost         # 服务主机，默认localhost
    worker-threads: 8       # 工作线程数，默认CPU核数*2
    boss-threads: 1         # Boss线程数，默认1
    connect-timeout: 5000   # 连接超时时间（毫秒），默认5000
```

### 客户端配置

```yaml
ss-rpc:
  client:
    connect-timeout: 5000   # 连接超时时间（毫秒），默认5000
    request-timeout: 10000  # 请求超时时间（毫秒），默认10000
    retry-times: 3          # 重试次数，默认3
```

### 注册中心配置

```yaml
ss-rpc:
  registry:
    type: memory            # 注册中心类型，默认memory
    address: ""             # 注册中心地址
    connect-timeout: 5000   # 连接超时时间（毫秒），默认5000
    session-timeout: 30000  # 会话超时时间（毫秒），默认30000
```

支持的注册中心类型：
- `memory`: 内存注册中心（开发测试用）
- `zookeeper`: ZooKeeper注册中心（生产环境）
- `nacos`: Nacos注册中心（生产环境）

### 序列化配置

```yaml
ss-rpc:
  serialization:
    type: json  # 序列化类型，默认json
```

支持的序列化类型：
- `json`: JSON序列化（基于Jackson）
- `jdk`: JDK原生序列化
- `kryo`: Kryo序列化（高性能）
- `protobuf`: Protocol Buffers序列化

### 负载均衡配置

```yaml
ss-rpc:
  load-balance:
    algorithm: round_robin  # 负载均衡算法，默认round_robin
```

支持的负载均衡算法：
- `round_robin`: 轮询
- `random`: 随机
- `weighted_round_robin`: 加权轮询
- `least_connections`: 最少连接数

## 完整配置示例

```yaml
ss-rpc:
  enabled: true
  server:
    port: 8080
    host: 0.0.0.0
    worker-threads: 16
    boss-threads: 2
    connect-timeout: 3000
  client:
    connect-timeout: 3000
    request-timeout: 8000
    retry-times: 2
  registry:
    type: memory
    address: ""
    connect-timeout: 3000
    session-timeout: 60000
  serialization:
    type: json
  load-balance:
    algorithm: random
```

## 注解说明

### @RpcService

标记在服务实现类上，表示该类提供RPC服务。

```java
@RpcService(
    value = "",           // 服务名称，默认为接口名
    version = "1.0.0",    // 服务版本号，默认1.0.0
    weight = 100,         // 服务权重，默认100
    hotReload = false     // 是否启用热重载，默认false
)
```

### @RpcReference

标记在字段上，表示需要注入RPC服务代理对象。

```java
@RpcReference(
    value = "",              // 服务名称，默认为字段类型名
    version = "1.0.0",       // 服务版本号，默认1.0.0
    timeout = 5000,          // 调用超时时间（毫秒），默认5000
    retryTimes = 3,          // 重试次数，默认3
    loadBalance = "random",  // 负载均衡策略，默认random
    async = false            // 是否异步调用，默认false
)
```

## 异步调用

SS-RPC支持异步调用，返回`CompletableFuture`：

```java
public interface AsyncUserService {
    CompletableFuture<User> getUserByIdAsync(Long id);
}

@RestController
public class AsyncUserController {
    
    @RpcReference(version = "1.0.0", async = true)
    private AsyncUserService userService;
    
    @GetMapping("/users/{id}/async")
    public CompletableFuture<User> getUserAsync(@PathVariable Long id) {
        return userService.getUserByIdAsync(id);
    }
}
```

## 最佳实践

### 1. 服务接口设计

- 接口方法应该是幂等的
- 参数和返回值应该实现`Serializable`接口
- 避免使用复杂的继承关系

### 2. 异常处理

```java
@RpcService
@Service
public class UserServiceImpl implements UserService {
    
    @Override
    public User getUserById(Long id) {
        try {
            // 业务逻辑
            return userRepository.findById(id);
        } catch (Exception e) {
            log.error("Failed to get user by id: {}", id, e);
            throw new RpcException("User not found", e);
        }
    }
}
```

### 3. 配置管理

- 开发环境使用内存注册中心
- 生产环境使用ZooKeeper或Nacos
- 根据网络环境调整超时时间
- 根据服务重要性设置重试次数

### 4. 监控和日志

```yaml
logging:
  level:
    com.ssrpc: DEBUG  # 开启RPC调试日志
```

## 故障排除

### 常见问题

1. **服务注册失败**
   - 检查注册中心配置是否正确
   - 确认注册中心服务是否正常运行

2. **服务调用超时**
   - 增加超时时间配置
   - 检查网络连接是否正常
   - 确认服务提供者是否正常运行

3. **序列化异常**
   - 确认参数和返回值实现了`Serializable`接口
   - 检查序列化器配置是否正确

4. **负载均衡不生效**
   - 确认有多个服务实例注册
   - 检查负载均衡算法配置

### 调试技巧

1. 开启调试日志：
```yaml
logging:
  level:
    com.ssrpc: DEBUG
```

2. 使用Spring Boot Actuator监控：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

## 版本兼容性

| SS-RPC版本 | Spring Boot版本 | Java版本 |
|-----------|----------------|----------|
| 1.0.x     | 2.3.x - 2.7.x  | 8+       |

## 更新日志

### v1.0.0
- 初始版本发布
- 支持基本的RPC功能
- 集成Spring Boot自动配置
- 支持多种序列化方式
- 支持多种负载均衡算法 