package com.ssrpc.registry.factory;

import com.ssrpc.protocol.ServiceInstance;
import com.ssrpc.registry.api.ServiceChangeListener;
import com.ssrpc.registry.exception.RegistryException;
import com.ssrpc.registry.factory.RegistryManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RegistryManager 单元测试.
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@DisplayName("注册中心管理器测试")
class RegistryManagerTest {
    
    private RegistryManager registryManager;
    private ServiceInstance testInstance;
    
    @BeforeEach
    void setUp() {
        registryManager = new RegistryManager("memory");
        testInstance = createTestServiceInstance();
    }
    
    @AfterEach
    void tearDown() throws RegistryException {
        if (registryManager != null && registryManager.isStarted()) {
            // 清理所有注册的服务
            try {
                List<String> serviceNames = registryManager.getServiceNames();
                for (String serviceName : serviceNames) {
                    List<ServiceInstance> instances = registryManager.discoverServices(serviceName);
                    for (ServiceInstance instance : instances) {
                        registryManager.unregisterService(instance);
                    }
                }
            } catch (Exception e) {
                // 忽略清理过程中的异常
            }
            
            registryManager.stop();
        }
    }
    
    @Test
    @DisplayName("测试默认构造方法")
    void testDefaultConstructor() {
        RegistryManager manager = new RegistryManager();
        
        assertNotNull(manager);
        assertEquals("memory", manager.getRegistryType());
        assertNotNull(manager.getServiceRegistry());
        assertNotNull(manager.getServiceDiscovery());
    }
    
    @Test
    @DisplayName("测试指定类型构造方法")
    void testConstructorWithType() {
        RegistryManager manager = new RegistryManager("memory");
        
        assertNotNull(manager);
        assertEquals("memory", manager.getRegistryType());
        assertNotNull(manager.getServiceRegistry());
        assertNotNull(manager.getServiceDiscovery());
    }
    
    @Test
    @DisplayName("测试管理器生命周期")
    void testLifecycle() throws RegistryException {
        // 初始状态
        assertFalse(registryManager.isStarted());
        assertFalse(registryManager.isAvailable());
        
        // 启动
        registryManager.start();
        assertTrue(registryManager.isStarted());
        assertTrue(registryManager.isAvailable());
        
        // 停止
        registryManager.stop();
        assertFalse(registryManager.isStarted());
        assertFalse(registryManager.isAvailable());
    }
    
    @Test
    @DisplayName("测试重复启动")
    void testMultipleStart() throws RegistryException {
        registryManager.start();
        assertTrue(registryManager.isStarted());
        
        // 重复启动应该不抛异常
        assertDoesNotThrow(() -> registryManager.start());
        assertTrue(registryManager.isStarted());
    }
    
    @Test
    @DisplayName("测试重复停止")
    void testMultipleStop() throws RegistryException {
        registryManager.start();
        registryManager.stop();
        assertFalse(registryManager.isStarted());
        
        // 重复停止应该不抛异常
        assertDoesNotThrow(() -> registryManager.stop());
        assertFalse(registryManager.isStarted());
    }
    
    @Test
    @DisplayName("测试服务注册")
    void testRegisterService() throws RegistryException {
        registryManager.start();
        
        // 注册服务
        assertDoesNotThrow(() -> registryManager.registerService(testInstance));
        
        // 验证服务已注册
        List<ServiceInstance> instances = registryManager.discoverServices("test-service");
        assertEquals(1, instances.size());
        assertEquals(testInstance.getInstanceId(), instances.get(0).getInstanceId());
    }
    
    @Test
    @DisplayName("测试未启动状态下注册服务失败")
    void testRegisterServiceWhenNotStarted() {
        RegistryException exception = assertThrows(RegistryException.class, 
            () -> registryManager.registerService(testInstance));
        assertEquals(RegistryException.ErrorCodes.REGISTRY_UNAVAILABLE, exception.getErrorCode());
    }
    
    @Test
    @DisplayName("测试服务注销")
    void testUnregisterService() throws RegistryException {
        registryManager.start();
        registryManager.registerService(testInstance);
        
        // 注销服务
        assertDoesNotThrow(() -> registryManager.unregisterService(testInstance));
        
        // 验证服务已注销
        List<ServiceInstance> instances = registryManager.discoverServices("test-service");
        assertTrue(instances.isEmpty());
    }
    
    @Test
    @DisplayName("测试未启动状态下注销服务失败")
    void testUnregisterServiceWhenNotStarted() {
        RegistryException exception = assertThrows(RegistryException.class, 
            () -> registryManager.unregisterService(testInstance));
        assertEquals(RegistryException.ErrorCodes.REGISTRY_UNAVAILABLE, exception.getErrorCode());
    }
    
    @Test
    @DisplayName("测试服务更新")
    void testUpdateService() throws RegistryException {
        registryManager.start();
        registryManager.registerService(testInstance);
        
        // 更新服务
        ServiceInstance updatedInstance = createTestServiceInstance();
        updatedInstance.setPort(9999);
        
        assertDoesNotThrow(() -> registryManager.updateService(updatedInstance));
        
        // 验证更新成功
        List<ServiceInstance> instances = registryManager.discoverServices("test-service");
        assertEquals(1, instances.size());
        assertEquals(9999, instances.get(0).getPort());
    }
    
    @Test
    @DisplayName("测试未启动状态下更新服务失败")
    void testUpdateServiceWhenNotStarted() {
        RegistryException exception = assertThrows(RegistryException.class, 
            () -> registryManager.updateService(testInstance));
        assertEquals(RegistryException.ErrorCodes.REGISTRY_UNAVAILABLE, exception.getErrorCode());
    }
    
