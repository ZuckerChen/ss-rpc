package com.ssrpc.registry.factory;

import com.ssrpc.core.spi.ExtensionLoader;
import com.ssrpc.registry.api.ServiceDiscovery;
import com.ssrpc.registry.api.ServiceRegistry;
import com.ssrpc.registry.impl.memory.MemoryServiceDiscovery;
import com.ssrpc.registry.impl.memory.MemoryServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 注册中心工厂.
 * 
 * 基于SPI机制创建和管理注册中心实例
 * 支持多种注册中心实现：内存、ZooKeeper、Nacos等
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class RegistryFactory {
    
    private static final Logger log = LoggerFactory.getLogger(RegistryFactory.class);
    
    /**
     * 注册中心实例缓存
     */
    private static final ConcurrentHashMap<String, ServiceRegistry> registryCache = new ConcurrentHashMap<>();
    
    /**
     * 服务发现实例缓存
     */
    private static final ConcurrentHashMap<String, ServiceDiscovery> discoveryCache = new ConcurrentHashMap<>();
    
    /**
     * 获取服务注册中心实例
     * 
     * @param type 注册中心类型
     * @return 服务注册中心实例
     */
    public static ServiceRegistry getRegistry(String type) {
        if (type == null || type.trim().isEmpty()) {
            type = "memory"; // 默认使用内存注册中心
        }
        
        return registryCache.computeIfAbsent(type, t -> {
            try {
                ExtensionLoader<ServiceRegistry> loader = ExtensionLoader.getExtensionLoader(ServiceRegistry.class);
                ServiceRegistry registry = loader.hasExtension(t) ? loader.getExtension(t) : null;
                if (registry == null) {
                    log.warn("No registry implementation found for type: {}, using memory registry", t);
                    registry = new MemoryServiceRegistry();
                }
                
                log.info("Created registry instance: {} for type: {}", registry.getClass().getSimpleName(), t);
                return registry;
                
            } catch (Exception e) {
                log.error("Failed to create registry for type: {}, using memory registry", t, e);
                return new MemoryServiceRegistry();
            }
        });
    }
    
    /**
     * 获取服务发现实例
     * 
     * @param type 注册中心类型
     * @return 服务发现实例
     */
    public static ServiceDiscovery getDiscovery(String type) {
        if (type == null || type.trim().isEmpty()) {
            type = "memory"; // 默认使用内存服务发现
        }
        
        return discoveryCache.computeIfAbsent(type, t -> {
            try {
                ExtensionLoader<ServiceDiscovery> loader = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class);
                ServiceDiscovery discovery = loader.hasExtension(t) ? loader.getExtension(t) : null;
                if (discovery == null) {
                    log.warn("No discovery implementation found for type: {}, using memory discovery", t);
                    discovery = new MemoryServiceDiscovery();
                }
                
                log.info("Created discovery instance: {} for type: {}", discovery.getClass().getSimpleName(), t);
                return discovery;
                
            } catch (Exception e) {
                log.error("Failed to create discovery for type: {}, using memory discovery", t, e);
                return new MemoryServiceDiscovery();
            }
        });
    }
    
    /**
     * 获取内存注册中心和服务发现的组合实例
     * 
     * @return 内存注册中心实例，其关联的服务发现可通过getServiceDiscovery()获取
     */
    public static MemoryServiceRegistry getMemoryRegistry() {
        ServiceRegistry registry = getRegistry("memory");
        if (registry instanceof MemoryServiceRegistry) {
            return (MemoryServiceRegistry) registry;
        } else {
            // 如果缓存中的不是MemoryServiceRegistry，创建新的
            MemoryServiceRegistry memoryRegistry = new MemoryServiceRegistry();
            registryCache.put("memory", memoryRegistry);
            return memoryRegistry;
        }
    }
    
    /**
     * 创建新的注册中心实例（不使用缓存）
     * 
     * @param type 注册中心类型
     * @return 新的服务注册中心实例
     */
    public static ServiceRegistry createRegistry(String type) {
        if (type == null || type.trim().isEmpty()) {
            type = "memory";
        }
        
        try {
            // 直接创建新实例，不使用SPI缓存
            if ("memory".equals(type)) {
                ServiceRegistry registry = new MemoryServiceRegistry();
                log.info("Created new registry instance: {} for type: {}", registry.getClass().getSimpleName(), type);
                return registry;
            } else {
                // 对于其他类型，尝试通过SPI加载，但这里仍然会使用SPI的缓存
                // 如果需要真正的新实例，需要直接实例化具体类
                ExtensionLoader<ServiceRegistry> loader = ExtensionLoader.getExtensionLoader(ServiceRegistry.class);
                if (loader.hasExtension(type)) {
                    ServiceRegistry registry = loader.getExtension(type);
                    log.warn("SPI extension returns cached instance for type: {}, consider implementing direct instantiation", type);
                    return registry;
                } else {
                    log.warn("No registry implementation found for type: {}, creating memory registry", type);
                    ServiceRegistry registry = new MemoryServiceRegistry();
                    log.info("Created new registry instance: {} for type: {}", registry.getClass().getSimpleName(), type);
                    return registry;
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to create registry for type: {}, creating memory registry", type, e);
            return new MemoryServiceRegistry();
        }
    }
    
    /**
     * 创建新的服务发现实例（不使用缓存）
     * 
     * @param type 注册中心类型
     * @return 新的服务发现实例
     */
    public static ServiceDiscovery createDiscovery(String type) {
        if (type == null || type.trim().isEmpty()) {
            type = "memory";
        }
        
        try {
            // 直接创建新实例，不使用SPI缓存
            if ("memory".equals(type)) {
                ServiceDiscovery discovery = new MemoryServiceDiscovery();
                log.info("Created new discovery instance: {} for type: {}", discovery.getClass().getSimpleName(), type);
                return discovery;
            } else {
                // 对于其他类型，尝试通过SPI加载，但这里仍然会使用SPI的缓存
                // 如果需要真正的新实例，需要直接实例化具体类
                ExtensionLoader<ServiceDiscovery> loader = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class);
                if (loader.hasExtension(type)) {
                    ServiceDiscovery discovery = loader.getExtension(type);
                    log.warn("SPI extension returns cached instance for type: {}, consider implementing direct instantiation", type);
                    return discovery;
                } else {
                    log.warn("No discovery implementation found for type: {}, creating memory discovery", type);
                    ServiceDiscovery discovery = new MemoryServiceDiscovery();
                    log.info("Created new discovery instance: {} for type: {}", discovery.getClass().getSimpleName(), type);
                    return discovery;
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to create discovery for type: {}, creating memory discovery", type, e);
            return new MemoryServiceDiscovery();
        }
    }
    
    /**
     * 清空缓存
     */
    public static void clearCache() {
        // 关闭所有缓存的实例
        registryCache.values().forEach(registry -> {
            try {
                registry.close();
            } catch (Exception e) {
                log.warn("Error closing registry: {}", registry.getClass().getSimpleName(), e);
            }
        });
        
        discoveryCache.values().forEach(discovery -> {
            try {
                discovery.close();
            } catch (Exception e) {
                log.warn("Error closing discovery: {}", discovery.getClass().getSimpleName(), e);
            }
        });
        
        registryCache.clear();
        discoveryCache.clear();
        
        log.info("Registry factory cache cleared");
    }
    
    /**
     * 获取缓存的注册中心数量
     */
    public static int getCachedRegistryCount() {
        return registryCache.size();
    }
    
    /**
     * 获取缓存的服务发现数量
     */
    public static int getCachedDiscoveryCount() {
        return discoveryCache.size();
    }
} 