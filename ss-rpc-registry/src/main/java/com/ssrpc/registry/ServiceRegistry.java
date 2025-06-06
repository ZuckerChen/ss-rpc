package com.ssrpc.registry;

import com.ssrpc.protocol.ServiceInstance;

/**
 * 服务注册接口.
 * 
 * 提供服务注册、注销、健康检查等功能
 * 支持多种注册中心实现：内存、ZooKeeper、Nacos等
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public interface ServiceRegistry {
    
    /**
     * 注册服务实例
     * 
     * @param serviceInstance 服务实例信息
     * @throws RegistryException 服务注册异常
     */
    void register(ServiceInstance serviceInstance) throws RegistryException;
    
    /**
     * 注销服务实例
     * 
     * @param serviceInstance 服务实例信息
     * @throws RegistryException 服务注册异常
     */
    void unregister(ServiceInstance serviceInstance) throws RegistryException;
    
    /**
     * 更新服务实例信息
     * 
     * @param serviceInstance 服务实例信息
     * @throws RegistryException 服务注册异常
     */
    void update(ServiceInstance serviceInstance) throws RegistryException;
    
    /**
     * 发送心跳保持服务实例活跃状态
     * 
     * @param serviceInstance 服务实例信息
     * @throws RegistryException 服务注册异常
     */
    void heartbeat(ServiceInstance serviceInstance) throws RegistryException;
    
    /**
     * 获取指定服务实例的详细信息
     * 
     * @param instanceId 实例ID
     * @return 服务实例信息，如果不存在则返回null
     * @throws RegistryException 服务注册异常
     */
    ServiceInstance getInstance(String instanceId) throws RegistryException;
    
    /**
     * 检查服务实例是否存在
     * 
     * @param instanceId 实例ID
     * @return true表示存在，false表示不存在
     * @throws RegistryException 服务注册异常
     */
    boolean instanceExists(String instanceId) throws RegistryException;
    
    /**
     * 设置服务实例状态
     * 
     * @param instanceId 实例ID
     * @param status 新状态 (UP/DOWN/UNKNOWN)
     * @throws RegistryException 服务注册异常
     */
    void setInstanceStatus(String instanceId, String status) throws RegistryException;
    
    /**
     * 启动服务注册中心
     * 
     * @throws RegistryException 启动异常
     */
    void start() throws RegistryException;
    
    /**
     * 停止服务注册中心
     * 
     * @throws RegistryException 停止异常
     */
    void stop() throws RegistryException;
    
    /**
     * 检查服务注册中心是否已启动
     * 
     * @return true表示已启动，false表示未启动
     */
    boolean isStarted();
    
    /**
     * 获取注册中心类型
     * 
     * @return 注册中心类型
     */
    String getRegistryType();
    
    /**
     * 获取注册中心地址
     * 
     * @return 注册中心地址
     */
    String getRegistryAddress();
} 