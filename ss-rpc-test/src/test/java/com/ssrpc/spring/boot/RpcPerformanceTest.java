package com.ssrpc.spring.boot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RPC性能测试类
 * 
 * @author shuang.kou
 * @since 1.0.0
 */
@DisplayName("RPC性能测试")
class RpcPerformanceTest {
    
    private TestUserServiceImpl service;
    
    @BeforeEach
    void setUp() {
        service = new TestUserServiceImpl();
    }
    
    @Test
    @DisplayName("测试基本性能指标")
    void testBasicPerformance() {
        int iterations = 1000;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            service.ping();
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double throughput = (double) iterations / duration * 1000;
        
        System.out.println("基本性能测试结果:");
        System.out.println("总请求数: " + iterations);
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("吞吐量: " + String.format("%.2f", throughput) + " req/s");
        
        assertTrue(duration < 5000, "1000次调用应在5秒内完成");
        assertTrue(throughput > 100, "吞吐量应大于100 req/s");
    }
} 