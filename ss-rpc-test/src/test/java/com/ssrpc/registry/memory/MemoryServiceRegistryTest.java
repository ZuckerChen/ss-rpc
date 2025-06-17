package com.ssrpc.registry.memory;

import com.ssrpc.protocol.ServiceInstance;
import com.ssrpc.registry.exception.RegistryException;
import com.ssrpc.registry.impl.memory.MemoryServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemoryServiceRegistry 单元测试.
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@DisplayName("内存服务注册测试")
class MemoryServiceRegistryTest {
    
    private MemoryServiceRegistry registry;
    private ServiceInstance testInstance;
    
    @BeforeEach
    void setUp() {
        registry = new MemoryServiceRegistry();
        testInstance = createTestServiceInstance();
    }
    
    @Test
    @DisplayName("测试服务注册成功")
    void testRegisterSuccess() throws RegistryException {
        // 启动注册中心
        registry.start();
        
        // 注册服务
        assertDoesNotThrow(() -> registry.register(testInstance));
        
        // 验证服务已注册
        assertTrue(registry.instanceExists(testInstance.getInstanceId()));
        assertEquals(testInstance, registry.getInstance(testInstance.getInstanceId()));
        assertEquals(1, registry.getInstanceCount());
    }
    
    @Test
    @DisplayName("测试重复注册服务")
    void testRegisterDuplicate() throws RegistryException {
        registry.start();
        
        // 首次注册
        registry.register(testInstance);
        
        // 重复注册应该成功（更新模式）
        assertDoesNotThrow(() -> registry.register(testInstance));
        assertEquals(1, registry.getInstanceCount());
    }
    
    @Test
    @DisplayName("测试未启动状态下注册服务失败")
    void testRegisterWhenNotStarted() {
        // 未启动状态下注册应该失败
        RegistryException exception = assertThrows(RegistryException.class, 
            () -> registry.register(testInstance));
        assertEquals(RegistryException.ErrorCodes.CONFIGURATION_ERROR, exception.getErrorCode());
    }
    
    @Test
    @DisplayName("测试注册空服务实例失败")
    void testRegisterNullInstance() throws RegistryException {
        registry.start();
        
        RegistryException exception = assertThrows(RegistryException.class, 
            () -> registry.register(null));
        assertEquals(RegistryException.ErrorCodes.CONFIGURATION_ERROR, exception.getErrorCode());
    }
    
    @Test
    @DisplayName("测试服务注销成功")
    void testUnregisterSuccess() throws RegistryException {
        registry.start();
        registry.register(testInstance);
        
        // 注销服务
        assertDoesNotThrow(() -> registry.unregister(testInstance));
        
        // 验证服务已注销
        assertFalse(registry.instanceExists(testInstance.getInstanceId()));
        assertNull(registry.getInstance(testInstance.getInstanceId()));
        assertEquals(0, registry.getInstanceCount());
    }
    
    @Test
    @DisplayName("测试注销不存在的服务")
    void testUnregisterNonExistent() throws RegistryException {
        registry.start();
        
        // 注销不存在的服务应该不抛异常
        assertDoesNotThrow(() -> registry.unregister(testInstance));
    }
    
    @Test
    @DisplayName("测试服务更新成功")
    void testUpdateSuccess() throws RegistryException {
        registry.start();
        registry.register(testInstance);
        
        // 创建更新后的实例
        ServiceInstance updatedInstance = createTestServiceInstance();
        updatedInstance.setPort(9999);
        
        // 更新服务
        assertDoesNotThrow(() -> registry.update(updatedInstance));
        
        // 验证更新成功
        ServiceInstance retrieved = registry.getInstance(testInstance.getInstanceId());
        assertEquals(9999, retrieved.getPort());
    }
    
