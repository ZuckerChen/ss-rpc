package com.ssrpc.registry.memory;

import com.ssrpc.protocol.ServiceInstance;
import com.ssrpc.registry.api.ServiceChangeListener;
import com.ssrpc.registry.exception.RegistryException;
import com.ssrpc.registry.impl.memory.MemoryServiceDiscovery;
import com.ssrpc.registry.impl.memory.MemoryServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemoryServiceDiscovery 单元测试.
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@DisplayName("内存服务发现测试")
class MemoryServiceDiscoveryTest {
    
    private MemoryServiceRegistry registry;
    private MemoryServiceDiscovery discovery;
    private ServiceInstance testInstance1;
    private ServiceInstance testInstance2;
    
    @BeforeEach
    void setUp() {
        registry = new MemoryServiceRegistry();
        discovery = registry.getServiceDiscovery();
        testInstance1 = createTestServiceInstance("test-instance-1", "test-service", "1.0.0", 8080);
        testInstance2 = createTestServiceInstance("test-instance-2", "test-service", "1.0.0", 8081);
    }
    
    @Test
    @DisplayName("测试发现服务成功")
    void testDiscoverSuccess() throws RegistryException {
        discovery.start();
        
        // 通过注册中心添加服务实例到共享存储
        addInstanceToDiscovery(testInstance1);
        addInstanceToDiscovery(testInstance2);
        
        // 发现服务
        List<ServiceInstance> instances = discovery.discover("test-service");
        
        assertEquals(2, instances.size());
        assertTrue(instances.contains(testInstance1));
        assertTrue(instances.contains(testInstance2));
    }
    
    @Test
    @DisplayName("测试按版本发现服务")
    void testDiscoverByVersion() throws RegistryException {
        discovery.start();
        
        ServiceInstance instance2_0 = createTestServiceInstance("test-instance-3", "test-service", "2.0.0", 8082);
        
        addInstanceToDiscovery(testInstance1); // 1.0.0
        addInstanceToDiscovery(instance2_0);   // 2.0.0
        
        // 发现1.0.0版本的服务
        List<ServiceInstance> instances1_0 = discovery.discover("test-service", "1.0.0");
        assertEquals(1, instances1_0.size());
        assertEquals(testInstance1, instances1_0.get(0));
        
        // 发现2.0.0版本的服务
        List<ServiceInstance> instances2_0 = discovery.discover("test-service", "2.0.0");
        assertEquals(1, instances2_0.size());
        assertEquals(instance2_0, instances2_0.get(0));
    }
    
    @Test
    @DisplayName("测试发现不存在的服务")
    void testDiscoverNonExistentService() throws RegistryException {
        discovery.start();
        
        List<ServiceInstance> instances = discovery.discover("non-existent-service");
        assertTrue(instances.isEmpty());
    }
    
    @Test
    @DisplayName("测试发现服务时过滤不健康实例")
    void testDiscoverFilterUnhealthyInstances() throws RegistryException {
        discovery.start();
        
        // 创建不健康的实例
        ServiceInstance unhealthyInstance = createTestServiceInstance("unhealthy-instance", "test-service", "1.0.0", 8083);
        unhealthyInstance.setHealthy(false);
        
        addInstanceToDiscovery(testInstance1);     // 健康
        addInstanceToDiscovery(unhealthyInstance); // 不健康
        
        List<ServiceInstance> instances = discovery.discover("test-service");
        
        // 只应该返回健康的实例
        assertEquals(1, instances.size());
        assertEquals(testInstance1, instances.get(0));
    }
    
    @Test
    @DisplayName("测试未启动状态下发现服务失败")
    void testDiscoverWhenNotStarted() {
        RegistryException exception = assertThrows(RegistryException.class, 
            () -> discovery.discover("test-service"));
        assertEquals(RegistryException.ErrorCodes.REGISTRY_UNAVAILABLE, exception.getErrorCode());
    }
    
    @Test
    @DisplayName("测试发现服务时传入空服务名失败")
    void testDiscoverWithNullServiceName() throws RegistryException {
        discovery.start();
        
        RegistryException exception = assertThrows(RegistryException.class, 
            () -> discovery.discover(null));
        assertEquals(RegistryException.ErrorCodes.INVALID_PARAMETER, exception.getErrorCode());
        
        exception = assertThrows(RegistryException.class, 
            () -> discovery.discover(""));
        assertEquals(RegistryException.ErrorCodes.INVALID_PARAMETER, exception.getErrorCode());
    }
    
    @Test
    @DisplayName("测试获取所有服务名称")
    void testGetServiceNames() throws RegistryException {
        discovery.start();
        
        ServiceInstance otherServiceInstance = createTestServiceInstance("other-instance", "other-service", "1.0.0", 9090);
        
        addInstanceToDiscovery(testInstance1);
        addInstanceToDiscovery(otherServiceInstance);
        
        List<String> serviceNames = discovery.getServiceNames();
        
        assertEquals(2, serviceNames.size());
        assertTrue(serviceNames.contains("test-service"));
        assertTrue(serviceNames.contains("other-service"));
    }
    
    @Test
    @DisplayName("测试添加服务监听器")
    void testAddServiceListener() throws RegistryException, InterruptedException {
        discovery.start();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ServiceInstance> registeredInstance = new AtomicReference<>();
        
        ServiceChangeListener listener = new ServiceChangeListener() {
            @Override
            public void onServiceRegistered(String serviceName, ServiceInstance instance) {
                registeredInstance.set(instance);
                latch.countDown();
            }
            
            @Override
            public void onServiceUnregistered(String serviceName, ServiceInstance instance) {}
            
            @Override
            public void onServiceUpdated(String serviceName, ServiceInstance oldInstance, ServiceInstance newInstance) {}
            
            @Override
            public void onServiceInstancesChanged(String serviceName, List<ServiceInstance> instances) {}
            
            @Override
            public void onServiceHealthChanged(String serviceName, ServiceInstance instance, boolean healthy) {}
        };
        
        // 添加监听器
        discovery.addServiceListener("test-service", listener);
        
        // 触发服务注册事件
        discovery.notifyServiceRegistered(testInstance1);
        
        // 等待事件通知
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(testInstance1, registeredInstance.get());
    }
    
