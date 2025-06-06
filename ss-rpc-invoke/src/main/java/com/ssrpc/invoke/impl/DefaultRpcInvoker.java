package com.ssrpc.invoke.impl;

import com.ssrpc.core.exception.RpcException;
import com.ssrpc.core.rpc.RpcInvoker;
import com.ssrpc.core.rpc.RpcRequest;
import com.ssrpc.core.rpc.RpcResponse;
import com.ssrpc.transport.api.RpcClient;
import com.ssrpc.core.util.CompletableFutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 默认RPC调用器实现.
 * 
 * 基于网络客户端实现远程服务调用
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class DefaultRpcInvoker implements RpcInvoker {
    
    private static final Logger log = LoggerFactory.getLogger(DefaultRpcInvoker.class);
    
    private final RpcClient rpcClient;
    private final String targetAddress;
    private final long timeout;
    
    public DefaultRpcInvoker(RpcClient rpcClient, String targetAddress, long timeout) {
        this.rpcClient = rpcClient;
        this.targetAddress = targetAddress;
        this.timeout = timeout;
    }
    
    @Override
    public RpcResponse invoke(RpcRequest request) throws RpcException {
        if (!isAvailable()) {
            throw new RpcException("RPC invoker is not available");
        }
        
        try {
            // 设置请求超时
            if (request.getTimeout() <= 0) {
                request.setTimeout(timeout);
            }
            
            log.debug("Invoking RPC request: {} to {}", request.getRequestId(), targetAddress);
            
            // 发送同步请求
            RpcResponse response = rpcClient.sendRequestSync(targetAddress, request);
            
            log.debug("RPC response received: {}", response.getRequestId());
            
            return response;
            
        } catch (Exception e) {
            log.error("Failed to invoke RPC request: {}", request.getRequestId(), e);
            throw new RpcException("RPC invocation failed", e);
        }
    }
    
    @Override
    public CompletableFuture<RpcResponse> invokeAsync(RpcRequest request) {
        if (!isAvailable()) {
            return CompletableFutureUtils.failedFuture(
                new RpcException("RPC invoker is not available"));
        }
        
        try {
            // 设置请求超时
            if (request.getTimeout() <= 0) {
                request.setTimeout(timeout);
            }
            
            log.debug("Invoking async RPC request: {} to {}", request.getRequestId(), targetAddress);
            
            // 发送异步请求
            CompletableFuture<RpcResponse> future = rpcClient.sendRequest(targetAddress, request);
            
            // 添加超时处理和完成回调
            return CompletableFutureUtils.orTimeout(future, timeout, TimeUnit.MILLISECONDS)
                    .whenComplete((response, throwable) -> {
                        if (throwable != null) {
                            log.error("Async RPC request failed: {}", request.getRequestId(), throwable);
                        } else {
                            log.debug("Async RPC response received: {}", response.getRequestId());
                        }
                    });
                    
        } catch (Exception e) {
            log.error("Failed to invoke async RPC request: {}", request.getRequestId(), e);
            return CompletableFutureUtils.failedFuture(new RpcException("Async RPC invocation failed", e));
        }
    }
    
    @Override
    public boolean isAvailable() {
        // 检查RPC客户端是否可用，不要求预先建立连接
        // 连接会在第一次调用时懒加载建立
        return rpcClient != null && 
               rpcClient.isStarted() && 
               targetAddress != null && 
               !targetAddress.trim().isEmpty();
    }
    
    /**
     * 检查是否已连接到目标地址
     * 
     * @return true如果已建立连接且连接活跃
     */
    public boolean isConnected() {
        return isAvailable() && rpcClient.isConnected(targetAddress);
    }
    
    @Override
    public String getTargetAddress() {
        return targetAddress;
    }
    
    @Override
    public void destroy() {
        // 调用器本身不负责关闭客户端，只是标记为不可用
        log.info("RPC invoker destroyed for address: {}", targetAddress);
    }
    
    @Override
    public String toString() {
        return "DefaultRpcInvoker{" +
                "targetAddress='" + targetAddress + '\'' +
                ", timeout=" + timeout +
                ", available=" + isAvailable() +
                '}';
    }
} 