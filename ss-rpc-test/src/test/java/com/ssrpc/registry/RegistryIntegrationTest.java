package com.ssrpc.registry;

import com.ssrpc.protocol.ServiceInstance;
import com.ssrpc.registry.api.ServiceChangeListener;
import com.ssrpc.registry.exception.RegistryException;
import com.ssrpc.registry.factory.RegistryManager;
import com.ssrpc.registry.impl.memory.MemoryServiceDiscovery;
import com.ssrpc.registry.impl.memory.MemoryServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Registry模块集成测试.
 * 
 * 测试注册中心和服务发现的协同工作
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@DisplayName("Registry模块集成测试")
class RegistryIntegrationTest {
    
    private MemoryServiceRegistry registry;
    private MemoryServiceDiscovery discovery;
    private RegistryManager registryManager;
    
    @BeforeEach
    void setUp() throws RegistryException {
        // 创建共享存储的注册中心和服务发现
        registry = new MemoryServiceRegistry();
        discovery = registry.getServiceDiscovery();
        
        // 创建注册中心管理器
        registryManager = new RegistryManager("memory");
        
        // 启动所有组件
        registry.start();
        discovery.start();
        registryManager.start();
    }
    
    @Test
    @DisplayName("测试注册中心和服务发现的数据同步")
    void testRegistryDiscoveryDataSync() throws RegistryException {
        ServiceInstance instance = createTestServiceInstance("instance-1", "test-service", "1.0.0");
        
        // 通过注册中心注册服务
        registry.register(instance);
        
        // 通过服务发现查找服务
        List<ServiceInstance> discoveredInstances = discovery.discover("test-service");
        
        assertEquals(1, discoveredInstances.size());
        assertEquals(instance.getInstanceId(), discoveredInstances.get(0).getInstanceId());
        
        // 通过注册中心注销服务
        registry.unregister(instance);
        
        // 通过服务发现验证服务已注销
        discoveredInstances = discovery.discover("test-service");
        assertTrue(discoveredInstances.isEmpty());
    }
    
    @Test
    @DisplayName("测试服务变更事件通知")
    void testServiceChangeNotification() throws RegistryException, InterruptedException {
        CountDownLatch registerLatch = new CountDownLatch(1);
        CountDownLatch unregisterLatch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(1);
        
        ServiceChangeListener listener = new ServiceChangeListener() {
            @Override
            public void onServiceRegistered(String serviceName, ServiceInstance instance) {
                registerLatch.countDown();
            }
            
            @Override
            public void onServiceUnregistered(String serviceName, ServiceInstance instance) {
                unregisterLatch.countDown();
            }
            
            @Override
            public void onServiceUpdated(String serviceName, ServiceInstance oldInstance, ServiceInstance newInstance) {
                updateLatch.countDown();
            }
            
            @Override
            public void onServiceInstancesChanged(String serviceName, List<ServiceInstance> instances) {}
            
            @Override
            public void onServiceHealthChanged(String serviceName, ServiceInstance instance, boolean healthy) {}
        };
        
        // 添加监听器
        discovery.addServiceListener("test-service", listener);
        
        ServiceInstance instance = createTestServiceInstance("instance-1", "test-service", "1.0.0");
        
        // 注册服务
        registry.register(instance);
        assertTrue(registerLatch.await(1, TimeUnit.SECONDS));
        
        // 更新服务
        instance.setPort(9999);
        registry.update(instance);
        assertTrue(updateLatch.await(1, TimeUnit.SECONDS));
        
        // 注销服务
        registry.unregister(instance);
        assertTrue(unregisterLatch.await(1, TimeUnit.SECONDS));
    }
    
    @Test
    @DisplayName("测试多服务实例管理")
    void testMultipleServiceInstances() throws RegistryException {
        // 创建多个服务实例
        ServiceInstance instance1 = createTestServiceInstance("instance-1", "user-service", "1.0.0");
        ServiceInstance instance2 = createTestServiceInstance("instance-2", "user-service", "1.0.0");
        ServiceInstance instance3 = createTestServiceInstance("instance-3", "order-service", "1.0.0");
        
        // 注册所有实例
        registry.register(instance1);
        registry.register(instance2);
        registry.register(instance3);
        
        // 验证user-service有2个实例
        List<ServiceInstance> userServiceInstances = discovery.discover("user-service");
        assertEquals(2, userServiceInstances.size());
        
        // 验证order-service有1个实例
        List<ServiceInstance> orderServiceInstances = discovery.discover("order-service");
        assertEquals(1, orderServiceInstances.size());
        
        // 验证总共有2个不同的服务
        List<String> serviceNames = discovery.getServiceNames();
        assertEquals(2, serviceNames.size());
        assertTrue(serviceNames.contains("user-service"));
        assertTrue(serviceNames.contains("order-service"));
    }
    
    @Test
    @DisplayName("测试服务版本管理")
    void testServiceVersionManagement() throws RegistryException {
        // 创建不同版本的服务实例
        ServiceInstance instance1_0 = createTestServiceInstance("instance-1", "api-service", "1.0.0");
        ServiceInstance instance1_1 = createTestServiceInstance("instance-2", "api-service", "1.1.0");
        ServiceInstance instance2_0 = createTestServiceInstance("instance-3", "api-service", "2.0.0");
        
        // 注册所有版本
        registry.register(instance1_0);
        registry.register(instance1_1);
        registry.register(instance2_0);
        
        // 发现所有版本的实例
        List<ServiceInstance> allInstances = discovery.discover("api-service");
        assertEquals(3, allInstances.size());
        
        // 发现特定版本的实例
        List<ServiceInstance> v1_0_instances = discovery.discover("api-service", "1.0.0");
        assertEquals(1, v1_0_instances.size());
        assertEquals("1.0.0", v1_0_instances.get(0).getVersion());
        
        List<ServiceInstance> v2_0_instances = discovery.discover("api-service", "2.0.0");
        assertEquals(1, v2_0_instances.size());
        assertEquals("2.0.0", v2_0_instances.get(0).getVersion());
    }
    
