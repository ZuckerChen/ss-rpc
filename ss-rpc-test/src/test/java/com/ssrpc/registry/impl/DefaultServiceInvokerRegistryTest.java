package com.ssrpc.registry.impl;

import com.ssrpc.protocol.RpcInvoker;
import com.ssrpc.protocol.RpcRequest;
import com.ssrpc.protocol.RpcResponse;
import com.ssrpc.registry.impl.DefaultServiceInvokerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultServiceInvokerRegistry 单元测试.
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@DisplayName("默认服务调用器注册测试")
class DefaultServiceInvokerRegistryTest {
    
    private DefaultServiceInvokerRegistry registry;
    private RpcInvoker testInvoker;
    
    @BeforeEach
    void setUp() {
        registry = new DefaultServiceInvokerRegistry();
        testInvoker = createTestInvoker();
    }
    
    @Test
    @DisplayName("测试注册服务调用器成功")
    void testRegisterInvokerSuccess() {
        assertDoesNotThrow(() -> registry.registerInvoker("test-service", "1.0.0", testInvoker));
        
        // 验证注册成功
        assertTrue(registry.containsService("test-service", "1.0.0"));
        assertEquals(testInvoker, registry.getInvoker("test-service", "1.0.0"));
        assertEquals(1, registry.getServiceCount());
    }
    
    @Test
    @DisplayName("测试重复注册服务调用器")
    void testRegisterInvokerDuplicate() {
        registry.registerInvoker("test-service", "1.0.0", testInvoker);
        
        RpcInvoker newInvoker = createTestInvoker();
        
        // 重复注册应该覆盖原有的
        assertDoesNotThrow(() -> registry.registerInvoker("test-service", "1.0.0", newInvoker));
        
        assertEquals(newInvoker, registry.getInvoker("test-service", "1.0.0"));
        assertEquals(1, registry.getServiceCount());
    }
    
    @Test
    @DisplayName("测试注册服务调用器时传入空参数失败")
    void testRegisterInvokerWithNullParameters() {
        // 空服务名
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> registry.registerInvoker(null, "1.0.0", testInvoker));
        assertTrue(exception.getMessage().contains("Service name cannot be null or empty"));
        
        // 空字符串服务名
        exception = assertThrows(IllegalArgumentException.class,
            () -> registry.registerInvoker("", "1.0.0", testInvoker));
        assertTrue(exception.getMessage().contains("Service name cannot be null or empty"));
        
        // 空版本
        exception = assertThrows(IllegalArgumentException.class,
            () -> registry.registerInvoker("test-service", null, testInvoker));
        assertTrue(exception.getMessage().contains("Service version cannot be null or empty"));
        
        // 空字符串版本
        exception = assertThrows(IllegalArgumentException.class,
            () -> registry.registerInvoker("test-service", "", testInvoker));
        assertTrue(exception.getMessage().contains("Service version cannot be null or empty"));
        
