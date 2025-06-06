package com.ssrpc.core.proxy;

import com.ssrpc.core.exception.RpcException;
import com.ssrpc.core.rpc.RpcInvoker;
import com.ssrpc.core.rpc.RpcRequest;
import com.ssrpc.core.rpc.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;

/**
 * JDK动态代理工厂实现.
 * 
 * 基于JDK动态代理创建RPC服务代理对象
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class JdkProxyFactory implements ProxyFactory {
    
    private static final Logger log = LoggerFactory.getLogger(JdkProxyFactory.class);
    
    @Override
    public <T> T createProxy(Class<T> serviceInterface, RpcInvoker invoker) {
        if (!isSupported(serviceInterface)) {
            throw new IllegalArgumentException("Service interface must be an interface: " + serviceInterface);
        }
        
        if (invoker == null) {
            throw new IllegalArgumentException("RPC invoker cannot be null");
        }
        
        return (T) Proxy.newProxyInstance(
            serviceInterface.getClassLoader(),
            new Class[]{serviceInterface},
            new RpcInvocationHandler(serviceInterface, invoker)
        );
    }
    
    @Override
    public boolean isSupported(Class<?> serviceInterface) {
        return serviceInterface != null && serviceInterface.isInterface();
    }
    
    /**
     * RPC调用处理器
     */
    private static class RpcInvocationHandler implements InvocationHandler {
        
        private final Class<?> serviceInterface;
        private final RpcInvoker invoker;
        
        public RpcInvocationHandler(Class<?> serviceInterface, RpcInvoker invoker) {
            this.serviceInterface = serviceInterface;
            this.invoker = invoker;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 处理Object类的方法
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method, args);
            }
            
            // 处理toString方法
            if ("toString".equals(method.getName()) && method.getParameterCount() == 0) {
                return serviceInterface.getName() + "@" + System.identityHashCode(proxy);
            }
            
            // 构造RPC请求
            RpcRequest request = buildRpcRequest(method, args);
            
            // 处理异步调用
            if (method.getReturnType() == CompletableFuture.class) {
                // 对于异步方法，服务端已经等待Future完成并返回实际结果
                // 客户端需要将这个结果重新包装为CompletableFuture
                return invoker.invokeAsync(request)
                        .thenApply(response -> {
                            Object result = extractResult(response);
                            // 服务端已经解析了异步结果，直接返回
                            return result;
                        });
            }
            
            // 同步调用
            RpcResponse response = invoker.invoke(request);
            return extractResult(response);
        }
        
        /**
         * 处理Object类的方法
         */
        private Object handleObjectMethod(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            
            switch (methodName) {
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return serviceInterface.getName() + "@" + System.identityHashCode(proxy);
                default:
                    throw new UnsupportedOperationException("Method not supported: " + methodName);
            }
        }
        
        /**
         * 构造RPC请求
         */
        private RpcRequest buildRpcRequest(Method method, Object[] args) {
            // 获取服务名称（使用接口全限定名）
            String serviceName = serviceInterface.getName();
            
            // 获取方法名称
            String methodName = method.getName();
            
            // 获取参数类型
            Class<?>[] parameterTypes = method.getParameterTypes();
            
            // 获取参数值
            Object[] parameters = args;
            
            // 创建请求
            RpcRequest request = new RpcRequest();
            request.setInterfaceName(serviceName);
            request.setMethodName(methodName);
            request.setParameterTypes(parameterTypes);
            request.setParameters(parameters);
            request.setVersion("1.0.0");
            request.setTimeout(5000L);
            
            log.debug("Built RPC request: {} for method: {}.{}", 
                request.getRequestId(), serviceName, methodName);
            
            return request;
        }
        
        /**
         * 提取响应结果
         */
        private Object extractResult(RpcResponse response) {
            if (response == null) {
                throw new RpcException("RPC response is null");
            }
            
            if (!response.isSuccess()) {
                if (response.getErrorMessage() != null) {
                    throw new RpcException("RPC call failed: " + response.getErrorMessage());
                } else {
                    throw new RpcException("RPC call failed with unknown error");
                }
            }
            
            return response.getResult();
        }
    }
    
    @Override
    public String toString() {
        return "JdkProxyFactory{}";
    }
} 