package com.ssrpc.registry.impl.memory;

import com.ssrpc.protocol.ServiceInstance;
import com.ssrpc.registry.api.ServiceChangeListener;
import com.ssrpc.registry.api.ServiceDiscovery;
import com.ssrpc.registry.exception.RegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 内存服务发现实现.
 * 
 * 基于内存存储的服务发现实现，适用于测试和单机场景
 * 与MemoryServiceRegistry配合使用
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class MemoryServiceDiscovery implements ServiceDiscovery {
    
    private static final Logger log = LoggerFactory.getLogger(MemoryServiceDiscovery.class);
    
    /**
     * 服务实例存储 - 与MemoryServiceRegistry共享
     */
    private final ConcurrentHashMap<String, ServiceInstance> instances;
    
    /**
     * 服务变更监听器
     * Key: serviceName, Value: 监听器列表
     */
    private final ConcurrentHashMap<String, List<ServiceChangeListener>> listeners = new ConcurrentHashMap<>();
    
    /**
     * 启动状态
     */
    private final AtomicBoolean started = new AtomicBoolean(false);
    
    /**
     * 构造方法 - 与注册中心共享存储
     */
    public MemoryServiceDiscovery(ConcurrentHashMap<String, ServiceInstance> sharedInstances) {
        this.instances = sharedInstances != null ? sharedInstances : new ConcurrentHashMap<>();
    }
    
    /**
     * 构造方法 - 独立存储
     */
    public MemoryServiceDiscovery() {
        this(new ConcurrentHashMap<>());
    }
    
    @Override
    public List<ServiceInstance> discover(String serviceName) throws RegistryException {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.INVALID_PARAMETER,
                "Service name cannot be null or empty"
            );
        }
        
        if (!started.get()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.REGISTRY_UNAVAILABLE,
                "Service discovery is not started"
            );
        }
        
        try {
            List<ServiceInstance> result = instances.values().stream()
                    .filter(instance -> serviceName.equals(instance.getServiceName()))
                    .filter(ServiceInstance::isHealthy)
                    .collect(Collectors.toList());
            
            log.debug("Discovered {} instances for service: {}", result.size(), serviceName);
            return result;
            
        } catch (Exception e) {
            throw new RegistryException(
                RegistryException.ErrorCodes.DISCOVERY_FAILED,
                "Failed to discover service: " + serviceName,
                e
            );
        }
    }
    
    @Override
    public List<ServiceInstance> discover(String serviceName, String version) throws RegistryException {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.INVALID_PARAMETER,
                "Service name cannot be null or empty"
            );
        }
        
        if (version == null || version.trim().isEmpty()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.INVALID_PARAMETER,
                "Service version cannot be null or empty"
            );
        }
        
        if (!started.get()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.REGISTRY_UNAVAILABLE,
                "Service discovery is not started"
            );
        }
        
        try {
            List<ServiceInstance> result = instances.values().stream()
                    .filter(instance -> serviceName.equals(instance.getServiceName()))
                    .filter(instance -> version.equals(instance.getVersion()))
                    .filter(ServiceInstance::isHealthy)
                    .collect(Collectors.toList());
            
            log.debug("Discovered {} instances for service: {}:{}", result.size(), serviceName, version);
            return result;
            
        } catch (Exception e) {
            throw new RegistryException(
                RegistryException.ErrorCodes.DISCOVERY_FAILED,
                "Failed to discover service: " + serviceName + ":" + version,
                e
            );
        }
    }
    
    @Override
    public List<String> getServiceNames() throws RegistryException {
        if (!started.get()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.REGISTRY_UNAVAILABLE,
                "Service discovery is not started"
            );
        }
        
        try {
            Set<String> serviceNames = instances.values().stream()
                    .filter(ServiceInstance::isHealthy)
                    .map(ServiceInstance::getServiceName)
                    .collect(Collectors.toSet());
            
            List<String> result = new ArrayList<>(serviceNames);
            log.debug("Found {} unique service names", result.size());
            return result;
            
        } catch (Exception e) {
            throw new RegistryException(
                RegistryException.ErrorCodes.DISCOVERY_FAILED,
                "Failed to get service names",
                e
            );
        }
    }
    
    @Override
    public void addServiceListener(String serviceName, ServiceChangeListener listener) throws RegistryException {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.INVALID_PARAMETER,
                "Service name cannot be null or empty"
            );
        }
        
        if (listener == null) {
            throw new RegistryException(
                RegistryException.ErrorCodes.INVALID_PARAMETER,
                "Service change listener cannot be null"
            );
        }
        
        listeners.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>()).add(listener);
        log.info("Added service listener for service: {}", serviceName);
    }
    
    @Override
    public void removeServiceListener(String serviceName, ServiceChangeListener listener) throws RegistryException {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new RegistryException(
                RegistryException.ErrorCodes.INVALID_PARAMETER,
                "Service name cannot be null or empty"
            );
        }
        
        if (listener == null) {
            throw new RegistryException(
                RegistryException.ErrorCodes.INVALID_PARAMETER,
                "Service change listener cannot be null"
            );
        }
        
        List<ServiceChangeListener> serviceListeners = listeners.get(serviceName);
        if (serviceListeners != null) {
            boolean removed = serviceListeners.remove(listener);
            if (removed) {
                log.info("Removed service listener for service: {}", serviceName);
            }
            
            // 如果没有监听器了，移除整个条目
            if (serviceListeners.isEmpty()) {
                listeners.remove(serviceName);
            }
        }
    }
    
    /**
     * 通知服务变更事件 - 由MemoryServiceRegistry调用
     */
    public void notifyServiceRegistered(ServiceInstance instance) {
        String serviceName = instance.getServiceName();
        List<ServiceChangeListener> serviceListeners = listeners.get(serviceName);
        
        if (serviceListeners != null && !serviceListeners.isEmpty()) {
            for (ServiceChangeListener listener : serviceListeners) {
                try {
                    listener.onServiceRegistered(serviceName, instance);
                } catch (Exception e) {
                    log.warn("Error notifying service registered event for service: {}", serviceName, e);
                }
            }
            
            // 通知实例列表变更
            try {
                List<ServiceInstance> currentInstances = discover(serviceName);
                for (ServiceChangeListener listener : serviceListeners) {
                    try {
                        listener.onServiceInstancesChanged(serviceName, currentInstances);
                    } catch (Exception e) {
                        log.warn("Error notifying service instances changed event for service: {}", serviceName, e);
                    }
                }
            } catch (RegistryException e) {
                log.warn("Error getting current instances for service: {}", serviceName, e);
            }
        }
    }
    
    /**
     * 通知服务注销事件 - 由MemoryServiceRegistry调用
     */
    public void notifyServiceUnregistered(ServiceInstance instance) {
        String serviceName = instance.getServiceName();
        List<ServiceChangeListener> serviceListeners = listeners.get(serviceName);
        
        if (serviceListeners != null && !serviceListeners.isEmpty()) {
            for (ServiceChangeListener listener : serviceListeners) {
                try {
                    listener.onServiceUnregistered(serviceName, instance);
                } catch (Exception e) {
                    log.warn("Error notifying service unregistered event for service: {}", serviceName, e);
                }
            }
            
            // 通知实例列表变更
            try {
                List<ServiceInstance> currentInstances = discover(serviceName);
                for (ServiceChangeListener listener : serviceListeners) {
                    try {
                        listener.onServiceInstancesChanged(serviceName, currentInstances);
                    } catch (Exception e) {
                        log.warn("Error notifying service instances changed event for service: {}", serviceName, e);
                    }
                }
            } catch (RegistryException e) {
                log.warn("Error getting current instances for service: {}", serviceName, e);
            }
        }
    }
    
    /**
     * 通知服务更新事件 - 由MemoryServiceRegistry调用
     */
    public void notifyServiceUpdated(ServiceInstance oldInstance, ServiceInstance newInstance) {
        String serviceName = newInstance.getServiceName();
        List<ServiceChangeListener> serviceListeners = listeners.get(serviceName);
        
        if (serviceListeners != null && !serviceListeners.isEmpty()) {
            for (ServiceChangeListener listener : serviceListeners) {
                try {
                    listener.onServiceUpdated(serviceName, oldInstance, newInstance);
                } catch (Exception e) {
                    log.warn("Error notifying service updated event for service: {}", serviceName, e);
                }
            }
        }
    }
    
    /**
     * 通知健康状态变更事件 - 由MemoryServiceRegistry调用
     */
    public void notifyServiceHealthChanged(ServiceInstance instance, boolean healthy) {
        String serviceName = instance.getServiceName();
        List<ServiceChangeListener> serviceListeners = listeners.get(serviceName);
        
        if (serviceListeners != null && !serviceListeners.isEmpty()) {
            for (ServiceChangeListener listener : serviceListeners) {
                try {
                    listener.onServiceHealthChanged(serviceName, instance, healthy);
                } catch (Exception e) {
                    log.warn("Error notifying service health changed event for service: {}", serviceName, e);
                }
            }
        }
    }
    
    /**
     * 启动服务发现
     */
    public void start() throws RegistryException {
        if (started.compareAndSet(false, true)) {
            try {
                log.info("Memory service discovery started");
            } catch (Exception e) {
                started.set(false);
                throw new RegistryException(
                    RegistryException.ErrorCodes.UNKNOWN_ERROR,
                    "Failed to start memory service discovery",
                    e
                );
            }
        }
    }
    
    /**
     * 停止服务发现
     */
    public void stop() throws RegistryException {
        if (started.compareAndSet(true, false)) {
            try {
                listeners.clear();
                log.info("Memory service discovery stopped");
            } catch (Exception e) {
                started.set(true);
                throw new RegistryException(
                    RegistryException.ErrorCodes.UNKNOWN_ERROR,
                    "Failed to stop memory service discovery",
                    e
                );
            }
        }
    }
    
    /**
     * 检查是否已启动
     */
    public boolean isStarted() {
        return started.get();
    }
    
    @Override
    public String getType() {
        return "memory";
    }
    
    @Override
    public boolean isAvailable() {
        return started.get();
    }
    
    @Override
    public void close() {
        try {
            stop();
        } catch (RegistryException e) {
            log.warn("Error occurred while closing service discovery", e);
        }
    }
    
    /**
     * 获取监听器数量
     */
    public int getListenerCount() {
        return listeners.values().stream().mapToInt(List::size).sum();
    }
    
    /**
     * 获取服务实例数量
     */
    public int getInstanceCount() {
        return instances.size();
    }
    
    @Override
    public String toString() {
        return "MemoryServiceDiscovery{" +
                "instanceCount=" + instances.size() +
                ", listenerCount=" + getListenerCount() +
                ", started=" + started.get() +
                '}';
    }
} 