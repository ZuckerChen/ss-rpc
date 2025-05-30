# SS-RPC 快速入门指南

## 项目介绍

SS-RPC（Simple & Smart RPC）是一个基于Java开发的轻量级、高性能RPC框架。它采用现代化的架构设计，提供简单易用的API，同时具备企业级的功能特性。

### 核心优势

🚀 **高性能** - 基于Netty实现，支持万级QPS  
🔧 **易使用** - 注解驱动，零配置启动  
🌐 **多协议** - 支持JSON、Protobuf、Kryo等序列化  
🏗️ **可扩展** - 插件化架构，组件可替换  
📊 **可监控** - 内置监控统计和健康检查  
🔒 **高可用** - 服务发现、负载均衡、容错机制  

## 快速开始

### 环境要求

- Java 8+
- Maven 3.6+
- ZooKeeper 3.6+（可选）

### 添加依赖

```xml
<dependency>
    <groupId>com.ssrpc</groupId>
    <artifactId>ss-rpc-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 定义服务接口

```java
public interface UserService {
    User getUserById(Long id);
    boolean updateUser(User user);
}

@Data
public class User {
    private Long id;
    private String name;
    private String email;
}
```

## 下一步

- 查看[完整需求文档](./requirements.md)了解详细功能
- 查看[架构设计文档](./architecture.md)了解技术实现
- 查看[开发路线图](./roadmap.md)了解项目规划

## 社区支持

- GitHub: https://github.com/your-username/ss-rpc
- 问题反馈: https://github.com/your-username/ss-rpc/issues

欢迎加入SS-RPC社区，一起打造更好的RPC框架！ 