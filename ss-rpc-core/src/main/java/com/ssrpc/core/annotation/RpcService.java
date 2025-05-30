package com.ssrpc.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC服务提供者注解
 * 标记在服务实现类上，表示该类提供RPC服务
 * 
 * @author SS-RPC Team
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcService {
    
    /**
     * 服务名称，默认为接口名
     */
    String value() default "";
    
    /**
     * 服务版本号
     */
    String version() default "1.0.0";
    
    /**
     * 服务权重，用于负载均衡
     */
    int weight() default 100;
    
    /**
     * 是否启用热重载（仅开发环境）
     */
    boolean hotReload() default false;
} 