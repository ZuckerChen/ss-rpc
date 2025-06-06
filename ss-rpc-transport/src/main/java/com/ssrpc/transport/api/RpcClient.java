package com.ssrpc.transport.api;

import com.ssrpc.transport.exception.TransportException;
import com.ssrpc.core.rpc.RpcRequest;
import com.ssrpc.core.rpc.RpcResponse;

import java.util.concurrent.CompletableFuture;

/**
 * RPC客户端接口.
 * 
 * 定义RPC客户端的基本功能，包括发送请求、管理连接等
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public interface RpcClient {
    
    /**
     * 启动客户端
     * 
     * @throws TransportException 传输异常
     */
    void start() throws TransportException;
    
    /**
     * 停止客户端
     */
    void shutdown();
    
    /**
     * 发送RPC请求（异步）
     * 
     * @param address 服务端地址（格式：host:port）
     * @param request RPC请求
     * @return 响应的CompletableFuture
     */
    CompletableFuture<RpcResponse> sendRequest(String address, RpcRequest request);
    
    /**
     * 发送RPC请求（同步）
     * 
     * @param address 服务端地址（格式：host:port）
     * @param request RPC请求
     * @return RPC响应
     * @throws TransportException 传输异常
     */
    RpcResponse sendRequestSync(String address, RpcRequest request) throws TransportException;
    
    /**
     * 检查与指定地址的连接是否可用
     * 
     * @param address 服务端地址
     * @return true表示连接可用，false表示不可用
     */
    boolean isConnected(String address);
    
    /**
     * 获取客户端状态
     * 
     * @return true表示已启动，false表示未启动
     */
    boolean isStarted();
} 