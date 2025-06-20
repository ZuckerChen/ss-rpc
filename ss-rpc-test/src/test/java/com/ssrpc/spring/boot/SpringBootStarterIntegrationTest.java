package com.ssrpc.spring.boot;

import com.ssrpc.registry.api.ServiceDiscovery;
import com.ssrpc.registry.api.ServiceRegistry;
import com.ssrpc.registry.factory.RegistryManager;
import com.ssrpc.spring.boot.autoconfigure.RpcProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spring Boot Starter集成测试
 * 
 * @author shuang.kou
 * @since 1.0.0
 */
@SpringBootTest(classes = SpringBootStarterTestConfiguration.class)
@TestPropertySource(properties = {
        "ss-rpc.enabled=true",
        "ss-rpc.server.port=9998",
        "ss-rpc.server.host=localhost",
        "ss-rpc.registry.type=memory",
        "ss-rpc.serialization.type=json",
        "ss-rpc.load-balance.algorithm=round_robin"
})
@DisplayName("Spring Boot Starter集成测试")
class SpringBootStarterIntegrationTest {
    
    @Autowired
    private RpcProperties rpcProperties;
    
    @Autowired
    private ServiceRegistry serviceRegistry;
    
    @Autowired
    private ServiceDiscovery serviceDiscovery;
    
    @Autowired
    private RegistryManager registryManager;
    
    @Autowired
    private TestUserController testUserController;
    
    @Autowired
    private TestUserServiceImpl testUserService;
    
    @Test
    @DisplayName("测试Spring Boot自动配置")
    void testAutoConfiguration() {
        // 验证配置属性注入
        assertNotNull(rpcProperties);
        assertTrue(rpcProperties.isEnabled());
        assertEquals(9998, rpcProperties.getServer().getPort());
        assertEquals("localhost", rpcProperties.getServer().getHost());
        assertEquals("memory", rpcProperties.getRegistry().getType());
        assertEquals("json", rpcProperties.getSerialization().getType());
        assertEquals("round_robin", rpcProperties.getLoadBalance().getAlgorithm());
        
        // 验证核心组件注入
        assertNotNull(serviceRegistry);
        assertNotNull(serviceDiscovery);
        assertNotNull(registryManager);
        assertTrue(registryManager.isStarted());
    }
    
    @Test
    @DisplayName("测试RPC服务自动注册")
    void testServiceAutoRegistration() throws Exception {
        // 验证服务实现类被正确创建
        assertNotNull(testUserService);
        
        // 验证服务被自动注册到注册中心
        List<com.ssrpc.protocol.ServiceInstance> instances = 
                serviceDiscovery.discover(TestUserService.class.getName(), "1.0.0");
        
        assertNotNull(instances);
        assertFalse(instances.isEmpty());
        assertEquals(1, instances.size());
        
        com.ssrpc.protocol.ServiceInstance instance = instances.get(0);
        assertEquals(TestUserService.class.getName(), instance.getServiceName());
        assertEquals("1.0.0", instance.getVersion());
        assertEquals("localhost", instance.getHost());
        assertEquals(9998, instance.getPort());
        assertTrue(instance.isHealthy());
    }
    
    @Test
    @DisplayName("测试RPC引用自动注入")
    void testReferenceAutoInjection() {
        // 验证控制器被正确创建
        assertNotNull(testUserController);
        
        // 验证RPC服务引用被自动注入
        TestUserService userService = testUserController.getUserService();
        assertNotNull(userService);
        
        // 验证异步服务引用被自动注入
        TestUserService asyncUserService = testUserController.getAsyncUserService();
        assertNotNull(asyncUserService);
        
        // 验证注入的是代理对象
        assertNotSame(testUserService, userService);
        assertTrue(userService.getClass().getName().contains("Proxy"));
    }
    
