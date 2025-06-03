package com.ssrpc.core.server;

/**
 * RPC服务器接口
 * 定义RPC服务器的基本功能
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public interface RpcServer {
    
    /**
     * 启动RPC服务器
     */
    void start();
    
    /**
     * 停止RPC服务器
     */
    void stop();
    
    /**
     * 注册服务
     * @param serviceInterface 服务接口
     * @param serviceImpl 服务实现
     */
    void registerService(Class<?> serviceInterface, Object serviceImpl);
    
    /**
     * 获取服务器端口
     * @return 端口号
     */
    int getPort();
    
    /**
     * 检查服务器是否运行中
     * @return true-运行中，false-已停止
     */
    boolean isRunning();
} 