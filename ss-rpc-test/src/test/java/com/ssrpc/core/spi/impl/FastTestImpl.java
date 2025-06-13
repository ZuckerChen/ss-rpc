package com.ssrpc.core.spi.impl;

import com.ssrpc.core.spi.TestInterface;

/**
 * 快速测试实现
 *
 * @author chenzhang
 * @since 1.0.0
 */
public class FastTestImpl implements TestInterface {
    
    @Override
    public String test() {
        return "Fast implementation test result";
    }
    
    @Override
    public String getName() {
        return "FastTestImpl";
    }
} 