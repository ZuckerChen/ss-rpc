package com.ssrpc.core.spi.impl;

import com.ssrpc.core.spi.TestInterface;

/**
 * 默认测试实现
 *
 * @author chenzhang
 * @since 1.0.0
 */
public class DefaultTestImpl implements TestInterface {
    
    @Override
    public String test() {
        return "Default implementation test result";
    }
    
    @Override
    public String getName() {
        return "DefaultTestImpl";
    }
} 