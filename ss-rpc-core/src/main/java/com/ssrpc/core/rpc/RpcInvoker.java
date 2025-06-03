package com.ssrpc.core.rpc;

import com.ssrpc.core.exception.RpcException;

/**
 * RPC调用器接口.
 * 
 * 定义RPC调用的核心方法，用于发起远程调用
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public interface RpcInvoker {
    
    /**
     * 执行RPC调用
     * 
     * @param request RPC请求
     * @return RPC响应
     * @throws RpcException RPC异常
     */
    RpcResponse invoke(RpcRequest request) throws RpcException;
    
    /**
     * 执行异步RPC调用
     * 
     * @param request RPC请求
     * @return RPC响应的CompletableFuture
     */
    java.util.concurrent.CompletableFuture<RpcResponse> invokeAsync(RpcRequest request);
    
    /**
     * 检查调用器是否可用
     * 
     * @return true表示可用，false表示不可用
     */
    boolean isAvailable();
    
    /**
     * 获取目标服务地址
     * 
     * @return 服务地址
     */
    String getTargetAddress();
    
    /**
     * 销毁调用器，释放资源
     */
    void destroy();
} 