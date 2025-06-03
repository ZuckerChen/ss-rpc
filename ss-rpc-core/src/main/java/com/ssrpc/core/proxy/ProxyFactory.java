package com.ssrpc.core.proxy;

import com.ssrpc.core.rpc.RpcInvoker;

/**
 * 代理工厂接口.
 * 
 * 用于创建RPC服务的代理对象
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public interface ProxyFactory {
    
    /**
     * 创建服务代理对象
     * 
     * @param serviceInterface 服务接口类型
     * @param invoker RPC调用器
     * @param <T> 服务接口类型
     * @return 代理对象
     */
    <T> T createProxy(Class<T> serviceInterface, RpcInvoker invoker);
    
    /**
     * 检查是否支持指定的接口类型
     * 
     * @param serviceInterface 服务接口类型
     * @return true表示支持，false表示不支持
     */
    boolean isSupported(Class<?> serviceInterface);
} 