package com.ssrpc.core.spi;

import com.ssrpc.core.factory.ExtensionFactory;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * SPI机制演示测试.
 * 
 * 展示如何在ss-rpc项目中使用SPI机制
 * 
 * @author chenzhang
 * @since 1.0.0
 */
class SpiDemoTest {
    
    private static final Logger log = LoggerFactory.getLogger(SpiDemoTest.class);
    
    @Test
    void demonstrateSpiMechanism() {
        log.info("=== SS-RPC SPI Mechanism Demo ===");
        
        // 演示ExtensionFactory的使用
        demonstrateExtensionFactory();
        
        log.info("=== SPI Demo completed ===");
    }
    
    /**
     * 演示ExtensionFactory的使用
     */
    private void demonstrateExtensionFactory() {
        log.info("--- ExtensionFactory Usage Demo ---");
        
        // 测试测试接口的SPI
        demonstrateTestInterface();
        
        // 尝试加载其他模块的SPI（如果可用）
        tryLoadOtherModulesSpi();
    }
    
    /**
     * 演示测试接口的SPI使用
     */
    private void demonstrateTestInterface() {
        try {
            Class<?> testInterfaceClass = ExtensionLoaderTest.TestInterface.class;
            
            // 获取支持的扩展
            Set<String> extensions = ExtensionFactory.getSupportedExtensions(testInterfaceClass);
            log.info("Available test implementations: {}", extensions);
            
            // 获取默认扩展
            Object defaultImpl = ExtensionFactory.getDefaultExtension(testInterfaceClass);
            log.info("Default test implementation: {}", defaultImpl.getClass().getSimpleName());
            
            // 获取特定扩展
            for (String extName : extensions) {
                Object impl = ExtensionFactory.getExtension(testInterfaceClass, extName);
                log.info("Test implementation [{}]: {}", extName, impl.getClass().getSimpleName());
            }
            
        } catch (Exception e) {
            log.warn("Test interface SPI demo failed: {}", e.getMessage());
        }
    }
    
    /**
     * 尝试加载其他模块的SPI
     */
    private void tryLoadOtherModulesSpi() {
        // 尝试加载序列化器SPI
        tryLoadSpi("com.ssrpc.serialization.Serializer", "Serializer");
        
        // 尝试加载负载均衡器SPI
        tryLoadSpi("com.ssrpc.loadbalance.LoadBalancer", "LoadBalancer");
        
        // 尝试加载服务注册SPI
        tryLoadSpi("com.ssrpc.registry.ServiceRegistry", "ServiceRegistry");
    }
    
    /**
     * 尝试加载指定的SPI
     */
    private void tryLoadSpi(String className, String displayName) {
        try {
            Class<?> spiClass = Class.forName(className);
            
            // 检查支持的扩展
            Set<String> extensions = ExtensionFactory.getSupportedExtensions(spiClass);
            log.info("Available {} implementations: {}", displayName, extensions);
            
            // 获取默认扩展
            Object defaultImpl = ExtensionFactory.getDefaultExtension(spiClass);
            log.info("Default {} implementation: {}", displayName, defaultImpl.getClass().getSimpleName());
            
        } catch (ClassNotFoundException e) {
            log.info("{} SPI not available in current classpath", displayName);
        } catch (Exception e) {
            log.warn("{} SPI demo failed: {}", displayName, e.getMessage());
        }
    }
    
    @Test
    void testExtensionFactoryBasics() {
        log.info("--- Testing ExtensionFactory Basic Operations ---");
        
        // 测试缓存清理
        int initialCacheSize = ExtensionFactory.getCacheSize();
        log.info("Initial cache size: {}", initialCacheSize);
        
        // 加载一个扩展
        Class<?> testInterfaceClass = ExtensionLoaderTest.TestInterface.class;
        ExtensionFactory.getSupportedExtensions(testInterfaceClass);
        
        int afterLoadCacheSize = ExtensionFactory.getCacheSize();
        log.info("After load cache size: {}", afterLoadCacheSize);
        
        // 清理缓存
        ExtensionFactory.clearCache();
        
        int afterClearCacheSize = ExtensionFactory.getCacheSize();
        log.info("After clear cache size: {}", afterClearCacheSize);
        
        // 打印扩展信息
        ExtensionFactory.printExtensionInfo();
    }
} 