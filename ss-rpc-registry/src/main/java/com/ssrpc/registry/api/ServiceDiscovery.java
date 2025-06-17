package com.ssrpc.registry.api;

import com.ssrpc.core.spi.SPI;
import com.ssrpc.protocol.ServiceInstance;
import com.ssrpc.registry.exception.RegistryException;

import java.util.List;

/**
 * 服务发现接口.
 * 
 * 提供服务发现、监听、负载均衡等功能
 * 支持多种注册中心实现：内存、ZooKeeper、Nacos等
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@SPI("memory")
public interface ServiceDiscovery {
    
    /**
     * 发现指定服务的所有实例
     * 
     * @param serviceName 服务名称
     * @return 服务实例列表，如果没有找到则返回空列表
     * @throws RegistryException 服务发现异常
     */
    List<ServiceInstance> discover(String serviceName) throws RegistryException;
    
    /**
     * 发现指定服务和版本的所有实例
     * 
     * @param serviceName 服务名称
     * @param version 服务版本
     * @return 服务实例列表，如果没有找到则返回空列表
     * @throws RegistryException 服务发现异常
     */
    List<ServiceInstance> discover(String serviceName, String version) throws RegistryException;
    
    /**
     * 获取所有已注册的服务名称
     * 
     * @return 服务名称列表
     * @throws RegistryException 服务发现异常
     */
    List<String> getServiceNames() throws RegistryException;
    
    /**
     * 添加服务变更监听器
     * 
     * @param serviceName 服务名称
     * @param listener 服务变更监听器
     * @throws RegistryException 服务发现异常
     */
    void addServiceListener(String serviceName, ServiceChangeListener listener) throws RegistryException;
    
    /**
     * 移除服务变更监听器
     * 
     * @param serviceName 服务名称
     * @param listener 服务变更监听器
     * @throws RegistryException 服务发现异常
     */
    void removeServiceListener(String serviceName, ServiceChangeListener listener) throws RegistryException;
    
    /**
     * 获取注册中心类型
     * 
     * @return 注册中心类型
     */
    String getType();
    
    /**
     * 检查服务发现是否可用
     * 
     * @return true表示可用，false表示不可用
     */
    boolean isAvailable();
    
    /**
     * 关闭服务发现，释放资源
     */
    void close();
} 