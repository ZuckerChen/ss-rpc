package com.ssrpc;

/**
 * SS-RPC 全部测试套件
 * 
 * 包含所有模块的测试用例
 * 
 * 测试类列表：
 * - com.ssrpc.core.spi.SpiDemoTest - SPI机制测试
 * - com.ssrpc.transport.codec.spi.SerializerTest - 序列化器测试
 * - com.ssrpc.transport.codec.RpcCodecTest - RPC编解码器测试
 * - com.ssrpc.transport.codec.RpcCodecIntegrationTest - RPC编解码器集成测试
 * - com.ssrpc.registry.memory.MemoryServiceRegistryTest - 内存服务注册测试
 * - com.ssrpc.registry.memory.MemoryServiceDiscoveryTest - 内存服务发现测试
 * - com.ssrpc.registry.impl.DefaultServiceInvokerRegistryTest - 默认服务调用器注册测试
 * - com.ssrpc.registry.factory.RegistryFactoryTest - 注册中心工厂测试
 * - com.ssrpc.registry.factory.RegistryManagerTest - 注册中心管理器测试
 * - com.ssrpc.registry.RegistryIntegrationTest - Registry模块集成测试
 *
 * @author chenzhang
 * @since 1.0.0
 */
public class AllTestSuite {
    
    /**
     * 测试套件说明
     * 
     * 本类用于组织和说明项目中的所有测试用例。
     * 可以通过IDE或Maven命令运行所有测试：
     * 
     * Maven命令：mvn test
     * IDE：右键点击test目录 -> Run All Tests
     */
    private AllTestSuite() {
        // 工具类，不允许实例化
    }
} 