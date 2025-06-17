package com.ssrpc.registry.factory;

import com.ssrpc.registry.api.ServiceDiscovery;
import com.ssrpc.registry.api.ServiceRegistry;
import com.ssrpc.registry.factory.RegistryFactory;
import com.ssrpc.registry.impl.memory.MemoryServiceDiscovery;
import com.ssrpc.registry.impl.memory.MemoryServiceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RegistryFactory 单元测试.
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@DisplayName("注册中心工厂测试")
class RegistryFactoryTest {
    
    @BeforeEach
    void setUp() {
        // 每个测试前清空缓存
        RegistryFactory.clearCache();
    }
    
    @AfterEach
    void tearDown() {
        // 每个测试后清空缓存
        RegistryFactory.clearCache();
    }
    
    @Test
    @DisplayName("测试获取默认内存注册中心")
    void testGetDefaultRegistry() {
        ServiceRegistry registry = RegistryFactory.getRegistry(null);
        
        assertNotNull(registry);
        assertTrue(registry instanceof MemoryServiceRegistry);
        assertEquals("memory", registry.getType());
    }
    
    @Test
    @DisplayName("测试获取指定类型的注册中心")
    void testGetRegistryByType() {
        ServiceRegistry registry = RegistryFactory.getRegistry("memory");
        
        assertNotNull(registry);
        assertTrue(registry instanceof MemoryServiceRegistry);
        assertEquals("memory", registry.getType());
    }
    
    @Test
    @DisplayName("测试获取不存在类型的注册中心回退到内存实现")
    void testGetRegistryFallbackToMemory() {
        ServiceRegistry registry = RegistryFactory.getRegistry("non-existent-type");
        
        assertNotNull(registry);
        assertTrue(registry instanceof MemoryServiceRegistry);
        assertEquals("memory", registry.getType());
    }
    
    @Test
    @DisplayName("测试注册中心缓存机制")
    void testRegistryCache() {
        ServiceRegistry registry1 = RegistryFactory.getRegistry("memory");
        ServiceRegistry registry2 = RegistryFactory.getRegistry("memory");
        
        // 应该返回同一个实例
        assertSame(registry1, registry2);
        assertEquals(1, RegistryFactory.getCachedRegistryCount());
    }
    
    @Test
    @DisplayName("测试获取默认服务发现")
    void testGetDefaultDiscovery() {
        ServiceDiscovery discovery = RegistryFactory.getDiscovery(null);
        
        assertNotNull(discovery);
        assertTrue(discovery instanceof MemoryServiceDiscovery);
        assertEquals("memory", discovery.getType());
    }
    
    @Test
    @DisplayName("测试获取指定类型的服务发现")
    void testGetDiscoveryByType() {
        ServiceDiscovery discovery = RegistryFactory.getDiscovery("memory");
        
        assertNotNull(discovery);
        assertTrue(discovery instanceof MemoryServiceDiscovery);
        assertEquals("memory", discovery.getType());
    }
    
    @Test
    @DisplayName("测试获取不存在类型的服务发现回退到内存实现")
    void testGetDiscoveryFallbackToMemory() {
        ServiceDiscovery discovery = RegistryFactory.getDiscovery("non-existent-type");
        
        assertNotNull(discovery);
        assertTrue(discovery instanceof MemoryServiceDiscovery);
        assertEquals("memory", discovery.getType());
    }
    
    @Test
    @DisplayName("测试服务发现缓存机制")
    void testDiscoveryCache() {
        ServiceDiscovery discovery1 = RegistryFactory.getDiscovery("memory");
        ServiceDiscovery discovery2 = RegistryFactory.getDiscovery("memory");
        
        // 应该返回同一个实例
        assertSame(discovery1, discovery2);
        assertEquals(1, RegistryFactory.getCachedDiscoveryCount());
    }
    
    @Test
    @DisplayName("测试获取内存注册中心组合实例")
    void testGetMemoryRegistry() {
        MemoryServiceRegistry registry = RegistryFactory.getMemoryRegistry();
        
        assertNotNull(registry);
        assertTrue(registry instanceof MemoryServiceRegistry);
        
        // 获取关联的服务发现
        MemoryServiceDiscovery discovery = registry.getServiceDiscovery();
        assertNotNull(discovery);
        assertTrue(discovery instanceof MemoryServiceDiscovery);
    }
    
