package com.ssrpc.registry.factory;

import com.ssrpc.protocol.ServiceInstance;
import com.ssrpc.registry.api.ServiceChangeListener;
import com.ssrpc.registry.api.ServiceDiscovery;
import com.ssrpc.registry.api.ServiceRegistry;
import com.ssrpc.registry.exception.RegistryException;
import com.ssrpc.registry.impl.memory.MemoryServiceDiscovery;
import com.ssrpc.registry.impl.memory.MemoryServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 注册中心管理器.
 * 
 * 统一管理服务注册和服务发现，提供一站式的注册中心服务
 * 支持多种注册中心实现，自动处理注册中心的生命周期
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class RegistryManager {
    
    private static final Logger log = LoggerFactory.getLogger(RegistryManager.class);
    
    /**
     * 服务注册中心
     */
    private final ServiceRegistry serviceRegistry;
    
    /**
     * 服务发现
     */
    private final ServiceDiscovery serviceDiscovery;
    
    /**
     * 注册中心类型
     */
    private final String registryType;
    
    /**
     * 启动状态
     */
    private final AtomicBoolean started = new AtomicBoolean(false);
    
    /**
     * 构造方法 - 使用指定类型的注册中心
     * 
     * @param registryType 注册中心类型
     */
    public RegistryManager(String registryType) {
        this.registryType = registryType != null ? registryType : "memory";
        
        // 对于内存注册中心，使用共享存储的组合实例
        if ("memory".equals(this.registryType)) {
            MemoryServiceRegistry memoryRegistry = RegistryFactory.getMemoryRegistry();
            this.serviceRegistry = memoryRegistry;
            this.serviceDiscovery = memoryRegistry.getServiceDiscovery();
        } else {
            // 对于其他类型，分别创建注册和发现实例
            this.serviceRegistry = RegistryFactory.getRegistry(this.registryType);
            this.serviceDiscovery = RegistryFactory.getDiscovery(this.registryType);
        }
        
        log.info("Created registry manager with type: {}", this.registryType);
    }
    
    /**
     * 默认构造方法 - 使用内存注册中心
     */
    public RegistryManager() {
        this("memory");
    }
    
    /**
     * 启动注册中心管理器
     * 
     * @throws RegistryException 启动异常
     */
    public void start() throws RegistryException {
        if (started.compareAndSet(false, true)) {
            try {
                // 启动服务注册中心
                if (serviceRegistry instanceof MemoryServiceRegistry) {
                    ((MemoryServiceRegistry) serviceRegistry).start();
                }
                
                // 启动服务发现（如果不是共享实例）
                if (serviceDiscovery != serviceRegistry && serviceDiscovery instanceof MemoryServiceDiscovery) {
                    ((MemoryServiceDiscovery) serviceDiscovery).start();
                }
                
                log.info("Registry manager started successfully with type: {}", registryType);
                
            } catch (Exception e) {
                started.set(false);
                throw new RegistryException(
                    RegistryException.ErrorCodes.UNKNOWN_ERROR,
                    "Failed to start registry manager",
                    e
                );
            }
        }
    }
    
    /**
     * 停止注册中心管理器
     * 
     * @throws RegistryException 停止异常
     */
    public void stop() throws RegistryException {
        if (started.compareAndSet(true, false)) {
            try {
                // 停止服务发现（如果不是共享实例）
                if (serviceDiscovery != serviceRegistry && serviceDiscovery instanceof MemoryServiceDiscovery) {
                    ((MemoryServiceDiscovery) serviceDiscovery).stop();
                }
                
                // 停止服务注册中心
                if (serviceRegistry instanceof MemoryServiceRegistry) {
                    ((MemoryServiceRegistry) serviceRegistry).stop();
                }
                
                log.info("Registry manager stopped successfully");
                
            } catch (Exception e) {
                started.set(true);
                throw new RegistryException(
                    RegistryException.ErrorCodes.UNKNOWN_ERROR,
                    "Failed to stop registry manager",
                    e
                );
            }
        }
    }
    
    /**
     * 注册服务实例
     * 
     * @param serviceInstance 服务实例
     * @throws RegistryException 注册异常
     */
    public void registerService(ServiceInstance serviceInstance) throws RegistryException {
        if (!started.get()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.REGISTRY_UNAVAILABLE,
                "Registry manager is not started"
            );
        }
        
        serviceRegistry.register(serviceInstance);
        log.debug("Registered service: {} at {}", 
            serviceInstance.getServiceName(), serviceInstance.getAddress());
    }
    
    /**
     * 注销服务实例
     * 
     * @param serviceInstance 服务实例
     * @throws RegistryException 注销异常
     */
    public void unregisterService(ServiceInstance serviceInstance) throws RegistryException {
        if (!started.get()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.REGISTRY_UNAVAILABLE,
                "Registry manager is not started"
            );
        }
        
        serviceRegistry.unregister(serviceInstance);
        log.debug("Unregistered service: {} at {}", 
            serviceInstance.getServiceName(), serviceInstance.getAddress());
    }
    
    /**
     * 更新服务实例
     * 
     * @param serviceInstance 服务实例
     * @throws RegistryException 更新异常
     */
    public void updateService(ServiceInstance serviceInstance) throws RegistryException {
        if (!started.get()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.REGISTRY_UNAVAILABLE,
                "Registry manager is not started"
            );
        }
        
        serviceRegistry.update(serviceInstance);
        log.debug("Updated service: {} at {}", 
            serviceInstance.getServiceName(), serviceInstance.getAddress());
    }
    
    /**
     * 发送心跳
     * 
     * @param serviceInstance 服务实例
     * @throws RegistryException 心跳异常
     */
    public void heartbeat(ServiceInstance serviceInstance) throws RegistryException {
        if (!started.get()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.REGISTRY_UNAVAILABLE,
                "Registry manager is not started"
            );
        }
        
        serviceRegistry.heartbeat(serviceInstance);
        log.debug("Heartbeat sent for service: {} at {}", 
            serviceInstance.getServiceName(), serviceInstance.getAddress());
    }
    
    /**
     * 发现服务实例
     * 
     * @param serviceName 服务名称
     * @return 服务实例列表
     * @throws RegistryException 发现异常
     */
    public List<ServiceInstance> discoverServices(String serviceName) throws RegistryException {
        if (!started.get()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.REGISTRY_UNAVAILABLE,
                "Registry manager is not started"
            );
        }
        
        List<ServiceInstance> instances = serviceDiscovery.discover(serviceName);
        log.debug("Discovered {} instances for service: {}", instances.size(), serviceName);
        return instances;
    }
    
    /**
     * 发现指定版本的服务实例
     * 
     * @param serviceName 服务名称
     * @param version 服务版本
     * @return 服务实例列表
     * @throws RegistryException 发现异常
     */
    public List<ServiceInstance> discoverServices(String serviceName, String version) throws RegistryException {
        if (!started.get()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.REGISTRY_UNAVAILABLE,
                "Registry manager is not started"
            );
        }
        
        List<ServiceInstance> instances = serviceDiscovery.discover(serviceName, version);
        log.debug("Discovered {} instances for service: {}:{}", instances.size(), serviceName, version);
        return instances;
    }
    
    /**
     * 获取所有服务名称
     * 
     * @return 服务名称列表
     * @throws RegistryException 获取异常
     */
    public List<String> getServiceNames() throws RegistryException {
        if (!started.get()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.REGISTRY_UNAVAILABLE,
                "Registry manager is not started"
            );
        }
        
        return serviceDiscovery.getServiceNames();
    }
    
    /**
     * 添加服务变更监听器
     * 
     * @param serviceName 服务名称
     * @param listener 监听器
     * @throws RegistryException 添加异常
     */
    public void addServiceListener(String serviceName, ServiceChangeListener listener) throws RegistryException {
        if (!started.get()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.REGISTRY_UNAVAILABLE,
                "Registry manager is not started"
            );
        }
        
        serviceDiscovery.addServiceListener(serviceName, listener);
        log.debug("Added service listener for service: {}", serviceName);
    }
    
    /**
     * 移除服务变更监听器
     * 
     * @param serviceName 服务名称
     * @param listener 监听器
     * @throws RegistryException 移除异常
     */
    public void removeServiceListener(String serviceName, ServiceChangeListener listener) throws RegistryException {
        if (!started.get()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.REGISTRY_UNAVAILABLE,
                "Registry manager is not started"
            );
        }
        
        serviceDiscovery.removeServiceListener(serviceName, listener);
        log.debug("Removed service listener for service: {}", serviceName);
    }
    
    /**
     * 获取服务注册中心
     * 
     * @return 服务注册中心
     */
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }
    
    /**
     * 获取服务发现
     * 
     * @return 服务发现
     */
    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }
    
    /**
     * 获取注册中心类型
     * 
     * @return 注册中心类型
     */
    public String getRegistryType() {
        return registryType;
    }
    
    /**
     * 检查是否已启动
     * 
     * @return true表示已启动，false表示未启动
     */
    public boolean isStarted() {
        return started.get();
    }
    
    /**
     * 检查注册中心是否可用
     * 
     * @return true表示可用，false表示不可用
     */
    public boolean isAvailable() {
        return started.get() && serviceRegistry.isAvailable() && serviceDiscovery.isAvailable();
    }
    
    /**
     * 关闭注册中心管理器
     */
    public void close() {
        try {
            stop();
        } catch (RegistryException e) {
            log.warn("Error occurred while closing registry manager", e);
        }
    }
    
    @Override
    public String toString() {
        return "RegistryManager{" +
                "registryType='" + registryType + '\'' +
                ", started=" + started.get() +
                ", available=" + isAvailable() +
                '}';
    }
} 