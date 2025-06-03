package com.ssrpc.core.network;

import com.ssrpc.core.exception.NetworkException;

/**
 * RPC服务器接口.
 * 
 * 定义RPC服务器的基本功能
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public interface RpcServer {
    
    /**
     * 启动服务端
     * 
     * @param port 监听端口
     * @throws NetworkException 网络异常
     */
    void start(int port) throws NetworkException;
    
    /**
     * 停止服务端
     */
    void shutdown();
    
    /**
     * 获取服务端状态
     * 
     * @return true表示已启动，false表示未启动
     */
    boolean isStarted();
    
    /**
     * 获取服务端监听端口
     * 
     * @return 监听端口，如果未启动则返回-1
     */
    int getPort();
} 