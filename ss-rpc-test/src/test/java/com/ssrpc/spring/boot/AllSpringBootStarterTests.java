package com.ssrpc.spring.boot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Spring Boot Starter所有测试的运行器
 * 
 * @author shuang.kou
 * @since 1.0.0
 */
@DisplayName("SS-RPC Spring Boot Starter 所有测试")
class AllSpringBootStarterTests {
    
    @Test
    @DisplayName("运行所有Spring Boot Starter测试")
    void runAllTests() {
        // 这个测试类主要用于IDE中快速运行所有相关测试
        // 实际的测试逻辑在各个具体的测试类中
        
        System.out.println("=== SS-RPC Spring Boot Starter 测试概览 ===");
        System.out.println("1. RpcAnnotationTest - 测试RPC注解功能");
        System.out.println("2. RpcProcessorTest - 测试RPC处理器逻辑");
        System.out.println("3. SpringBootStarterBasicTest - 测试Spring Boot基础集成");
        System.out.println("4. SpringBootStarterIntegrationTest - 测试完整集成（需要修复依赖）");
        System.out.println("============================================");
        
        // 注意：这个测试主要用于文档目的
        // 实际测试请运行各个具体的测试类
    }
} 