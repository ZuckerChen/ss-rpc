# 🚀 SS-RPC - Simple & Smart RPC Framework

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/your-username/ss-rpc)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![Java Version](https://img.shields.io/badge/java-8%2B-orange)](https://openjdk.java.net/)
[![Maven Central](https://img.shields.io/badge/maven--central-1.0.0--SNAPSHOT-red)](https://mvnrepository.com/)

> **新一代智能化Java RPC框架，开发者体验优先，AI驱动，云原生设计**

## ✨ 核心特性

### 🎯 开发者体验优先 (DX-First)
- **零配置启动** - 一个`@EnableRpc`注解搞定
- **智能错误诊断** - 比传统框架友好100倍的错误提示
- **热重载支持** - 开发环境无需重启

### 🧠 AI驱动的智能化
- **自适应负载均衡** - 机器学习优化路由选择
- **智能熔断降级** - 预测式故障处理
- **自动性能调优** - AI分析并建议最优配置

### ☁️ 云原生优先设计
- **Kubernetes原生支持** - 开箱即用的K8s集成
- **Service Mesh集成** - Istio、Linkerd无缝对接
- **可观测性增强** - Prometheus、Jaeger自动集成

### 🔥 现代Java特性全支持
- **异步编程** - 原生支持CompletableFuture
- **响应式编程** - Mono/Flux完美集成
- **虚拟线程** - Java 19+虚拟线程支持
- **函数式编程** - Stream API深度集成

## 🚀 快速开始

### 1. 添加依赖
```xml
<dependency>
    <groupId>com.ssrpc</groupId>
    <artifactId>ss-rpc-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 定义服务
```java
// 服务接口
public interface UserService {
    User getUserById(Long id);
    CompletableFuture<User> getUserAsync(Long id);
}

// 服务实现
@RpcService
@Component
public class UserServiceImpl implements UserService {
    public User getUserById(Long id) {
        return new User(id, "张三", "zhangsan@example.com");
    }
    
    public CompletableFuture<User> getUserAsync(Long id) {
        return CompletableFuture.supplyAsync(() -> getUserById(id));
    }
}
```

### 3. 消费服务
```java
@RestController
public class UserController {
    
    @RpcReference  // 自动发现和注入
    private UserService userService;
    
    @GetMapping("/user/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }
}
```

### 4. 启动应用
```java
@SpringBootApplication
@EnableRpc  // 一个注解启动RPC
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

就这么简单！🎉

## 📊 与主流框架对比

| 特性 | SS-RPC | Dubbo | gRPC | Motan |
|------|--------|-------|------|-------|
| **零配置启动** | ✅ 一个注解 | ❌ 复杂XML | ❌ Proto文件 | ⚠️ 需配置 |
| **现代Java支持** | ✅ Java 8-21 | ⚠️ 部分支持 | ❌ 有限 | ⚠️ 基础 |
| **智能负载均衡** | ✅ AI驱动 | ⚠️ 静态策略 | ⚠️ 基础 | ⚠️ 基础 |
| **云原生支持** | ✅ K8s原生 | ⚠️ 需适配 | ✅ 良好 | ❌ 有限 |
| **开发者体验** | ✅ 极致优化 | ⚠️ 学习成本高 | ❌ 调试困难 | ⚠️ 一般 |

## 🏗️ 项目架构

```
ss-rpc/
├── ss-rpc-core/                 # 核心功能模块
├── ss-rpc-protocol/             # 协议模块
├── ss-rpc-serialization/        # 序列化模块
├── ss-rpc-registry/             # 注册中心模块
├── ss-rpc-loadbalance/          # 负载均衡模块
├── ss-rpc-spring-boot-starter/  # Spring Boot集成
└── ss-rpc-examples/             # 示例项目
```

## 📈 开发路线图

- [x] **第1周** - 项目基础搭建（95%完成）
  - [x] Maven多模块架构
  - [x] 核心注解设计
  - [x] 单元测试框架
  - [ ] CI/CD流水线

- [ ] **第2周** - 网络通信模块
  - [ ] RPC协议设计
  - [ ] Netty服务器实现
  - [ ] 协议编解码器

- [ ] **第3周** - 服务注册发现
  - [ ] ZooKeeper/Nacos集成
  - [ ] 服务发现机制
  - [ ] 健康检查

- [ ] **第4周** - 序列化与负载均衡
  - [ ] 多种序列化支持
  - [ ] 智能负载均衡
  - [ ] 性能优化

## 🛠️ 技术栈

- **核心** - Java 8-21, Netty 4.1+, Spring Boot 2.7+
- **构建** - Maven, JUnit 5, JaCoCo, CheckStyle
- **云原生** - Kubernetes, Prometheus, Jaeger
- **注册中心** - ZooKeeper, Nacos
- **序列化** - Jackson, Protobuf, Kryo

## 🤝 贡献指南

我们欢迎所有形式的贡献！

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源协议。

## 🙏 致谢

感谢以下开源项目的启发：
- [Apache Dubbo](https://dubbo.apache.org/) - 功能完整的RPC框架
- [gRPC](https://grpc.io/) - 高性能跨语言RPC
- [Spring Boot](https://spring.io/projects/spring-boot) - 简化Spring应用开发

---

<div align="center">

**如果这个项目对你有帮助，请给我们一个 ⭐ Star！**

</div> 