    @Test
    @DisplayName("测试创建新的注册中心实例（不使用缓存）")
    void testCreateRegistry() {
        ServiceRegistry registry1 = RegistryFactory.createRegistry("memory");
        ServiceRegistry registry2 = RegistryFactory.createRegistry("memory");
        
        assertNotNull(registry1);
        assertNotNull(registry2);
        // 应该是不同的实例
        assertNotSame(registry1, registry2);
        
        // 缓存应该为空
        assertEquals(0, RegistryFactory.getCachedRegistryCount());
    }
    
    @Test
    @DisplayName("测试创建新的服务发现实例（不使用缓存）")
    void testCreateDiscovery() {
        ServiceDiscovery discovery1 = RegistryFactory.createDiscovery("memory");
        ServiceDiscovery discovery2 = RegistryFactory.createDiscovery("memory");
        
        assertNotNull(discovery1);
        assertNotNull(discovery2);
        // 应该是不同的实例
        assertNotSame(discovery1, discovery2);
        
        // 缓存应该为空
        assertEquals(0, RegistryFactory.getCachedDiscoveryCount());
    }
    
    @Test
    @DisplayName("测试创建不存在类型的注册中心回退到内存实现")
    void testCreateRegistryFallbackToMemory() {
        ServiceRegistry registry = RegistryFactory.createRegistry("non-existent-type");
        
        assertNotNull(registry);
        assertTrue(registry instanceof MemoryServiceRegistry);
        assertEquals("memory", registry.getType());
    }
    
    @Test
    @DisplayName("测试创建不存在类型的服务发现回退到内存实现")
    void testCreateDiscoveryFallbackToMemory() {
        ServiceDiscovery discovery = RegistryFactory.createDiscovery("non-existent-type");
        
        assertNotNull(discovery);
        assertTrue(discovery instanceof MemoryServiceDiscovery);
        assertEquals("memory", discovery.getType());
    }
    
    @Test
    @DisplayName("测试清空缓存")
    void testClearCache() {
        // 创建一些缓存实例
        RegistryFactory.getRegistry("memory");
        RegistryFactory.getDiscovery("memory");
        
        assertEquals(1, RegistryFactory.getCachedRegistryCount());
        assertEquals(1, RegistryFactory.getCachedDiscoveryCount());
        
        // 清空缓存
        RegistryFactory.clearCache();
        
        assertEquals(0, RegistryFactory.getCachedRegistryCount());
        assertEquals(0, RegistryFactory.getCachedDiscoveryCount());
    }
    
    @Test
    @DisplayName("测试多种类型的缓存")
    void testMultipleTypesCache() {
        ServiceRegistry memoryRegistry = RegistryFactory.getRegistry("memory");
        ServiceRegistry anotherRegistry = RegistryFactory.getRegistry("another-type");
        
        assertNotNull(memoryRegistry);
        assertNotNull(anotherRegistry);
        assertNotSame(memoryRegistry, anotherRegistry);
        
        // 应该有两个缓存实例
        assertEquals(2, RegistryFactory.getCachedRegistryCount());
    }
    
    @Test
    @DisplayName("测试空字符串类型回退到默认")
    void testEmptyStringTypeFallback() {
        ServiceRegistry registry = RegistryFactory.getRegistry("");
        ServiceDiscovery discovery = RegistryFactory.getDiscovery("");
        
        assertNotNull(registry);
        assertNotNull(discovery);
        assertTrue(registry instanceof MemoryServiceRegistry);
        assertTrue(discovery instanceof MemoryServiceDiscovery);
    }
    
    @Test
    @DisplayName("测试工厂方法的线程安全性")
    void testThreadSafety() throws InterruptedException {
        final int threadCount = 10;
        final ServiceRegistry[] registries = new ServiceRegistry[threadCount];
        Thread[] threads = new Thread[threadCount];
        
        // 创建多个线程同时获取注册中心
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                registries[index] = RegistryFactory.getRegistry("memory");
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
        
        // 验证所有线程获取的是同一个实例
        ServiceRegistry firstRegistry = registries[0];
        assertNotNull(firstRegistry);
        
        for (int i = 1; i < threadCount; i++) {
            assertSame(firstRegistry, registries[i]);
        }
        
        // 应该只有一个缓存实例
        assertEquals(1, RegistryFactory.getCachedRegistryCount());
    }
} 