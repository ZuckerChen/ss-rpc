package com.ssrpc.core.invoker;

import com.ssrpc.core.exception.RpcException;
import com.ssrpc.core.rpc.RpcRequest;
import com.ssrpc.core.rpc.RpcResponse;
import com.ssrpc.protocol.RpcInvoker;

import java.util.concurrent.CompletableFuture;

/**
 * ServiceInvoker到RpcInvoker的适配器.
 * 
 * 将本地服务调用器包装为RPC调用器接口
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class ServiceInvokerAdapter implements RpcInvoker {
    
    private final ServiceInvoker serviceInvoker;
    
    public ServiceInvokerAdapter(ServiceInvoker serviceInvoker) {
        this.serviceInvoker = serviceInvoker;
    }
    
    @Override
    public com.ssrpc.protocol.RpcResponse invoke(com.ssrpc.protocol.RpcRequest request) throws Exception {
        try {
            // 调用本地服务
            Object result = serviceInvoker.invoke(
                request.getMethodName(),
                request.getParameterTypes(),
                request.getParameters()
            );
            
            // 创建成功响应
            return com.ssrpc.protocol.RpcResponse.success(request.getRequestId(), result);
            
        } catch (Exception e) {
            // 创建错误响应
            return com.ssrpc.protocol.RpcResponse.error(request.getRequestId(), e);
        }
    }
    
    @Override
    public CompletableFuture<com.ssrpc.protocol.RpcResponse> invokeAsync(com.ssrpc.protocol.RpcRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return invoke(request);
            } catch (Exception e) {
                return com.ssrpc.protocol.RpcResponse.error(request.getRequestId(), e);
            }
        });
    }
    
    @Override
    public boolean isAvailable() {
        return serviceInvoker != null;
    }
    
    @Override
    public String getTargetAddress() {
        return "local://" + serviceInvoker.getServiceName();
    }
    
    @Override
    public void destroy() {
        // 本地服务调用器通常不需要销毁
    }
    
    public ServiceInvoker getServiceInvoker() {
        return serviceInvoker;
    }
} 