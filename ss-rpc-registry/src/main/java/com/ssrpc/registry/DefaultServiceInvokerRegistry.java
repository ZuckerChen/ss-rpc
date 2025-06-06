package com.ssrpc.registry;

import com.ssrpc.protocol.RpcInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认服务调用器注册实现.
 * 
 * 基于内存的服务调用器注册管理
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class DefaultServiceInvokerRegistry implements ServiceInvokerRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(DefaultServiceInvokerRegistry.class);
    
    private final ConcurrentHashMap<String, RpcInvoker> invokers = new ConcurrentHashMap<>();
    
    @Override
    public void registerInvoker(String serviceName, String version, RpcInvoker invoker) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Service name cannot be null or empty");
        }
        
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("Service version cannot be null or empty");
        }
        
        if (invoker == null) {
            throw new IllegalArgumentException("Service invoker cannot be null");
        }
        
        String serviceKey = buildServiceKey(serviceName, version);
        RpcInvoker oldInvoker = invokers.put(serviceKey, invoker);
        
        if (oldInvoker != null) {
            log.warn("Service invoker replaced for key: {}", serviceKey);
        } else {
            log.info("Service invoker registered for key: {}", serviceKey);
        }
    }
    
    @Override
    public RpcInvoker getInvoker(String serviceName, String version) {
        String serviceKey = buildServiceKey(serviceName, version);
        return invokers.get(serviceKey);
    }
    
    @Override
    public RpcInvoker removeInvoker(String serviceName, String version) {
        String serviceKey = buildServiceKey(serviceName, version);
        RpcInvoker removed = invokers.remove(serviceKey);
        
        if (removed != null) {
            log.info("Service invoker removed for key: {}", serviceKey);
        } else {
            log.debug("No service invoker found to remove for key: {}", serviceKey);
        }
        
        return removed;
    }
    
    @Override
    public boolean containsService(String serviceName, String version) {
        String serviceKey = buildServiceKey(serviceName, version);
        return invokers.containsKey(serviceKey);
    }
    
    @Override
    public int getServiceCount() {
        return invokers.size();
    }
    
    @Override
    public void clear() {
        int count = invokers.size();
        invokers.clear();
        log.info("Cleared {} service invokers", count);
    }
    
    /**
     * 构建服务键
     */
    private String buildServiceKey(String serviceName, String version) {
        if (serviceName == null || version == null) {
            throw new IllegalArgumentException("Service name and version cannot be null");
        }
        return serviceName + ":" + version;
    }
    
    /**
     * 获取所有注册的服务键
     */
    public java.util.Set<String> getAllServiceKeys() {
        return java.util.Collections.unmodifiableSet(invokers.keySet());
    }
    
    @Override
    public String toString() {
        return "DefaultServiceInvokerRegistry{" +
                "serviceCount=" + invokers.size() +
                ", services=" + invokers.keySet() +
                '}';
    }
} 