    @Test
    @DisplayName("测试健康状态过滤")
    void testHealthStatusFiltering() throws RegistryException {
        ServiceInstance healthyInstance = createTestServiceInstance("healthy-instance", "test-service", "1.0.0");
        ServiceInstance unhealthyInstance = createTestServiceInstance("unhealthy-instance", "test-service", "1.0.0");
        unhealthyInstance.setHealthy(false);
        
        // 注册健康和不健康的实例
        registry.register(healthyInstance);
        registry.register(unhealthyInstance);
        
        // 服务发现应该只返回健康的实例
        List<ServiceInstance> discoveredInstances = discovery.discover("test-service");
        assertEquals(1, discoveredInstances.size());
        assertEquals("healthy-instance", discoveredInstances.get(0).getInstanceId());
        assertTrue(discoveredInstances.get(0).isHealthy());
    }
    
    @Test
    @DisplayName("测试注册中心管理器的完整流程")
    void testRegistryManagerCompleteFlow() throws RegistryException, InterruptedException {
        CountDownLatch eventLatch = new CountDownLatch(3); // 注册、更新、注销事件
        AtomicInteger eventCount = new AtomicInteger(0);
        
        ServiceChangeListener listener = new ServiceChangeListener() {
            @Override
            public void onServiceRegistered(String serviceName, ServiceInstance instance) {
                eventCount.incrementAndGet();
                eventLatch.countDown();
            }
            
            @Override
            public void onServiceUnregistered(String serviceName, ServiceInstance instance) {
                eventCount.incrementAndGet();
                eventLatch.countDown();
            }
            
            @Override
            public void onServiceUpdated(String serviceName, ServiceInstance oldInstance, ServiceInstance newInstance) {
                eventCount.incrementAndGet();
                eventLatch.countDown();
            }
            
            @Override
            public void onServiceInstancesChanged(String serviceName, List<ServiceInstance> instances) {}
            
            @Override
            public void onServiceHealthChanged(String serviceName, ServiceInstance instance, boolean healthy) {}
        };
        
        // 添加监听器
        registryManager.addServiceListener("test-service", listener);
        
        ServiceInstance instance = createTestServiceInstance("instance-1", "test-service", "1.0.0");
        
        // 1. 注册服务
        registryManager.registerService(instance);
        List<ServiceInstance> instances = registryManager.discoverServices("test-service");
        assertEquals(1, instances.size());
        
        // 2. 更新服务
        instance.setPort(9999);
        registryManager.updateService(instance);
        instances = registryManager.discoverServices("test-service");
        assertEquals(9999, instances.get(0).getPort());
        
        // 3. 心跳
        registryManager.heartbeat(instance);
        instances = registryManager.discoverServices("test-service");
        assertEquals(1, instances.size());
        
        // 4. 注销服务
        registryManager.unregisterService(instance);
        instances = registryManager.discoverServices("test-service");
        assertTrue(instances.isEmpty());
        
        // 等待所有事件通知
        assertTrue(eventLatch.await(2, TimeUnit.SECONDS));
        assertEquals(3, eventCount.get());
    }
    
    @Test
    @DisplayName("测试并发操作")
    void testConcurrentOperations() throws InterruptedException {
        final int threadCount = 10;
        final int instancesPerThread = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);
        
        // 创建多个线程并发注册服务
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < instancesPerThread; j++) {
                        ServiceInstance instance = createTestServiceInstance(
                            "thread-" + threadIndex + "-instance-" + j,
                            "concurrent-service",
                            "1.0.0"
                                                 );
                         instance.setPort(8080 + threadIndex * 100 + j);
                         
                         try {
                             registryManager.registerService(instance);
                         } catch (RegistryException e) {
                             e.printStackTrace();
                         }
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    completeLatch.countDown();
                }
            }).start();
        }
        
        // 启动所有线程
        startLatch.countDown();
        
        // 等待所有线程完成
        assertTrue(completeLatch.await(5, TimeUnit.SECONDS));
        
        // 验证所有实例都已注册
        try {
            List<ServiceInstance> allInstances = registryManager.discoverServices("concurrent-service");
            assertEquals(threadCount * instancesPerThread, allInstances.size());
            
            // 验证服务名称列表
            List<String> serviceNames = registryManager.getServiceNames();
            assertTrue(serviceNames.contains("concurrent-service"));
        } catch (RegistryException e) {
            fail("Failed to discover services: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("测试异常情况处理")
    void testExceptionHandling() throws RegistryException {
        ServiceInstance instance = createTestServiceInstance("instance-1", "test-service", "1.0.0");
        
        // 测试更新不存在的服务
        assertThrows(RegistryException.class, () -> registry.update(instance));
        
        // 测试注册null实例
        assertThrows(RegistryException.class, () -> registry.register(null));
        
        // 测试发现服务时传入null服务名
        assertThrows(RegistryException.class, () -> discovery.discover(null));
        
        // 测试添加null监听器
        assertThrows(RegistryException.class, () -> discovery.addServiceListener("test-service", null));
    }
    
    /**
     * 创建测试用的服务实例
     */
    private ServiceInstance createTestServiceInstance(String instanceId, String serviceName, String version) {
        ServiceInstance instance = new ServiceInstance();
        instance.setInstanceId(instanceId);
        instance.setServiceName(serviceName);
        instance.setVersion(version);
        instance.setHost("127.0.0.1");
        instance.setPort(8080);
        instance.setHealthy(true);
        return instance;
    }
} 