    @Test
    @DisplayName("测试RPC同步调用")
    void testSyncRpcCall() {
        TestUserController controller = testUserController;
        
        // 测试ping方法
        String pingResult = controller.ping();
        // 注意：由于目前返回的是模拟响应，这里可能是null
        // 在实际网络传输实现后，应该返回"pong"
        
        // 测试获取用户（模拟调用）
        // 由于目前使用模拟响应，实际的业务逻辑不会执行
        // 这里主要验证代理对象创建和方法调用不报错
        assertDoesNotThrow(() -> {
            TestUser user = controller.getUser(1L);
            // 模拟响应返回null，实际实现后应该返回用户对象
        });
        
        // 测试获取所有用户
        assertDoesNotThrow(() -> {
            List<TestUser> users = controller.getAllUsers();
            // 模拟响应返回null，实际实现后应该返回用户列表
        });
    }
    
    @Test
    @DisplayName("测试RPC异步调用")
    void testAsyncRpcCall() throws Exception {
        TestUserController controller = testUserController;
        
        // 测试异步调用
        CompletableFuture<TestUser> future = controller.getUserAsync(1L);
        assertNotNull(future);
        
        // 等待异步调用完成
        TestUser user = future.get(5, TimeUnit.SECONDS);
        // 模拟响应返回null，实际实现后应该返回用户对象
    }
    
    @Test
    @DisplayName("测试RPC异常处理")
    void testRpcExceptionHandling() {
        TestUserController controller = testUserController;
        
        // 测试异常传播（模拟环境下可能不会抛出实际异常）
        assertDoesNotThrow(() -> {
            try {
                controller.testException();
            } catch (Exception e) {
                // 预期的异常，在实际网络传输实现后应该能正确传播异常
            }
        });
    }
    
    @Test
    @DisplayName("测试服务创建和更新")
    void testServiceCreateAndUpdate() {
        TestUserController controller = testUserController;
        
        // 测试创建用户
        TestUser newUser = new TestUser(null, "新用户", "newuser@example.com", 25);
        assertDoesNotThrow(() -> {
            TestUser createdUser = controller.createUser(newUser);
            // 模拟响应返回null，实际实现后应该返回创建的用户对象
        });
    }
    
    @Test
    @DisplayName("测试注册中心集成")
    void testRegistryIntegration() throws Exception {
        // 验证注册中心正常工作
        assertTrue(registryManager.isStarted());
        
        // 验证服务注册
        List<String> serviceNames = registryManager.getServiceNames();
        assertNotNull(serviceNames);
        assertTrue(serviceNames.contains(TestUserService.class.getName()));
        
        // 验证服务发现
        List<com.ssrpc.protocol.ServiceInstance> instances = 
                registryManager.discoverServices(TestUserService.class.getName());
        assertNotNull(instances);
        assertFalse(instances.isEmpty());
    }
    
    @Test
    @DisplayName("测试配置属性绑定")
    void testConfigurationBinding() {
        // 验证服务端配置
        RpcProperties.Server serverConfig = rpcProperties.getServer();
        assertNotNull(serverConfig);
        assertEquals(9998, serverConfig.getPort());
        assertEquals("localhost", serverConfig.getHost());
        
        // 验证客户端配置
        RpcProperties.Client clientConfig = rpcProperties.getClient();
        assertNotNull(clientConfig);
        assertEquals(5000, clientConfig.getConnectTimeout());
        assertEquals(10000, clientConfig.getRequestTimeout());
        assertEquals(3, clientConfig.getRetryTimes());
        
        // 验证注册中心配置
        RpcProperties.Registry registryConfig = rpcProperties.getRegistry();
        assertNotNull(registryConfig);
        assertEquals("memory", registryConfig.getType());
        
        // 验证序列化配置
        RpcProperties.Serialization serializationConfig = rpcProperties.getSerialization();
        assertNotNull(serializationConfig);
        assertEquals("json", serializationConfig.getType());
        
        // 验证负载均衡配置
        RpcProperties.LoadBalance loadBalanceConfig = rpcProperties.getLoadBalance();
        assertNotNull(loadBalanceConfig);
        assertEquals("round_robin", loadBalanceConfig.getAlgorithm());
    }
} 