package com.ssrpc.core.registry;

import com.ssrpc.core.invoker.ServiceInvoker;

/**
 * 服务调用器注册表接口.
 * 
 * 管理本地注册的服务调用器
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public interface ServiceInvokerRegistry {
    
    /**
     * 注册服务调用器
     * 
     * @param serviceName 服务名称
     * @param version 服务版本
     * @param invoker 服务调用器
     */
    void registerInvoker(String serviceName, String version, ServiceInvoker invoker);
    
    /**
     * 获取服务调用器
     * 
     * @param serviceName 服务名称
     * @param version 服务版本
     * @return 服务调用器，如果不存在则返回null
     */
    ServiceInvoker getInvoker(String serviceName, String version);
    
    /**
     * 移除服务调用器
     * 
     * @param serviceName 服务名称
     * @param version 服务版本
     * @return 被移除的服务调用器，如果不存在则返回null
     */
    ServiceInvoker removeInvoker(String serviceName, String version);
    
    /**
     * 检查服务是否已注册
     * 
     * @param serviceName 服务名称
     * @param version 服务版本
     * @return true表示已注册，false表示未注册
     */
    boolean containsService(String serviceName, String version);
    
    /**
     * 获取已注册的服务数量
     * 
     * @return 服务数量
     */
    int getServiceCount();
    
    /**
     * 清空所有注册的服务
     */
    void clear();
} 