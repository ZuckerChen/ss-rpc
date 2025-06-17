package com.ssrpc.registry.api;

import com.ssrpc.protocol.ServiceInstance;

import java.util.List;

/**
 * 服务变更监听器接口.
 * 
 * 用于监听服务实例的注册、注销、更新等变更事件
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public interface ServiceChangeListener {
    
    /**
     * 服务实例注册事件
     * 
     * @param serviceName 服务名称
     * @param instance 新注册的服务实例
     */
    void onServiceRegistered(String serviceName, ServiceInstance instance);
    
    /**
     * 服务实例注销事件
     * 
     * @param serviceName 服务名称
     * @param instance 被注销的服务实例
     */
    void onServiceUnregistered(String serviceName, ServiceInstance instance);
    
    /**
     * 服务实例更新事件
     * 
     * @param serviceName 服务名称
     * @param oldInstance 旧的服务实例
     * @param newInstance 新的服务实例
     */
    void onServiceUpdated(String serviceName, ServiceInstance oldInstance, ServiceInstance newInstance);
    
    /**
     * 服务实例列表变更事件
     * 
     * @param serviceName 服务名称
     * @param instances 当前所有服务实例列表
     */
    void onServiceInstancesChanged(String serviceName, List<ServiceInstance> instances);
    
    /**
     * 服务健康状态变更事件
     * 
     * @param serviceName 服务名称
     * @param instance 服务实例
     * @param healthy 新的健康状态
     */
    void onServiceHealthChanged(String serviceName, ServiceInstance instance, boolean healthy);
} 