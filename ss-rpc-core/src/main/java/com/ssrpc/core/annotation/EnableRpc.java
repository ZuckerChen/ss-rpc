package com.ssrpc.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用RPC框架注解.
 * 
 * 标记在Spring Boot主类或配置类上，启用SS-RPC框架功能
 * 包括服务注册、服务发现、代理对象创建等核心功能
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableRpc {
    
    /**
     * 扫描包路径，用于自动发现RPC服务
     * 默认扫描当前类所在包及子包
     */
    String[] scanPackages() default {};
    
    /**
     * 是否启用服务端功能
     * true: 启用服务提供者功能，注册本地服务
     * false: 仅启用客户端功能，消费远程服务
     */
    boolean server() default true;
    
    /**
     * 是否启用客户端功能
     * true: 启用服务消费者功能，创建远程服务代理
     * false: 仅启用服务端功能，提供本地服务
     */
    boolean client() default true;
    
    /**
     * 注册中心地址
     * 支持格式：
     * - zookeeper://127.0.0.1:2181
     * - nacos://127.0.0.1:8848
     * - memory://local (内存注册中心，用于测试)
     */
    String registryAddress() default "";
    
    /**
     * 服务端口，仅在server=true时生效
     * 0表示自动分配可用端口
     */
    int port() default 0;
    
    /**
     * 服务权重，用于负载均衡
     */
    int weight() default 100;
    
    /**
     * 是否启用开发模式
     * 开发模式下会启用热重载、详细日志等调试功能
     */
    boolean devMode() default false;
} 