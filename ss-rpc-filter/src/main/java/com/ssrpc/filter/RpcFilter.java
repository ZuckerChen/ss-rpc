package com.ssrpc.filter;

/**
 * RPC过滤器接口.
 * 
 * 提供RPC调用过程中的拦截功能，支持请求预处理、响应后处理等
 * 类似于Web框架中的Filter，可以实现日志记录、性能监控、权限验证等功能
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public interface RpcFilter {
    
    /**
     * 过滤器执行方法
     * 
     * @param request RPC请求
     * @param response RPC响应（可能为null，表示还未生成响应）
     * @param chain 过滤器链
     * @return 处理后的响应
     * @throws Exception 处理异常
     */
    Object filter(Object request, Object response, FilterChain chain) throws Exception;
    
    /**
     * 获取过滤器优先级
     * 数值越小优先级越高，越早执行
     * 
     * @return 优先级值
     */
    default int getOrder() {
        return Integer.MAX_VALUE;
    }
    
    /**
     * 过滤器是否启用
     * 
     * @return true表示启用，false表示禁用
     */
    default boolean isEnabled() {
        return true;
    }
    
    /**
     * 过滤器名称
     * 
     * @return 过滤器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * 过滤器链接口
     */
    interface FilterChain {
        
        /**
         * 继续执行过滤器链
         * 
         * @param request RPC请求
         * @param response RPC响应
         * @return 处理后的响应
         * @throws Exception 处理异常
         */
        Object proceed(Object request, Object response) throws Exception;
        
        /**
         * 获取当前过滤器索引
         * 
         * @return 过滤器索引
         */
        int getCurrentIndex();
        
        /**
         * 获取过滤器总数
         * 
         * @return 过滤器总数
         */
        int getFilterCount();
        
        /**
         * 判断是否为最后一个过滤器
         * 
         * @return true表示是最后一个
         */
        default boolean isLast() {
            return getCurrentIndex() >= getFilterCount() - 1;
        }
    }
} 