    @Test
    @DisplayName("测试更新不存在的服务失败")
    void testUpdateNonExistent() throws RegistryException {
        registry.start();
        
        RegistryException exception = assertThrows(RegistryException.class, 
            () -> registry.update(testInstance));
        assertEquals(RegistryException.ErrorCodes.INSTANCE_NOT_FOUND, exception.getErrorCode());
    }
    
    @Test
    @DisplayName("测试心跳成功")
    void testHeartbeatSuccess() throws RegistryException {
        registry.start();
        registry.register(testInstance);
        
        // 发送心跳
        assertDoesNotThrow(() -> registry.heartbeat(testInstance));
        
        // 验证实例仍然存在
        assertTrue(registry.instanceExists(testInstance.getInstanceId()));
    }
    
    @Test
    @DisplayName("测试心跳不存在的服务")
    void testHeartbeatNonExistent() throws RegistryException {
        registry.start();
        
        // 对不存在的服务发送心跳应该不抛异常
        assertDoesNotThrow(() -> registry.heartbeat(testInstance));
    }
    
    @Test
    @DisplayName("测试获取服务实例")
    void testGetInstance() throws RegistryException {
        registry.start();
        registry.register(testInstance);
        
        ServiceInstance retrieved = registry.getInstance(testInstance.getInstanceId());
        assertNotNull(retrieved);
        assertEquals(testInstance.getInstanceId(), retrieved.getInstanceId());
        assertEquals(testInstance.getServiceName(), retrieved.getServiceName());
    }
    
    @Test
    @DisplayName("测试获取不存在的服务实例")
    void testGetNonExistentInstance() throws RegistryException {
        registry.start();
        
        ServiceInstance retrieved = registry.getInstance("non-existent-id");
        assertNull(retrieved);
    }
    
    @Test
    @DisplayName("测试检查服务实例是否存在")
    void testInstanceExists() throws RegistryException {
        registry.start();
        
        // 未注册时应该返回false
        assertFalse(registry.instanceExists(testInstance.getInstanceId()));
        
        // 注册后应该返回true
        registry.register(testInstance);
        assertTrue(registry.instanceExists(testInstance.getInstanceId()));
        
        // 注销后应该返回false
        registry.unregister(testInstance);
        assertFalse(registry.instanceExists(testInstance.getInstanceId()));
    }
    
    @Test
    @DisplayName("测试注册中心生命周期")
    void testLifecycle() throws RegistryException {
        // 初始状态
        assertFalse(registry.isStarted());
        assertFalse(registry.isAvailable());
        
        // 启动
        registry.start();
        assertTrue(registry.isStarted());
        assertTrue(registry.isAvailable());
        
        // 停止
        registry.stop();
        assertFalse(registry.isStarted());
        assertFalse(registry.isAvailable());
    }
    
    @Test
    @DisplayName("测试注册中心类型")
    void testRegistryType() {
        assertEquals("memory", registry.getType());
    }
    
    @Test
    @DisplayName("测试关闭注册中心")
    void testClose() throws RegistryException {
        registry.start();
        registry.register(testInstance);
        
        // 关闭注册中心
        assertDoesNotThrow(() -> registry.close());
        
        // 验证状态
        assertFalse(registry.isStarted());
        assertFalse(registry.isAvailable());
    }
    
    @Test
    @DisplayName("测试toString方法")
    void testToString() throws RegistryException {
        registry.start();
        registry.register(testInstance);
        
        String str = registry.toString();
        assertNotNull(str);
        assertTrue(str.contains("MemoryServiceRegistry"));
        assertTrue(str.contains("instanceCount=1"));
    }
    
    /**
     * 创建测试用的服务实例
     */
    private ServiceInstance createTestServiceInstance() {
        ServiceInstance instance = new ServiceInstance();
        instance.setInstanceId("test-instance-1");
        instance.setServiceName("test-service");
        instance.setVersion("1.0.0");
        instance.setHost("127.0.0.1");
        instance.setPort(8080);
        instance.setHealthy(true);
        return instance;
    }
} 