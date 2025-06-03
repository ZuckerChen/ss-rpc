package com.ssrpc.core.invoker;

import com.ssrpc.core.exception.RpcException;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * 基于反射的服务调用器实现.
 * 
 * 通过反射调用本地服务实例的方法
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@Slf4j
public class ReflectionServiceInvoker implements ServiceInvoker {
    
    private final Class<?> serviceType;
    private final Object serviceInstance;
    private final String serviceName;
    private final String version;
    
    // 方法缓存，提高反射调用性能
    private final ConcurrentHashMap<String, Method> methodCache = new ConcurrentHashMap<>();
    
    public ReflectionServiceInvoker(Class<?> serviceType, Object serviceInstance, 
                                  String serviceName, String version) {
        if (serviceType == null) {
            throw new IllegalArgumentException("Service type cannot be null");
        }
        
        if (serviceInstance == null) {
            throw new IllegalArgumentException("Service instance cannot be null");
        }
        
        if (!serviceType.isInstance(serviceInstance)) {
            throw new IllegalArgumentException("Service instance must implement service type");
        }
        
        this.serviceType = serviceType;
        this.serviceInstance = serviceInstance;
        this.serviceName = serviceName != null ? serviceName : serviceType.getName();
        this.version = version != null ? version : "1.0.0";
        
        log.info("Created reflection service invoker for: {}:{}", this.serviceName, this.version);
    }
    
    @Override
    public Object invoke(String methodName, Class<?>[] parameterTypes, Object[] parameters) throws RpcException {
        if (methodName == null || methodName.trim().isEmpty()) {
            throw new RpcException("Method name cannot be null or empty");
        }
        
        try {
            // 获取方法
            Method method = getMethod(methodName, parameterTypes);
            
            // 设置方法可访问
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            
            log.debug("Invoking method: {}.{} on instance: {}", 
                serviceName, methodName, serviceInstance.getClass().getSimpleName());
            
            // 调用方法
            Object result = method.invoke(serviceInstance, parameters);
            
            // 处理异步方法结果 - 如果返回值是CompletableFuture，等待其完成
            if (result instanceof CompletableFuture) {
                CompletableFuture<?> future = (CompletableFuture<?>) result;
                try {
                    // 等待异步结果完成（使用合理的超时时间）
                    result = future.get(10, java.util.concurrent.TimeUnit.SECONDS);
                    log.debug("Async method completed: {}.{}", serviceName, methodName);
                } catch (java.util.concurrent.TimeoutException e) {
                    log.error("Async method timeout: {}.{}", serviceName, methodName, e);
                    throw new RpcException("Async method execution timeout", e);
                } catch (java.util.concurrent.ExecutionException e) {
                    log.error("Async method execution failed: {}.{}", serviceName, methodName, e.getCause());
                    throw new RpcException("Async method execution failed", e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Async method interrupted: {}.{}", serviceName, methodName, e);
                    throw new RpcException("Async method interrupted", e);
                }
            }
            
            log.debug("Method invocation completed: {}.{}", serviceName, methodName);
            
            return result;
            
        } catch (InvocationTargetException e) {
            // 提取真实的异常
            Throwable targetException = e.getTargetException();
            log.error("Method invocation failed: {}.{}", serviceName, methodName, targetException);
            throw new RpcException("Method invocation failed", targetException);
            
        } catch (IllegalAccessException e) {
            log.error("Method access denied: {}.{}", serviceName, methodName, e);
            throw new RpcException("Method access denied", e);
            
        } catch (NoSuchMethodException e) {
            log.error("Method not found: {}.{}", serviceName, methodName, e);
            throw new RpcException("Method not found: " + methodName, e);
            
        } catch (Exception e) {
            log.error("Unexpected error during method invocation: {}.{}", serviceName, methodName, e);
            throw new RpcException("Unexpected error during method invocation", e);
        }
    }
    
    /**
     * 获取方法（带缓存）
     */
    private Method getMethod(String methodName, Class<?>[] parameterTypes) throws NoSuchMethodException {
        String methodKey = buildMethodKey(methodName, parameterTypes);
        
        Method method = methodCache.get(methodKey);
        if (method != null) {
            return method;
        }
        
        // 缓存中没有，进行查找
        method = findMethod(methodName, parameterTypes);
        
        // 缓存方法
        methodCache.put(methodKey, method);
        
        return method;
    }
    
    /**
     * 查找方法
     */
    private Method findMethod(String methodName, Class<?>[] parameterTypes) throws NoSuchMethodException {
        try {
            // 首先尝试精确匹配
            return serviceType.getMethod(methodName, parameterTypes);
            
        } catch (NoSuchMethodException e) {
            // 精确匹配失败，尝试在所有方法中查找兼容的方法
            Method[] methods = serviceType.getMethods();
            
            for (Method method : methods) {
                if (method.getName().equals(methodName) && 
                    isParameterTypesCompatible(method.getParameterTypes(), parameterTypes)) {
                    return method;
                }
            }
            
            // 都找不到，抛出异常
            throw new NoSuchMethodException("Method not found: " + methodName + 
                " with parameters: " + java.util.Arrays.toString(parameterTypes));
        }
    }
    
    /**
     * 检查参数类型是否兼容
     */
    private boolean isParameterTypesCompatible(Class<?>[] methodParamTypes, Class<?>[] requestParamTypes) {
        if (methodParamTypes.length != requestParamTypes.length) {
            return false;
        }
        
        for (int i = 0; i < methodParamTypes.length; i++) {
            if (!isTypeCompatible(methodParamTypes[i], requestParamTypes[i])) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 检查单个类型是否兼容
     */
    private boolean isTypeCompatible(Class<?> methodParamType, Class<?> requestParamType) {
        if (methodParamType.equals(requestParamType)) {
            return true;
        }
        
        // 检查是否可以赋值
        return methodParamType.isAssignableFrom(requestParamType);
    }
    
    /**
     * 构建方法键
     */
    private String buildMethodKey(String methodName, Class<?>[] parameterTypes) {
        StringBuilder keyBuilder = new StringBuilder(methodName);
        keyBuilder.append("(");
        
        if (parameterTypes != null) {
            for (int i = 0; i < parameterTypes.length; i++) {
                if (i > 0) {
                    keyBuilder.append(",");
                }
                keyBuilder.append(parameterTypes[i].getName());
            }
        }
        
        keyBuilder.append(")");
        return keyBuilder.toString();
    }
    
    @Override
    public Class<?> getServiceType() {
        return serviceType;
    }
    
    @Override
    public Object getServiceInstance() {
        return serviceInstance;
    }
    
    @Override
    public String getServiceName() {
        return serviceName;
    }
    
    @Override
    public String getVersion() {
        return version;
    }
    
    /**
     * 获取缓存的方法数量
     */
    public int getCachedMethodCount() {
        return methodCache.size();
    }
    
    /**
     * 清空方法缓存
     */
    public void clearMethodCache() {
        methodCache.clear();
        log.debug("Method cache cleared for service: {}", serviceName);
    }
    
    @Override
    public String toString() {
        return "ReflectionServiceInvoker{" +
                "serviceName='" + serviceName + '\'' +
                ", version='" + version + '\'' +
                ", serviceType=" + serviceType.getName() +
                ", cachedMethods=" + methodCache.size() +
                '}';
    }
} 