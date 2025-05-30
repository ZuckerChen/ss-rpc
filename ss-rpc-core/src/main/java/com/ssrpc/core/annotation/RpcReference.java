package com.ssrpc.core.annotation;

import com.ssrpc.core.loadbalance.LoadBalanceType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC服务消费者注解
 * 标记在字段上，表示需要注入远程服务代理
 * 
 * @author SS-RPC Team
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcReference {
    
    /**
     * 服务名称
     */
    String value() default "";
    
    /**
     * 服务版本号
     */
    String version() default "1.0.0";
    
    /**
     * 调用超时时间（毫秒）
     */
    long timeout() default 5000;
    
    /**
     * 重试次数
     */
    int retryTimes() default 3;
    
    /**
     * 负载均衡策略
     */
    LoadBalanceType loadBalance() default LoadBalanceType.RANDOM;
    
    /**
     * 是否异步调用
     */
    boolean async() default false;
} 