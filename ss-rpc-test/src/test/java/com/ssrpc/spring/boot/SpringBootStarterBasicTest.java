package com.ssrpc.spring.boot;

import com.ssrpc.spring.boot.autoconfigure.RpcProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spring Boot Starter基础测试
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
@DisplayName("Spring Boot Starter基础测试")
class SpringBootStarterBasicTest {
    
    @Autowired
    private RpcProperties rpcProperties;
    
    @Autowired
    private TestUserController testUserController;
    
    @Autowired
    private TestUserServiceImpl testUserService;
    
    @Test
    @DisplayName("测试Spring Boot自动配置属性")
    void testAutoConfigurationProperties() {
        // 验证配置属性注入
        assertNotNull(rpcProperties);
        assertTrue(rpcProperties.isEnabled());
        assertEquals(9998, rpcProperties.getServer().getPort());
        assertEquals("localhost", rpcProperties.getServer().getHost());
        assertEquals("memory", rpcProperties.getRegistry().getType());
        assertEquals("json", rpcProperties.getSerialization().getType());
        assertEquals("round_robin", rpcProperties.getLoadBalance().getAlgorithm());
    }
    
    @Test
    @DisplayName("测试Bean自动创建")
    void testBeanAutoCreation() {
        // 验证服务实现类被正确创建
        assertNotNull(testUserService);
        
        // 验证控制器被正确创建
        assertNotNull(testUserController);
    }
    
    @Test
    @DisplayName("测试RPC引用注入")
    void testReferenceInjection() {
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
    @DisplayName("测试配置属性详细绑定")
    void testDetailedConfigurationBinding() {
        // 验证服务端配置
        RpcProperties.Server serverConfig = rpcProperties.getServer();
        assertNotNull(serverConfig);
        assertEquals(9998, serverConfig.getPort());
        assertEquals("localhost", serverConfig.getHost());
        assertEquals(5000, serverConfig.getConnectTimeout());
        assertTrue(serverConfig.getWorkerThreads() > 0);
        assertEquals(1, serverConfig.getBossThreads());
        
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
        assertEquals("", registryConfig.getAddress());
        assertEquals(5000, registryConfig.getConnectTimeout());
        assertEquals(30000, registryConfig.getSessionTimeout());
        
        // 验证序列化配置
        RpcProperties.Serialization serializationConfig = rpcProperties.getSerialization();
        assertNotNull(serializationConfig);
        assertEquals("json", serializationConfig.getType());
        
        // 验证负载均衡配置
        RpcProperties.LoadBalance loadBalanceConfig = rpcProperties.getLoadBalance();
        assertNotNull(loadBalanceConfig);
        assertEquals("round_robin", loadBalanceConfig.getAlgorithm());
    }
    
    @Test
    @DisplayName("测试RPC方法调用不报错")
    void testRpcMethodCallsNoException() {
        TestUserController controller = testUserController;
        
        // 测试基本方法调用不抛异常
        assertDoesNotThrow(() -> {
            String result = controller.ping();
            // 模拟响应，实际网络传输实现后会返回真实结果
        });
        
        assertDoesNotThrow(() -> {
            TestUser user = controller.getUser(1L);
            // 模拟响应，实际网络传输实现后会返回真实结果
        });
        
        assertDoesNotThrow(() -> {
            controller.getAllUsers();
            // 模拟响应，实际网络传输实现后会返回真实结果
        });
        
        assertDoesNotThrow(() -> {
            TestUser newUser = new TestUser(null, "测试用户", "test@example.com", 25);
            controller.createUser(newUser);
            // 模拟响应，实际网络传输实现后会返回真实结果
        });
    }
    
    @Test
    @DisplayName("测试异步方法调用")
    void testAsyncMethodCall() {
        TestUserController controller = testUserController;
        
        assertDoesNotThrow(() -> {
            // 测试异步方法调用
            controller.getUserAsync(1L);
            // 返回CompletableFuture，实际网络传输实现后会有真实的异步处理
        });
    }
} 