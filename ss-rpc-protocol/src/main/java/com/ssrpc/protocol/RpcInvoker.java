package com.ssrpc.protocol;

import java.util.concurrent.CompletableFuture;

/**
 * RPC调用器接口.
 * 
 * 负责执行远程RPC调用，支持同步和异步调用
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public interface RpcInvoker {
    
    /**
     * 同步调用RPC方法
     * 
     * @param request RPC请求
     * @return RPC响应
     * @throws Exception RPC调用异常
     */
    RpcResponse invoke(RpcRequest request) throws Exception;
    
    /**
     * 异步调用RPC方法
     * 
     * @param request RPC请求
     * @return RPC响应的CompletableFuture
     */
    CompletableFuture<RpcResponse> invokeAsync(RpcRequest request);
    
    /**
     * 检查调用器是否可用
     * 
     * @return true表示可用，false表示不可用
     */
    boolean isAvailable();
    
    /**
     * 获取目标地址
     * 
     * @return 目标地址
     */
    String getTargetAddress();
    
    /**
     * 销毁调用器，释放资源
     */
    void destroy();
} 