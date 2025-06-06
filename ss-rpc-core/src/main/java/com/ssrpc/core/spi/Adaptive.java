package com.ssrpc.core.spi;

import java.lang.annotation.*;

/**
 * 自适应扩展注解.
 * 
 * 用于标记自适应扩展方法，支持根据运行时参数动态选择具体实现
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Adaptive {
    
    /**
     * 用于选择扩展的参数名称数组
     * 
     * @return 参数名称数组
     */
    String[] value() default {};
    
    /**
     * 默认扩展名称，当无法确定扩展时使用
     * 
     * @return 默认扩展名称
     */
    String defaultExtension() default "";
} 