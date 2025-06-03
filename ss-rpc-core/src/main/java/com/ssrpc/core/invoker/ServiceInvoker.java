package com.ssrpc.core.invoker;

import com.ssrpc.core.exception.RpcException;

/**
 * 服务调用器接口.
 * 
 * 负责调用本地服务实例的方法
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public interface ServiceInvoker {
    
    /**
     * 调用服务方法
     * 
     * @param methodName 方法名称
     * @param parameterTypes 参数类型数组
     * @param parameters 参数值数组
     * @return 方法调用结果
     * @throws RpcException RPC异常
     */
    Object invoke(String methodName, Class<?>[] parameterTypes, Object[] parameters) throws RpcException;
    
    /**
     * 获取服务接口类型
     * 
     * @return 服务接口类型
     */
    Class<?> getServiceType();
    
    /**
     * 获取服务实现实例
     * 
     * @return 服务实现实例
     */
    Object getServiceInstance();
    
    /**
     * 获取服务名称
     * 
     * @return 服务名称
     */
    String getServiceName();
    
    /**
     * 获取服务版本
     * 
     * @return 服务版本
     */
    String getVersion();
} 