# SS-RPC Phase 1 MVP 开发计划

## 🎯 MVP目标

实现SS-RPC框架的最小可用版本，包含：
- 基础RPC调用功能
- 简单的网络通信
- 基本的序列化支持
- 服务注册与发现
- 简单的负载均衡

## 📅 开发时间线

**总计：6周（Phase 1）**
- Week 1-2: 核心通信模块
- Week 3-4: 序列化和协议模块  
- Week 5-6: 服务注册发现和集成测试

## 🔧 技术实现路径

### Week 1-2: 网络通信模块 (ss-rpc-core)

#### 目标
实现基于Netty的客户端-服务端通信

#### 任务清单

**ss-rpc-core模块**：

1. **网络传输层**
   - [ ] `NettyServer` - Netty服务端实现
   - [ ] `NettyClient` - Netty客户端实现  
   - [ ] `ChannelHandler` - 消息处理器
   - [ ] `ConnectionManager` - 连接管理器

2. **RPC调用核心**
   - [ ] `RpcRequest` - RPC请求对象
   - [ ] `RpcResponse` - RPC响应对象
   - [ ] `RpcInvoker` - RPC调用器
   - [ ] `ProxyFactory` - 代理工厂

3. **注解处理**
   - [ ] 完善 `@RpcService` 注解处理逻辑
   - [ ] 完善 `@RpcReference` 注解处理逻辑
   - [ ] `AnnotationScanner` - 注解扫描器

#### 代码结构
```
ss-rpc-core/
├── network/
│   ├── NettyServer.java
│   ├── NettyClient.java
│   ├── ChannelHandler.java
│   └── ConnectionManager.java
├── rpc/
│   ├── RpcRequest.java
│   ├── RpcResponse.java
│   ├── RpcInvoker.java
│   └── ProxyFactory.java
├── annotation/
│   ├── RpcService.java (已存在)
│   ├── RpcReference.java (已存在)
│   └── AnnotationScanner.java
└── config/
    └── RpcConfig.java
```

### Week 3-4: 协议和序列化模块

#### ss-rpc-protocol模块

1. **协议定义**
   - [ ] `RpcProtocol` - 协议格式定义
   - [ ] `ProtocolEncoder` - 协议编码器
   - [ ] `ProtocolDecoder` - 协议解码器
   - [ ] `MessageType` - 消息类型枚举

#### ss-rpc-serialization模块

1. **序列化实现**
   - [ ] `Serializer` - 序列化接口
   - [ ] `JsonSerializer` - JSON序列化实现
   - [ ] `SerializerFactory` - 序列化工厂
   - [ ] `SerializationException` - 序列化异常

### Week 5-6: 服务注册发现和整合

#### ss-rpc-registry模块

1. **注册中心**
   - [ ] `ServiceRegistry` - 服务注册接口
   - [ ] `ServiceDiscovery` - 服务发现接口
   - [ ] `MemoryRegistry` - 内存注册中心（用于测试）
   - [ ] `RegistryConfig` - 注册中心配置

#### ss-rpc-loadbalance模块

1. **负载均衡**
   - [ ] `LoadBalancer` - 负载均衡接口
   - [ ] `RandomLoadBalancer` - 随机负载均衡
   - [ ] `RoundRobinLoadBalancer` - 轮询负载均衡

#### 整合和测试

1. **Spring Boot整合**
   - [ ] `RpcAutoConfiguration` - 自动配置类
   - [ ] `RpcProperties` - 配置属性
   - [ ] `@EnableRpc` - 启用注解

2. **示例和测试**
   - [ ] 完整的使用示例
   - [ ] 单元测试
   - [ ] 集成测试

## 🚀 MVP功能特性

### 核心功能
- ✅ 基于Netty的高性能网络通信
- ✅ 同步RPC调用
- ✅ JSON序列化支持
- ✅ 基于内存的服务注册发现
- ✅ 随机和轮询负载均衡
- ✅ Spring Boot自动配置

### 使用示例

**服务提供方**：
```java
@RpcService
public class UserServiceImpl implements UserService {
    @Override
    public User getUserById(Long id) {
        return new User(id, "John Doe");
    }
}

@SpringBootApplication
@EnableRpc
public class ProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }
}
```

**服务消费方**：
```java
@RestController
public class UserController {
    
    @RpcReference
    private UserService userService;
    
    @GetMapping("/user/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }
}

@SpringBootApplication
@EnableRpc
public class ConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}
```

**配置文件**：
```yaml
ss-rpc:
  server:
    port: 8080
  registry:
    type: memory
  serialization:
    type: json
  loadbalance:
    type: random
```

## 📋 开发规范

### 编码规范
- 遵循Google Java Style
- 所有公共API必须有JavaDoc
- 测试覆盖率 ≥ 80%
- 通过CheckStyle检查

### 提交规范
- 使用约定式提交格式
- 每个功能一个分支
- 代码审查后合并

### 分支策略
```
main
├── develop
└── phase1-mvp (当前分支)
    ├── feature/network-communication
    ├── feature/serialization-protocol  
    ├── feature/service-registry
    └── feature/spring-boot-integration
```

## 🧪 测试计划

### 单元测试
- 每个核心类都有对应的单元测试
- Mock外部依赖
- 覆盖正常和异常情况

### 集成测试
- 端到端RPC调用测试
- 多服务实例负载均衡测试
- 网络异常处理测试

### 性能测试
- 基准性能测试
- 并发压力测试
- 内存泄漏检测

## 📦 发布计划

### MVP v0.1.0 发布内容
- 核心RPC功能
- 基础序列化支持
- 内存注册中心
- Spring Boot Starter
- 使用示例和文档

### 验收标准
- [ ] 所有单元测试通过
- [ ] 集成测试通过
- [ ] 性能基准达标
- [ ] 代码覆盖率 ≥ 80%
- [ ] 文档完整
- [ ] 示例可运行

## 🔄 下一步行动

1. **立即开始**: Week 1 网络通信模块开发
2. **创建功能分支**: `feature/network-communication`
3. **实现顺序**: 
   - NettyServer/NettyClient
   - RpcRequest/RpcResponse  
   - RpcInvoker/ProxyFactory
   - 单元测试

让我们开始编码吧！🚀 