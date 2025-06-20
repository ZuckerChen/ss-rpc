package com.ssrpc.spring.boot.autoconfigure;

import com.ssrpc.core.annotation.RpcReference;
import com.ssrpc.loadbalance.LoadBalancer;
import com.ssrpc.loadbalance.LoadBalancerFactory;
import com.ssrpc.protocol.RpcRequest;
import com.ssrpc.protocol.RpcResponse;
import com.ssrpc.protocol.ServiceInstance;
import com.ssrpc.registry.api.ServiceDiscovery;
import com.ssrpc.registry.exception.RegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * RPC调用处理器
 * 实现动态代理，处理RPC方法调用
 * 
 * @author shuang.kou
 * @since 1.0.0
 */
public class RpcInvocationHandler implements InvocationHandler {
    
    private static final Logger log = LoggerFactory.getLogger(RpcInvocationHandler.class);
    
    private final Class<?> interfaceClass;
    private final RpcReference rpcReference;
    private final ServiceDiscovery serviceDiscovery;
    private final RpcProperties rpcProperties;
    private final LoadBalancer loadBalancer;
    
    public RpcInvocationHandler(Class<?> interfaceClass, RpcReference rpcReference, 
                               ServiceDiscovery serviceDiscovery, RpcProperties rpcProperties) {
        this.interfaceClass = interfaceClass;
        this.rpcReference = rpcReference;
        this.serviceDiscovery = serviceDiscovery;
        this.rpcProperties = rpcProperties;
        this.loadBalancer = LoadBalancerFactory.getLoadBalancer(rpcProperties.getLoadBalance().getAlgorithm());
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 处理Object类的方法
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }
        
        // 构建RPC请求
        RpcRequest request = buildRpcRequest(method, args);
        
        // 服务发现
        List<ServiceInstance> instances = discoverServices();
        if (instances.isEmpty()) {
            throw new RuntimeException("No available service instances for: " + interfaceClass.getName());
        }
        
        // 负载均衡选择实例
        ServiceInstance selectedInstance = loadBalancer.select(instances);
        if (selectedInstance == null) {
            throw new RuntimeException("Load balancer failed to select service instance");
        }
        
        // 发送RPC请求
        return sendRpcRequest(request, selectedInstance);
    }
    
    /**
     * 构建RPC请求
     */
    private RpcRequest buildRpcRequest(Method method, Object[] args) {
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setInterfaceName(interfaceClass.getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);
        request.setVersion(rpcReference.version());
        request.setTimeout(rpcReference.timeout());
        
        return request;
    }
    
    /**
     * 服务发现
     */
    private List<ServiceInstance> discoverServices() throws RegistryException {
        String serviceName = interfaceClass.getName();
        String version = rpcReference.version();
        
        log.debug("Discovering services for: {} with version: {}", serviceName, version);
        
        List<ServiceInstance> instances = serviceDiscovery.discover(serviceName, version);
        
        log.debug("Found {} service instances for: {}", instances.size(), serviceName);
        
        return instances;
    }
    
    /**
     * 发送RPC请求
     */
    private Object sendRpcRequest(RpcRequest request, ServiceInstance instance) throws Exception {
        log.debug("Sending RPC request to: {}:{} for method: {}", 
                instance.getHost(), instance.getPort(), request.getMethodName());
        
        // 这里应该使用实际的网络客户端发送请求
        // 目前先返回模拟响应，后续需要集成实际的网络传输层
        
        if (rpcReference.async()) {
            // 异步调用
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return doSyncCall(request, instance);
                } catch (Exception e) {
                    throw new RuntimeException("Async RPC call failed", e);
                }
            });
        } else {
            // 同步调用
            return doSyncCall(request, instance);
        }
    }
    
    /**
     * 执行同步调用
     */
    private Object doSyncCall(RpcRequest request, ServiceInstance instance) throws Exception {
        // TODO: 这里需要集成实际的网络传输层
        // 目前返回模拟响应
        
        log.warn("RPC call simulation - actual network transport not implemented yet");
        
        // 模拟网络延迟
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 创建模拟响应
        RpcResponse response = new RpcResponse();
        response.setRequestId(request.getRequestId());
        response.setStatus(RpcResponse.SUCCESS);
        
        // 根据方法返回类型返回默认值
        Class<?> returnType = getMethodReturnType(request);
        if (returnType == void.class || returnType == Void.class) {
            response.setResult(null);
        } else if (returnType.isPrimitive()) {
            response.setResult(getDefaultPrimitiveValue(returnType));
        } else {
            response.setResult(null);
        }
        
        if (response.isSuccess()) {
            return response.getResult();
        } else {
            throw new RuntimeException("RPC call failed: " + response.getErrorMessage());
        }
    }
    
    /**
     * 获取方法返回类型
     */
    private Class<?> getMethodReturnType(RpcRequest request) {
        try {
            Method method = interfaceClass.getMethod(request.getMethodName(), request.getParameterTypes());
            return method.getReturnType();
        } catch (NoSuchMethodException e) {
            log.warn("Failed to get method return type for: {}", request.getMethodName());
            return Object.class;
        }
    }
    
    /**
     * 获取基本类型的默认值
     */
    private Object getDefaultPrimitiveValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        if (type == char.class) return '\0';
        return null;
    }
} 