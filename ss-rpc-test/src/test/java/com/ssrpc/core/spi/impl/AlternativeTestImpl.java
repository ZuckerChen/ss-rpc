package com.ssrpc.core.spi.impl;

import com.ssrpc.core.spi.TestInterface;

/**
 * 替代测试实现
 *
 * @author chenzhang
 * @since 1.0.0
 */
public class AlternativeTestImpl implements TestInterface {
    
    @Override
    public String test() {
        return "Alternative implementation test result";
    }
    
    @Override
    public String getName() {
        return "AlternativeTestImpl";
    }
} 