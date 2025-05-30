package com.ssrpc.core.annotation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RpcService注解测试
 * 
 * @author SS-RPC Team
 * @since 1.0.0
 */
class RpcServiceTest {

    @RpcService
    static class TestService {
        public String hello() {
            return "Hello SS-RPC";
        }
    }

    @RpcService(value = "customService", version = "2.0.0", weight = 200)
    static class CustomService {
        public String test() {
            return "Custom Service";
        }
    }

    @Test
    void testDefaultAnnotationValues() {
        RpcService annotation = TestService.class.getAnnotation(RpcService.class);
        
        assertNotNull(annotation);
        assertEquals("", annotation.value());
        assertEquals("1.0.0", annotation.version());
        assertEquals(100, annotation.weight());
        assertFalse(annotation.hotReload());
    }

    @Test
    void testCustomAnnotationValues() {
        RpcService annotation = CustomService.class.getAnnotation(RpcService.class);
        
        assertNotNull(annotation);
        assertEquals("customService", annotation.value());
        assertEquals("2.0.0", annotation.version());
        assertEquals(200, annotation.weight());
        assertFalse(annotation.hotReload());
    }
} 