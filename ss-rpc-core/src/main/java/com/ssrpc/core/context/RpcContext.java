package com.ssrpc.core.context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC调用上下文.
 * 
 * 用于在RPC调用过程中传递上下文信息，如调用链路追踪、用户信息等
 * 基于ThreadLocal实现，确保线程安全
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class RpcContext {
    
    /**
     * 本地上下文存储 - 当前线程可见
     */
    private static final ThreadLocal<RpcContext> LOCAL_CONTEXT = new ThreadLocal<RpcContext>() {
        @Override
        protected RpcContext initialValue() {
            return new RpcContext();
        }
    };
    
    /**
     * 全局上下文存储 - 所有线程共享
     */
    private static final Map<String, Object> GLOBAL_CONTEXT = new ConcurrentHashMap<>();
    
    /**
     * 当前线程的上下文属性
     */
    private final Map<String, Object> attributes = new HashMap<>();
    
    /**
     * 当前请求的唯一标识
     */
    private String requestId;
    
    /**
     * 调用开始时间
     */
    private long startTime = System.currentTimeMillis();
    
    /**
     * 远程服务地址
     */
    private String remoteAddress;
    
    /**
     * 本地服务地址
     */
    private String localAddress;
    
    /**
     * 服务接口名称
     */
    private String serviceName;
    
    /**
     * 调用方法名称
     */
    private String methodName;
    
    /**
     * 是否异步调用
     */
    private boolean async = false;
    
    /**
     * 获取当前线程的RPC上下文
     */
    public static RpcContext current() {
        return LOCAL_CONTEXT.get();
    }
    
    /**
     * 清除当前线程的RPC上下文
     */
    public static void clear() {
        LOCAL_CONTEXT.remove();
    }
    
    /**
     * 设置请求ID
     */
    public RpcContext setRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
    
    /**
     * 获取请求ID
     */
    public String getRequestId() {
        return requestId;
    }
    
    /**
     * 设置远程地址
     */
    public RpcContext setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
        return this;
    }
    
    /**
     * 获取远程地址
     */
    public String getRemoteAddress() {
        return remoteAddress;
    }
    
    /**
     * 设置本地地址
     */
    public RpcContext setLocalAddress(String localAddress) {
        this.localAddress = localAddress;
        return this;
    }
    
    /**
     * 获取本地地址
     */
    public String getLocalAddress() {
        return localAddress;
    }
    
    /**
     * 设置服务名称
     */
    public RpcContext setServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }
    
    /**
     * 获取服务名称
     */
    public String getServiceName() {
        return serviceName;
    }
    
    /**
     * 设置方法名称
     */
    public RpcContext setMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }
    
    /**
     * 获取方法名称
     */
    public String getMethodName() {
        return methodName;
    }
    
    /**
     * 设置异步标识
     */
    public RpcContext setAsync(boolean async) {
        this.async = async;
        return this;
    }
    
    /**
     * 是否异步调用
     */
    public boolean isAsync() {
        return async;
    }
    
    /**
     * 获取调用开始时间
     */
    public long getStartTime() {
        return startTime;
    }
    
    /**
     * 重置开始时间（用于重试等场景）
     */
    public RpcContext resetStartTime() {
        this.startTime = System.currentTimeMillis();
        return this;
    }
    
    /**
     * 获取调用耗时（毫秒）
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * 设置上下文属性
     */
    public RpcContext setAttribute(String key, Object value) {
        attributes.put(key, value);
        return this;
    }
    
    /**
     * 获取上下文属性
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }
    
    /**
     * 获取上下文属性（带默认值）
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, T defaultValue) {
        T value = (T) attributes.get(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 移除上下文属性
     */
    public Object removeAttribute(String key) {
        return attributes.remove(key);
    }
    
    /**
     * 获取所有属性
     */
    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }
    
    /**
     * 设置全局上下文属性（所有线程共享）
     */
    public static void setGlobalAttribute(String key, Object value) {
        GLOBAL_CONTEXT.put(key, value);
    }
    
    /**
     * 获取全局上下文属性
     */
    @SuppressWarnings("unchecked")
    public static <T> T getGlobalAttribute(String key) {
        return (T) GLOBAL_CONTEXT.get(key);
    }
    
    /**
     * 移除全局上下文属性
     */
    public static Object removeGlobalAttribute(String key) {
        return GLOBAL_CONTEXT.remove(key);
    }
    
    /**
     * 清空全局上下文
     */
    public static void clearGlobalContext() {
        GLOBAL_CONTEXT.clear();
    }
    
    /**
     * 复制当前上下文到新的上下文对象
     */
    public RpcContext copy() {
        RpcContext newContext = new RpcContext();
        newContext.requestId = this.requestId;
        newContext.remoteAddress = this.remoteAddress;
        newContext.localAddress = this.localAddress;
        newContext.serviceName = this.serviceName;
        newContext.methodName = this.methodName;
        newContext.async = this.async;
        newContext.startTime = this.startTime;
        newContext.attributes.putAll(this.attributes);
        return newContext;
    }
    
    @Override
    public String toString() {
        return "RpcContext{" +
                "requestId='" + requestId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", remoteAddress='" + remoteAddress + '\'' +
                ", localAddress='" + localAddress + '\'' +
                ", async=" + async +
                ", elapsedTime=" + getElapsedTime() + "ms" +
                ", attributes=" + attributes.size() +
                '}';
    }
} 