    @Test
    @DisplayName("测试心跳")
    void testHeartbeat() throws RegistryException {
        registryManager.start();
        registryManager.registerService(testInstance);
        
        // 发送心跳
        assertDoesNotThrow(() -> registryManager.heartbeat(testInstance));
        
        // 验证服务仍然存在
        List<ServiceInstance> instances = registryManager.discoverServices("test-service");
        assertEquals(1, instances.size());
    }
    
    @Test
    @DisplayName("测试未启动状态下心跳失败")
    void testHeartbeatWhenNotStarted() {
        RegistryException exception = assertThrows(RegistryException.class, 
            () -> registryManager.heartbeat(testInstance));
        assertEquals(RegistryException.ErrorCodes.REGISTRY_UNAVAILABLE, exception.getErrorCode());
    }
    
    @Test
    @DisplayName("测试服务发现")
    void testDiscoverServices() throws RegistryException {
        registryManager.start();
        
        ServiceInstance instance1 = createTestServiceInstance("instance-1", "test-service", "1.0.0");
        ServiceInstance instance2 = createTestServiceInstance("instance-2", "test-service", "1.0.0");
        
        registryManager.registerService(instance1);
        registryManager.registerService(instance2);
        
        // 发现服务
        List<ServiceInstance> instances = registryManager.discoverServices("test-service");
        assertEquals(2, instances.size());
    }
    
    @Test
    @DisplayName("测试按版本发现服务")
    void testDiscoverServicesByVersion() throws RegistryException {
        registryManager.start();
        
        ServiceInstance instance1_0 = createTestServiceInstance("instance-1", "test-service", "1.0.0");
        ServiceInstance instance2_0 = createTestServiceInstance("instance-2", "test-service", "2.0.0");
        
        registryManager.registerService(instance1_0);
        registryManager.registerService(instance2_0);
        
        // 发现1.0.0版本的服务
        List<ServiceInstance> instances1_0 = registryManager.discoverServices("test-service", "1.0.0");
        assertEquals(1, instances1_0.size());
        assertEquals("1.0.0", instances1_0.get(0).getVersion());
        
        // 发现2.0.0版本的服务
        List<ServiceInstance> instances2_0 = registryManager.discoverServices("test-service", "2.0.0");
        assertEquals(1, instances2_0.size());
        assertEquals("2.0.0", instances2_0.get(0).getVersion());
    }
    
    @Test
    @DisplayName("测试未启动状态下发现服务失败")
    void testDiscoverServicesWhenNotStarted() {
        RegistryException exception = assertThrows(RegistryException.class, 
            () -> registryManager.discoverServices("test-service"));
        assertEquals(RegistryException.ErrorCodes.REGISTRY_UNAVAILABLE, exception.getErrorCode());
    }
    
    @Test
    @DisplayName("测试获取所有服务名称")
    void testGetServiceNames() throws RegistryException {
        registryManager.start();
        
        ServiceInstance instance1 = createTestServiceInstance("instance-1", "service-1", "1.0.0");
        ServiceInstance instance2 = createTestServiceInstance("instance-2", "service-2", "1.0.0");
        
        registryManager.registerService(instance1);
        registryManager.registerService(instance2);
        
        List<String> serviceNames = registryManager.getServiceNames();
        assertEquals(2, serviceNames.size());
        assertTrue(serviceNames.contains("service-1"));
        assertTrue(serviceNames.contains("service-2"));
    }
    
    @Test
    @DisplayName("测试添加服务监听器")
    void testAddServiceListener() throws RegistryException, InterruptedException {
        registryManager.start();
        
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
        
        // 添加监听器
        registryManager.addServiceListener("test-service", listener);
        
        // 注册服务触发事件
        registryManager.registerService(testInstance);
        
        // 等待事件通知
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }
    
    @Test
    @DisplayName("测试移除服务监听器")
    void testRemoveServiceListener() throws RegistryException, InterruptedException {
        registryManager.start();
        
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
        registryManager.addServiceListener("test-service", listener);
        registryManager.removeServiceListener("test-service", listener);
        
        // 注册服务，不应该收到通知
        registryManager.registerService(testInstance);
        
        // 等待一段时间，确保没有收到通知
        assertFalse(latch.await(100, TimeUnit.MILLISECONDS));
    }
    
    @Test
    @DisplayName("测试关闭管理器")
    void testClose() throws RegistryException {
        registryManager.start();
        registryManager.registerService(testInstance);
        
        // 关闭管理器
        assertDoesNotThrow(() -> registryManager.close());
        
        // 验证状态
        assertFalse(registryManager.isStarted());
        assertFalse(registryManager.isAvailable());
    }
    
    @Test
    @DisplayName("测试toString方法")
    void testToString() throws RegistryException {
        registryManager.start();
        
        String str = registryManager.toString();
        assertNotNull(str);
        assertTrue(str.contains("RegistryManager"));
        assertTrue(str.contains("memory"));
    }
    
    @Test
    @DisplayName("测试获取注册中心和服务发现实例")
    void testGetRegistryAndDiscovery() {
        assertNotNull(registryManager.getServiceRegistry());
        assertNotNull(registryManager.getServiceDiscovery());
        assertEquals("memory", registryManager.getRegistryType());
    }
    
    /**
     * 创建测试用的服务实例
     */
    private ServiceInstance createTestServiceInstance() {
        return createTestServiceInstance("test-instance-1", "test-service", "1.0.0");
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