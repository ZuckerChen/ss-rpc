# SS-RPC Bootstrap模块

## 概述

SS-RPC Bootstrap模块是整个RPC框架的启动引导模块，负责框架的初始化、启动和关闭流程。

## 架构作用

按照Dubbo等成熟RPC框架的分层架构设计，Bootstrap模块扮演以下角色：

1. **组装者角色**：负责组装各个模块（core、transport、registry等）
2. **生命周期管理**：统一管理框架的启动和关闭
3. **依赖隔离**：解决core和transport之间的循环依赖问题

## 架构改进

### 改进前的问题
```
┌─────────┐    ┌───────────┐
│  Core   │◄──►│Transport  │  ← 循环依赖
└─────────┘    └───────────┘
```

### 改进后的架构
```
┌─────────────┐
│ Bootstrap   │  ← 组装层，依赖所有模块
├─────────────┤
│   ┌─────┐   │
│   │Core │   │  ← 核心接口层
│   └─────┘   │
│      ▲      │
│   ┌─────┐   │
│   │Trans│   │  ← 传输实现层
│   └─────┘   │
└─────────────┘
```

## 主要功能

### RpcBootstrap类

- **单例模式**：全局唯一的框架实例
- **流式配置**：支持链式调用配置
- **生命周期管理**：统一的启动/关闭流程
- **异常处理**：完善的错误处理和资源清理

### 使用示例

```java
// 基础使用
RpcBootstrap.create()
    .applicationName("my-rpc-app")
    .serverPort(8080)
    .enableServer()
    .enableClient()
    .start();

// 自定义配置
RpcConfiguration config = new RpcConfiguration();
config.setApplicationName("my-app");
config.setServerPort(9090);

RpcBootstrap.create(config)
    .devMode()
    .start();
```

## 依赖关系

Bootstrap模块依赖以下模块：
- `ss-rpc-core`：核心接口和抽象
- `ss-rpc-transport`：网络传输实现
- `ss-rpc-registry`：服务注册发现
- `ss-rpc-serialization`：序列化实现
- `ss-rpc-loadbalance`：负载均衡

## 启动流程

1. **配置验证**：验证配置参数的有效性
2. **组件初始化**：初始化各个核心组件
3. **服务端启动**：启动RPC服务端（如果启用）
4. **客户端启动**：启动RPC客户端（如果启用）
5. **服务注册**：注册本地服务实例到注册中心
6. **钩子注册**：注册JVM关闭钩子
7. **状态标记**：标记框架为已启动状态

## 设计模式

- **单例模式**：确保框架实例唯一性
- **建造者模式**：提供流式配置API
- **模板方法**：标准化启动流程
- **观察者模式**：支持生命周期事件监听

## 注意事项

1. **启动顺序**：必须先配置再启动
2. **重复启动**：框架会检查重复启动并给出警告
3. **资源清理**：关闭时会自动清理所有资源
4. **异常处理**：启动失败会自动回滚和清理 