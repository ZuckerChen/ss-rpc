package com.ssrpc.core.spi;

import java.lang.annotation.*;

/**
 * SPI (Service Provider Interface) 注解.
 * 
 * 用于标记可扩展的接口，支持插件化扩展
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SPI {
    
    /**
     * 默认扩展名称
     * 
     * @return 默认扩展名称
     */
    String value() default "";
    
    /**
     * 是否为单例模式
     * 
     * @return true表示单例，false表示每次创建新实例
     */
    boolean singleton() default true;
    
    /**
     * 扩展范围，用于分组管理
     * 
     * @return 扩展范围
     */
    String scope() default "default";
} 