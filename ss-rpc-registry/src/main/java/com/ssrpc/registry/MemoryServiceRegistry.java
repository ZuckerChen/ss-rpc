package com.ssrpc.registry;

import com.ssrpc.protocol.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 内存服务注册实现.
 * 
 * 基于内存存储的服务注册实现，适用于测试和单机场景
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class MemoryServiceRegistry implements ServiceRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(MemoryServiceRegistry.class);
    
    /**
     * 服务实例存储
     */
    private final ConcurrentHashMap<String, ServiceInstance> instances = new ConcurrentHashMap<>();
    
    /**
     * 启动状态
     */
    private final AtomicBoolean started = new AtomicBoolean(false);
    
    /**
     * 构造方法
     */
    public MemoryServiceRegistry() {
    }
    
    @Override
    public void register(ServiceInstance serviceInstance) throws RegistryException {
        if (serviceInstance == null) {
            throw new RegistryException(
                RegistryException.ErrorCodes.CONFIGURATION_ERROR,
                "Service instance cannot be null"
            );
        }
        
        if (!started.get()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.CONFIGURATION_ERROR,
                "Service registry is not started"
            );
        }
        
        // 检查实例是否已存在
        if (instanceExists(serviceInstance.getInstanceId())) {
            throw new RegistryException(
                RegistryException.ErrorCodes.INSTANCE_ALREADY_EXISTS,
                "Service instance already exists: " + serviceInstance.getInstanceId()
            );
        }
        
        try {
            // 添加服务实例
            instances.put(serviceInstance.getInstanceId(), serviceInstance);
            
            log.info("Registered service instance: {} for service: {}", 
                    serviceInstance.getInstanceId(), serviceInstance.getServiceName());
            
        } catch (Exception e) {
            throw new RegistryException(
                RegistryException.ErrorCodes.REGISTRATION_FAILED,
                "Failed to register service instance: " + serviceInstance.getInstanceId(),
                e
            );
        }
    }
    
    @Override
    public void unregister(ServiceInstance serviceInstance) throws RegistryException {
        if (serviceInstance == null) {
            throw new RegistryException(
                RegistryException.ErrorCodes.CONFIGURATION_ERROR,
                "Service instance cannot be null"
            );
        }
        
        if (!started.get()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.CONFIGURATION_ERROR,
                "Service registry is not started"
            );
        }
        
        try {
            boolean removed = instances.remove(serviceInstance.getInstanceId()) != null;
            
            if (!removed) {
                log.warn("Service instance not found for unregistration: {}", serviceInstance.getInstanceId());
            } else {
                log.info("Unregistered service instance: {} for service: {}", 
                        serviceInstance.getInstanceId(), serviceInstance.getServiceName());
            }
            
        } catch (Exception e) {
            throw new RegistryException(
                RegistryException.ErrorCodes.UNREGISTRATION_FAILED,
                "Failed to unregister service instance: " + serviceInstance.getInstanceId(),
                e
            );
        }
    }
    
    @Override
    public void update(ServiceInstance serviceInstance) throws RegistryException {
        if (serviceInstance == null) {
            throw new RegistryException(
                RegistryException.ErrorCodes.CONFIGURATION_ERROR,
                "Service instance cannot be null"
            );
        }
        
        if (!started.get()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.CONFIGURATION_ERROR,
                "Service registry is not started"
            );
        }
        
        try {
            boolean updated = instances.replace(serviceInstance.getInstanceId(), serviceInstance) != null;
            
            if (!updated) {
                throw new RegistryException(
                    RegistryException.ErrorCodes.INSTANCE_NOT_FOUND,
                    "Service instance not found for update: " + serviceInstance.getInstanceId()
                );
            }
            
            log.debug("Updated service instance: {} for service: {}", 
                     serviceInstance.getInstanceId(), serviceInstance.getServiceName());
            
        } catch (RegistryException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistryException(
                RegistryException.ErrorCodes.UNREGISTRATION_FAILED,
                "Failed to update service instance: " + serviceInstance.getInstanceId(),
                e
            );
        }
    }
    
    @Override
    public void heartbeat(ServiceInstance serviceInstance) throws RegistryException {
        if (serviceInstance == null) {
            throw new RegistryException(
                RegistryException.ErrorCodes.CONFIGURATION_ERROR,
                "Service instance cannot be null"
            );
        }
        
        if (!started.get()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.CONFIGURATION_ERROR,
                "Service registry is not started"
            );
        }
        
        try {
            // 确保实例健康状态
            serviceInstance.setHealthy(true);
            
            // 更新实例信息
            boolean updated = instances.replace(serviceInstance.getInstanceId(), serviceInstance) != null;
            
            if (!updated) {
                // 如果实例不存在，重新注册
                log.warn("Service instance not found for heartbeat, re-registering: {}", 
                        serviceInstance.getInstanceId());
                register(serviceInstance);
            } else {
                log.debug("Heartbeat received for service instance: {}", serviceInstance.getInstanceId());
            }
            
        } catch (RegistryException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistryException(
                RegistryException.ErrorCodes.HEARTBEAT_FAILED,
                "Failed to process heartbeat for service instance: " + serviceInstance.getInstanceId(),
                e
            );
        }
    }
    
    @Override
    public ServiceInstance getInstance(String instanceId) throws RegistryException {
        if (instanceId == null || instanceId.trim().isEmpty()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.CONFIGURATION_ERROR,
                "Instance ID cannot be null or empty"
            );
        }
        
        if (!started.get()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.CONFIGURATION_ERROR,
                "Service registry is not started"
            );
        }
        
        try {
            return instances.get(instanceId.trim());
        } catch (Exception e) {
            throw new RegistryException(
                RegistryException.ErrorCodes.UNKNOWN_ERROR,
                "Failed to get service instance: " + instanceId,
                e
            );
        }
    }
    
    @Override
    public boolean instanceExists(String instanceId) throws RegistryException {
        if (instanceId == null || instanceId.trim().isEmpty()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.CONFIGURATION_ERROR,
                "Instance ID cannot be null or empty"
            );
        }
        
        if (!started.get()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.CONFIGURATION_ERROR,
                "Service registry is not started"
            );
        }
        
        try {
            return instances.containsKey(instanceId.trim());
        } catch (Exception e) {
            throw new RegistryException(
                RegistryException.ErrorCodes.UNKNOWN_ERROR,
                "Failed to check if service instance exists: " + instanceId,
                e
            );
        }
    }
    
    @Override
    public void setInstanceStatus(String instanceId, String status) throws RegistryException {
        if (instanceId == null || instanceId.trim().isEmpty()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.CONFIGURATION_ERROR,
                "Instance ID cannot be null or empty"
            );
        }
        
        if (status == null) {
            throw new RegistryException(
                RegistryException.ErrorCodes.CONFIGURATION_ERROR,
                "Status cannot be null"
            );
        }
        
        if (!started.get()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.CONFIGURATION_ERROR,
                "Service registry is not started"
            );
        }
        
        try {
            ServiceInstance instance = instances.get(instanceId.trim());
            if (instance == null) {
                throw new RegistryException(
                    RegistryException.ErrorCodes.INSTANCE_NOT_FOUND,
                    "Service instance not found: " + instanceId
                );
            }
            
            // 根据状态设置健康状态
            boolean healthy = "UP".equalsIgnoreCase(status);
            String oldStatus = instance.isHealthy() ? "UP" : "DOWN";
            
            instance.setHealthy(healthy);
            
            // 更新实例
            update(instance);
            
            log.info("Updated service instance status: {} from {} to {}", 
                    instanceId, oldStatus, status);
            
        } catch (RegistryException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistryException(
                RegistryException.ErrorCodes.UNKNOWN_ERROR,
                "Failed to update service instance status: " + instanceId,
                e
            );
        }
    }
    
    @Override
    public void start() throws RegistryException {
        if (started.compareAndSet(false, true)) {
            try {
                log.info("Memory service registry started");
            } catch (Exception e) {
                started.set(false);
                throw new RegistryException(
                    RegistryException.ErrorCodes.UNKNOWN_ERROR,
                    "Failed to start memory service registry",
                    e
                );
            }
        }
    }
    
    @Override
    public void stop() throws RegistryException {
        if (started.compareAndSet(true, false)) {
            try {
                instances.clear();
                log.info("Memory service registry stopped");
            } catch (Exception e) {
                started.set(true);
                throw new RegistryException(
                    RegistryException.ErrorCodes.UNKNOWN_ERROR,
                    "Failed to stop memory service registry",
                    e
                );
            }
        }
    }
    
    @Override
    public boolean isStarted() {
        return started.get();
    }
    
    @Override
    public String getRegistryType() {
        return "memory";
    }
    
    @Override
    public String getRegistryAddress() {
        return "memory://local";
    }
    
    /**
     * 获取服务实例数量
     */
    public int getInstanceCount() {
        return instances.size();
    }
    
    @Override
    public String toString() {
        return "MemoryServiceRegistry{" +
                "type=" + getRegistryType() +
                ", address=" + getRegistryAddress() +
                ", instanceCount=" + instances.size() +
                ", started=" + started.get() +
                '}';
    }
} 