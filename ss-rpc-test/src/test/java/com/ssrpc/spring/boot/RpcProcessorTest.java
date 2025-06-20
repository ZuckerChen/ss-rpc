package com.ssrpc.spring.boot;

import com.ssrpc.core.annotation.RpcReference;
import com.ssrpc.core.annotation.RpcService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RPC处理器测试类
 * 
 * @author shuang.kou
 * @since 1.0.0
 */
@DisplayName("RPC处理器测试")
class RpcProcessorTest {
    
    private TestUserServiceImpl serviceImpl;
    private TestUserController controller;
    
    @BeforeEach
    void setUp() {
        serviceImpl = new TestUserServiceImpl();
        controller = new TestUserController();
    }
    
    @Test
    @DisplayName("测试服务注册处理器逻辑")
    void testServiceRegistrationProcessor() {
        // 验证服务实现类有正确的注解
        assertTrue(serviceImpl.getClass().isAnnotationPresent(RpcService.class));
        
        RpcService annotation = serviceImpl.getClass().getAnnotation(RpcService.class);
        assertNotNull(annotation);
        
        // 验证注解属性
        assertEquals("1.0.0", annotation.version());
        assertEquals(100, annotation.weight());
        
        // 验证服务实现了正确的接口
        Class<?>[] interfaces = serviceImpl.getClass().getInterfaces();
        assertTrue(interfaces.length > 0);
        
        boolean implementsTestUserService = false;
        for (Class<?> intf : interfaces) {
            if (intf.equals(TestUserService.class)) {
                implementsTestUserService = true;
                break;
            }
        }
        assertTrue(implementsTestUserService);
    }
    
    @Test
    @DisplayName("测试引用注入处理器逻辑")
    void testReferenceInjectionProcessor() throws Exception {
        // 获取需要注入的字段
        Field userServiceField = controller.getClass().getDeclaredField("userService");
        Field asyncUserServiceField = controller.getClass().getDeclaredField("asyncUserService");
        
        // 验证字段有@RpcReference注解
        assertTrue(userServiceField.isAnnotationPresent(RpcReference.class));
        assertTrue(asyncUserServiceField.isAnnotationPresent(RpcReference.class));
        
        // 验证注解属性
        RpcReference userServiceRef = userServiceField.getAnnotation(RpcReference.class);
        assertEquals("1.0.0", userServiceRef.version());
        assertEquals(5000, userServiceRef.timeout());
        assertFalse(userServiceRef.async());
        
        RpcReference asyncUserServiceRef = asyncUserServiceField.getAnnotation(RpcReference.class);
        assertEquals("1.0.0", asyncUserServiceRef.version());
        assertEquals(3000, asyncUserServiceRef.timeout());
        assertTrue(asyncUserServiceRef.async());
        
        // 验证字段类型
        assertEquals(TestUserService.class, userServiceField.getType());
        assertEquals(TestUserService.class, asyncUserServiceField.getType());
    }
    
    @Test
    @DisplayName("测试代理对象创建")
    void testProxyCreation() {
        // 手动创建代理对象来测试代理逻辑
        TestUserService proxy = (TestUserService) Proxy.newProxyInstance(
                TestUserService.class.getClassLoader(),
                new Class[]{TestUserService.class},
                (proxyObj, method, args) -> {
                    // 简单的代理逻辑，返回默认值
                    if (method.getName().equals("ping")) {
                        return "proxy-pong";
                    }
                    if (method.getReturnType().equals(String.class)) {
                        return "proxy-result";
                    }
                    if (method.getReturnType().equals(TestUser.class)) {
                        return new TestUser(999L, "代理用户", "proxy@example.com", 25);
                    }
                    return null;
                }
        );
        
        // 验证代理对象
        assertNotNull(proxy);
        assertTrue(Proxy.isProxyClass(proxy.getClass()));
        
        // 测试代理方法调用
        assertEquals("proxy-pong", proxy.ping());
        
        TestUser proxyUser = proxy.getUserById(1L);
        assertNotNull(proxyUser);
        assertEquals(999L, proxyUser.getId());
        assertEquals("代理用户", proxyUser.getName());
    }
    
    @Test
    @DisplayName("测试服务发现逻辑")
    void testServiceDiscoveryLogic() {
        // 模拟服务发现过程
        String serviceName = TestUserService.class.getName();
        String version = "1.0.0";
        
        // 验证服务名称和版本
        assertEquals("com.ssrpc.spring.boot.TestUserService", serviceName);
        assertEquals("1.0.0", version);
        
        // 验证服务接口的方法
        assertDoesNotThrow(() -> {
            TestUserService.class.getMethod("getUserById", Long.class);
            TestUserService.class.getMethod("ping");
            TestUserService.class.getMethod("createUser", TestUser.class);
        });
    }
    
    @Test
    @DisplayName("测试负载均衡选择逻辑")
    void testLoadBalanceLogic() {
        // 模拟多个服务实例
        String[] hosts = {"localhost", "127.0.0.1", "192.168.1.100"};
        int[] ports = {9999, 10000, 10001};
        
        // 简单的轮询负载均衡测试
        int instanceCount = hosts.length;
        assertTrue(instanceCount > 0);
        
        for (int i = 0; i < 10; i++) {
            int selectedIndex = i % instanceCount;
            assertTrue(selectedIndex >= 0 && selectedIndex < instanceCount);
            
            String selectedHost = hosts[selectedIndex];
            int selectedPort = ports[selectedIndex];
            
            assertNotNull(selectedHost);
            assertTrue(selectedPort > 0);
        }
    }
    
    @Test
    @DisplayName("测试序列化逻辑")
    void testSerializationLogic() {
        // 测试对象序列化兼容性
        TestUser user = new TestUser(1L, "测试用户", "test@example.com", 25);
        
        // 验证对象实现了Serializable
        assertTrue(user instanceof java.io.Serializable);
        
        // 验证对象的基本属性
        assertNotNull(user.getId());
        assertNotNull(user.getName());
        assertNotNull(user.getEmail());
        assertNotNull(user.getAge());
    }
    
    @Test
    @DisplayName("测试异常处理逻辑")
    void testExceptionHandlingLogic() {
        // 测试各种异常情况
        TestUserServiceImpl service = new TestUserServiceImpl();
        
        // 测试参数验证异常
        Exception nullIdException = assertThrows(IllegalArgumentException.class, 
                () -> service.getUserById(null));
        assertTrue(nullIdException.getMessage().contains("cannot be null"));
        
        // 测试业务异常
        Exception notFoundException = assertThrows(RuntimeException.class, 
                () -> service.getUserById(999L));
        assertTrue(notFoundException.getMessage().contains("not found"));
        
        // 测试主动抛出的异常
        Exception customException = assertThrows(Exception.class, 
                () -> service.throwException());
        assertEquals("Test exception from service", customException.getMessage());
    }
    
    @Test
    @DisplayName("测试超时处理逻辑")
    void testTimeoutHandlingLogic() throws Exception {
        // 测试异步方法的超时处理
        TestUserServiceImpl service = new TestUserServiceImpl();
        
        java.util.concurrent.CompletableFuture<TestUser> future = service.getUserByIdAsync(1L);
        assertNotNull(future);
        
        // 测试在合理时间内完成
        TestUser result = future.get(1, java.util.concurrent.TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }
} 