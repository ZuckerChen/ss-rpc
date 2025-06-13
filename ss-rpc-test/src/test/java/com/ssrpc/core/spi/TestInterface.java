package com.ssrpc.core.spi;

import com.ssrpc.core.spi.SPI;

/**
 * SPI测试接口
 *
 * @author chenzhang
 * @since 1.0.0
 */
@SPI("default")
public interface TestInterface {
    
    /**
     * 测试方法
     *
     * @return 测试结果
     */
    String test();
    
    /**
     * 获取实现名称
     *
     * @return 实现名称
     */
    String getName();
} 