        // 空调用器
        exception = assertThrows(IllegalArgumentException.class,
            () -> registry.registerInvoker("test-service", "1.0.0", null));
        assertTrue(exception.getMessage().contains("Service invoker cannot be null"));
    }
    
    @Test
    @DisplayName("测试获取服务调用器")
    void testGetInvoker() {
        registry.registerInvoker("test-service", "1.0.0", testInvoker);
        
        RpcInvoker retrieved = registry.getInvoker("test-service", "1.0.0");
        assertNotNull(retrieved);
        assertEquals(testInvoker, retrieved);
    }
    
    @Test
    @DisplayName("测试获取不存在的服务调用器")
    void testGetNonExistentInvoker() {
        RpcInvoker retrieved = registry.getInvoker("non-existent-service", "1.0.0");
        assertNull(retrieved);
    }
    
    @Test
    @DisplayName("测试移除服务调用器")
    void testRemoveInvoker() {
        registry.registerInvoker("test-service", "1.0.0", testInvoker);
        
        RpcInvoker removed = registry.removeInvoker("test-service", "1.0.0");
        
        assertNotNull(removed);
        assertEquals(testInvoker, removed);
        assertFalse(registry.containsService("test-service", "1.0.0"));
        assertEquals(0, registry.getServiceCount());
    }
    
    @Test
    @DisplayName("测试移除不存在的服务调用器")
    void testRemoveNonExistentInvoker() {
        RpcInvoker removed = registry.removeInvoker("non-existent-service", "1.0.0");
        assertNull(removed);
    }
    
    @Test
    @DisplayName("测试检查服务是否存在")
    void testContainsService() {
        // 未注册时应该返回false
        assertFalse(registry.containsService("test-service", "1.0.0"));
        
        // 注册后应该返回true
        registry.registerInvoker("test-service", "1.0.0", testInvoker);
        assertTrue(registry.containsService("test-service", "1.0.0"));
        
        // 移除后应该返回false
        registry.removeInvoker("test-service", "1.0.0");
        assertFalse(registry.containsService("test-service", "1.0.0"));
    }
    
    @Test
    @DisplayName("测试获取服务数量")
    void testGetServiceCount() {
        assertEquals(0, registry.getServiceCount());
        
        registry.registerInvoker("service1", "1.0.0", testInvoker);
        assertEquals(1, registry.getServiceCount());
        
        registry.registerInvoker("service2", "1.0.0", testInvoker);
        assertEquals(2, registry.getServiceCount());
        
        registry.registerInvoker("service1", "2.0.0", testInvoker);
        assertEquals(3, registry.getServiceCount());
        
        registry.removeInvoker("service1", "1.0.0");
        assertEquals(2, registry.getServiceCount());
    }
    
    @Test
    @DisplayName("测试清空所有服务")
    void testClear() {
        registry.registerInvoker("service1", "1.0.0", testInvoker);
        registry.registerInvoker("service2", "1.0.0", testInvoker);
        registry.registerInvoker("service3", "1.0.0", testInvoker);
        
        assertEquals(3, registry.getServiceCount());
        
        registry.clear();
        
        assertEquals(0, registry.getServiceCount());
        assertFalse(registry.containsService("service1", "1.0.0"));
        assertFalse(registry.containsService("service2", "1.0.0"));
        assertFalse(registry.containsService("service3", "1.0.0"));
    }
    
    @Test
    @DisplayName("测试获取所有服务键")
    void testGetAllServiceKeys() {
        registry.registerInvoker("service1", "1.0.0", testInvoker);
        registry.registerInvoker("service2", "1.0.0", testInvoker);
        registry.registerInvoker("service1", "2.0.0", testInvoker);
        
        java.util.Set<String> serviceKeys = registry.getAllServiceKeys();
        
        assertEquals(3, serviceKeys.size());
        assertTrue(serviceKeys.contains("service1:1.0.0"));
        assertTrue(serviceKeys.contains("service2:1.0.0"));
        assertTrue(serviceKeys.contains("service1:2.0.0"));
    }
    
    @Test
    @DisplayName("测试多版本服务管理")
    void testMultiVersionServices() {
        RpcInvoker invoker1_0 = createTestInvoker();
        RpcInvoker invoker1_1 = createTestInvoker();
        RpcInvoker invoker2_0 = createTestInvoker();
        
        // 注册同一服务的不同版本
        registry.registerInvoker("api-service", "1.0.0", invoker1_0);
        registry.registerInvoker("api-service", "1.1.0", invoker1_1);
        registry.registerInvoker("api-service", "2.0.0", invoker2_0);
        
        assertEquals(3, registry.getServiceCount());
        
        // 验证每个版本都能正确获取
        assertEquals(invoker1_0, registry.getInvoker("api-service", "1.0.0"));
        assertEquals(invoker1_1, registry.getInvoker("api-service", "1.1.0"));
        assertEquals(invoker2_0, registry.getInvoker("api-service", "2.0.0"));
        
        // 验证版本隔离
        assertTrue(registry.containsService("api-service", "1.0.0"));
        assertTrue(registry.containsService("api-service", "1.1.0"));
        assertTrue(registry.containsService("api-service", "2.0.0"));
        assertFalse(registry.containsService("api-service", "3.0.0"));
    }
    
    @Test
    @DisplayName("测试toString方法")
    void testToString() {
        registry.registerInvoker("test-service", "1.0.0", testInvoker);
        
        String str = registry.toString();
        assertNotNull(str);
        assertTrue(str.contains("DefaultServiceInvokerRegistry"));
        assertTrue(str.contains("serviceCount=1"));
        assertTrue(str.contains("test-service:1.0.0"));
    }
    
    @Test
    @DisplayName("测试构建服务键时传入空参数")
    void testBuildServiceKeyWithNullParameters() {
        // 通过注册方法间接测试buildServiceKey方法的参数验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> registry.registerInvoker(null, "1.0.0", testInvoker));
        assertTrue(exception.getMessage().contains("Service name and version cannot be null") ||
                  exception.getMessage().contains("Service name cannot be null or empty"));
    }
    
    @Test
    @DisplayName("测试并发操作安全性")
    void testConcurrentOperations() throws InterruptedException {
        final int threadCount = 10;
        final int operationsPerThread = 100;
        Thread[] threads = new Thread[threadCount];
        
        // 创建多个线程并发操作
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String serviceName = "service-" + threadIndex;
                    String version = "1.0." + j;
                    RpcInvoker invoker = createTestInvoker();
                    
                    // 注册
                    registry.registerInvoker(serviceName, version, invoker);
                    
                    // 检查存在性
                    assertTrue(registry.containsService(serviceName, version));
                    
                    // 获取
                    assertEquals(invoker, registry.getInvoker(serviceName, version));
                    
                    // 移除
                    assertEquals(invoker, registry.removeInvoker(serviceName, version));
                    
                    // 验证已移除
                    assertFalse(registry.containsService(serviceName, version));
                }
            });
        }
        
        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证最终状态
        assertEquals(0, registry.getServiceCount());
    }
    
    /**
     * 创建测试用的RpcInvoker
     */
    private RpcInvoker createTestInvoker() {
        return new RpcInvoker() {
            @Override
            public RpcResponse invoke(RpcRequest request) throws Exception {
                RpcResponse response = new RpcResponse();
                response.setResult("test-result");
                return response;
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<RpcResponse> invokeAsync(RpcRequest request) {
                return java.util.concurrent.CompletableFuture.completedFuture(
                    new RpcResponse() {{ setResult("test-result"); }}
                );
            }
            
            @Override
            public boolean isAvailable() {
                return true;
            }
            
            @Override
            public String getTargetAddress() {
                return "127.0.0.1:8080";
            }
            
            @Override
            public void destroy() {
                // 测试实现，无需实际销毁
            }
        };
    }
} 