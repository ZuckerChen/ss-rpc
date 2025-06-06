package com.ssrpc.transport.api;

import com.ssrpc.transport.exception.TransportException;

/**
 * RPC服务端接口.
 * 
 * 定义RPC服务端的基本功能，包括启动、停止、服务注册等
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public interface RpcServer {
    
    /**
     * 启动服务端
     * 
     * @param port 监听端口
     * @throws TransportException 传输异常
     */
    void start(int port) throws TransportException;
    
    /**
     * 停止服务端
     */
    void shutdown();
    
    /**
     * 获取服务端运行状态
     * 
     * @return true表示正在运行，false表示已停止
     */
    boolean isRunning();
    
    /**
     * 获取服务端监听端口
     * 
     * @return 监听端口，如果未启动返回-1
     */
    int getPort();
} 