    @Test
    @DisplayName("测试移除服务监听器")
    void testRemoveServiceListener() throws RegistryException, InterruptedException {
        discovery.start();
        
        CountDownLatch latch = new CountDownLatch(1);
        
        ServiceChangeListener listener = new ServiceChangeListener() {
            @Override
            public void onServiceRegistered(String serviceName, ServiceInstance instance) {
                latch.countDown();
            }
            
            @Override
            public void onServiceUnregistered(String serviceName, ServiceInstance instance) {}
            
            @Override
            public void onServiceUpdated(String serviceName, ServiceInstance oldInstance, ServiceInstance newInstance) {}
            
            @Override
            public void onServiceInstancesChanged(String serviceName, List<ServiceInstance> instances) {}
            
            @Override
            public void onServiceHealthChanged(String serviceName, ServiceInstance instance, boolean healthy) {}
        };
        
        // 添加并移除监听器
        discovery.addServiceListener("test-service", listener);
        discovery.removeServiceListener("test-service", listener);
        
        // 触发事件，不应该收到通知
        discovery.notifyServiceRegistered(testInstance1);
        
        // 等待一段时间，确保没有收到通知
        assertFalse(latch.await(100, TimeUnit.MILLISECONDS));
    }
    
    @Test
    @DisplayName("测试添加监听器时传入空参数失败")
    void testAddListenerWithNullParameters() throws RegistryException {
        discovery.start();
        
        ServiceChangeListener listener = new TestServiceChangeListener();
        
        // 空服务名
        RegistryException exception = assertThrows(RegistryException.class, 
            () -> discovery.addServiceListener(null, listener));
        assertEquals(RegistryException.ErrorCodes.INVALID_PARAMETER, exception.getErrorCode());
        
        // 空监听器
        exception = assertThrows(RegistryException.class, 
            () -> discovery.addServiceListener("test-service", null));
        assertEquals(RegistryException.ErrorCodes.INVALID_PARAMETER, exception.getErrorCode());
    }
    
    @Test
    @DisplayName("测试服务发现生命周期")
    void testLifecycle() throws RegistryException {
        // 初始状态
        assertFalse(discovery.isStarted());
        assertFalse(discovery.isAvailable());
        
        // 启动
        discovery.start();
        assertTrue(discovery.isStarted());
        assertTrue(discovery.isAvailable());
        
        // 停止
        discovery.stop();
        assertFalse(discovery.isStarted());
        assertFalse(discovery.isAvailable());
    }
    
    @Test
    @DisplayName("测试服务发现类型")
    void testDiscoveryType() {
        assertEquals("memory", discovery.getType());
    }
    
    @Test
    @DisplayName("测试关闭服务发现")
    void testClose() throws RegistryException {
        discovery.start();
        
        assertDoesNotThrow(() -> discovery.close());
        
        assertFalse(discovery.isStarted());
        assertFalse(discovery.isAvailable());
    }
    
    @Test
    @DisplayName("测试获取监听器数量")
    void testGetListenerCount() throws RegistryException {
        discovery.start();
        
        assertEquals(0, discovery.getListenerCount());
        
        ServiceChangeListener listener1 = new TestServiceChangeListener();
        ServiceChangeListener listener2 = new TestServiceChangeListener();
        
        discovery.addServiceListener("service1", listener1);
        discovery.addServiceListener("service2", listener2);
        
        assertEquals(2, discovery.getListenerCount());
    }
    
    @Test
    @DisplayName("测试toString方法")
    void testToString() throws RegistryException {
        discovery.start();
        addInstanceToDiscovery(testInstance1);
        
        String str = discovery.toString();
        assertNotNull(str);
        assertTrue(str.contains("MemoryServiceDiscovery"));
    }
    
    /**
     * 创建测试用的服务实例
     */
    private ServiceInstance createTestServiceInstance(String instanceId, String serviceName, String version, int port) {
        ServiceInstance instance = new ServiceInstance();
        instance.setInstanceId(instanceId);
        instance.setServiceName(serviceName);
        instance.setVersion(version);
        instance.setHost("127.0.0.1");
        instance.setPort(port);
        instance.setHealthy(true);
        return instance;
    }
    
    /**
     * 通过注册中心添加实例到服务发现的共享存储中
     */
    private void addInstanceToDiscovery(ServiceInstance instance) throws RegistryException {
        // 启动注册中心（如果还没启动）
        if (!registry.isStarted()) {
            registry.start();
        }
        // 通过注册中心注册服务实例
        registry.register(instance);
    }
    
    /**
     * 测试用的服务变更监听器
     */
    private static class TestServiceChangeListener implements ServiceChangeListener {
        @Override
        public void onServiceRegistered(String serviceName, ServiceInstance instance) {}
        
        @Override
        public void onServiceUnregistered(String serviceName, ServiceInstance instance) {}
        
        @Override
        public void onServiceUpdated(String serviceName, ServiceInstance oldInstance, ServiceInstance newInstance) {}
        
        @Override
        public void onServiceInstancesChanged(String serviceName, List<ServiceInstance> instances) {}
        
        @Override
        public void onServiceHealthChanged(String serviceName, ServiceInstance instance, boolean healthy) {}
    }
} 