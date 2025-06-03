package com.ssrpc.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NetworkConfig配置测试
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class NetworkConfigTest {
    
    @Test
    @DisplayName("测试默认配置验证通过")
    void testDefaultConfigValidation() {
        NetworkConfig config = NetworkConfig.defaultConfig();
        
        // 默认配置应该能通过验证
        assertDoesNotThrow(config::validate, "默认配置应该能通过验证");
        
        // 验证默认心跳配置
        assertEquals(30, config.getHeartbeatInterval(), "默认心跳间隔应该是30秒");
        assertEquals(15, config.getHeartbeatTimeout(), "默认心跳超时应该是15秒");
        assertTrue(config.getHeartbeatTimeout() < config.getHeartbeatInterval(), 
                  "心跳超时应该小于心跳间隔");
    }
    
    @Test
    @DisplayName("测试高性能配置验证通过")
    void testHighPerformanceConfigValidation() {
        NetworkConfig config = NetworkConfig.highPerformanceConfig();
        
        assertDoesNotThrow(config::validate, "高性能配置应该能通过验证");
        assertEquals(20, config.getHeartbeatInterval(), "高性能配置心跳间隔应该是20秒");
        assertEquals(10, config.getHeartbeatTimeout(), "高性能配置心跳超时应该是10秒");
        assertTrue(config.getHeartbeatTimeout() < config.getHeartbeatInterval());
    }
    
    @Test
    @DisplayName("测试低延迟配置验证通过")
    void testLowLatencyConfigValidation() {
        NetworkConfig config = NetworkConfig.lowLatencyConfig();
        
        assertDoesNotThrow(config::validate, "低延迟配置应该能通过验证");
        assertEquals(10, config.getHeartbeatInterval(), "低延迟配置心跳间隔应该是10秒");
        assertEquals(5, config.getHeartbeatTimeout(), "低延迟配置心跳超时应该是5秒");
        assertTrue(config.getHeartbeatTimeout() < config.getHeartbeatInterval());
    }
    
    @Test
    @DisplayName("测试心跳超时大于间隔时验证失败")
    void testHeartbeatTimeoutGreaterThanInterval() {
        NetworkConfig config = NetworkConfig.defaultConfig();
        config.setHeartbeatInterval(5);
        config.setHeartbeatTimeout(10);  // 超时 > 间隔，应该验证失败
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            config::validate,
            "心跳超时大于间隔时应该验证失败"
        );
        
        assertTrue(exception.getMessage().contains("Heartbeat timeout should be less than heartbeat interval"),
                  "错误消息应该包含心跳超时配置错误的说明");
    }
    
    @Test
    @DisplayName("测试心跳超时等于间隔时验证失败")
    void testHeartbeatTimeoutEqualsInterval() {
        NetworkConfig config = NetworkConfig.defaultConfig();
        config.setHeartbeatInterval(10);
        config.setHeartbeatTimeout(10);  // 超时 = 间隔，应该验证失败
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            config::validate,
            "心跳超时等于间隔时应该验证失败"
        );
        
        assertTrue(exception.getMessage().contains("Heartbeat timeout should be less than heartbeat interval"));
    }
    
    @Test
    @DisplayName("测试禁用心跳时不验证心跳配置")
    void testDisabledHeartbeatSkipsValidation() {
        NetworkConfig config = NetworkConfig.defaultConfig();
        config.setHeartbeatEnabled(false);
        config.setHeartbeatInterval(5);
        config.setHeartbeatTimeout(10);  // 即使配置错误，禁用心跳时也不验证
        
        assertDoesNotThrow(config::validate, "禁用心跳时不应该验证心跳配置");
    }
    
    @Test
    @DisplayName("测试心跳超时为0时验证失败")
    void testZeroHeartbeatTimeout() {
        NetworkConfig config = NetworkConfig.defaultConfig();
        config.setHeartbeatTimeout(0);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            config::validate,
            "心跳超时为0时应该验证失败"
        );
        
        assertTrue(exception.getMessage().contains("Heartbeat timeout must be positive when heartbeat is enabled"));
    }
    
    @Test
    @DisplayName("测试便利方法返回正确的毫秒值")
    void testConvenienceMethods() {
        NetworkConfig config = NetworkConfig.defaultConfig();
        config.setHeartbeatInterval(10);
        config.setHeartbeatTimeout(5);
        
        assertEquals(10000L, config.getHeartbeatIntervalMillis(), "心跳间隔毫秒值应该正确");
        assertEquals(5000L, config.getHeartbeatTimeoutMillis(), "心跳超时毫秒值应该正确");
    }
    
    @Test
    @DisplayName("测试toString包含心跳超时信息")
    void testToStringContainsHeartbeatTimeout() {
        NetworkConfig config = NetworkConfig.defaultConfig();
        config.setHeartbeatTimeout(8);
        
        String toString = config.toString();
        assertTrue(toString.contains("heartbeatTimeout=8"), 
                  "toString应该包含心跳超时信息");
    }
} 