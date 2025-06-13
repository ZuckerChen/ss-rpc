package com.ssrpc.core.spi;

import com.ssrpc.core.factory.ExtensionFactory;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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
            Class<TestInterface> testInterfaceClass = TestInterface.class;
            
            // 获取支持的扩展
            Set<String> extensions = ExtensionFactory.getSupportedExtensions(testInterfaceClass);
            log.info("Available test implementations: {}", extensions);
            assertFalse(extensions.isEmpty(), "Should have at least one test implementation");
            
            // 获取默认扩展
            TestInterface defaultImpl = ExtensionFactory.getDefaultExtension(testInterfaceClass);
            log.info("Default test implementation: {}", defaultImpl.getClass().getSimpleName());
            log.info("Default test result: {}", defaultImpl.test());
            assertNotNull(defaultImpl, "Default implementation should not be null");
            assertEquals("DefaultTestImpl", defaultImpl.getName());
            
            // 获取特定扩展
            for (String extName : extensions) {
                TestInterface impl = ExtensionFactory.getExtension(testInterfaceClass, extName);
                log.info("Test implementation [{}]: {}", extName, impl.getClass().getSimpleName());
                log.info("Test result [{}]: {}", extName, impl.test());
                assertNotNull(impl, "Implementation should not be null for extension: " + extName);
            }
            
            // 测试特定扩展
            if (extensions.contains("alternative")) {
                TestInterface altImpl = ExtensionFactory.getExtension(testInterfaceClass, "alternative");
                assertEquals("AlternativeTestImpl", altImpl.getName());
                assertTrue(altImpl.test().contains("Alternative"));
            }
            
            if (extensions.contains("fast")) {
                TestInterface fastImpl = ExtensionFactory.getExtension(testInterfaceClass, "fast");
                assertEquals("FastTestImpl", fastImpl.getName());
                assertTrue(fastImpl.test().contains("Fast"));
            }
            
        } catch (Exception e) {
            log.error("Test interface SPI demo failed", e);
            fail("Test interface SPI demo should not fail: " + e.getMessage());
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
        Class<TestInterface> testInterfaceClass = TestInterface.class;
        Set<String> extensions = ExtensionFactory.getSupportedExtensions(testInterfaceClass);
        assertFalse(extensions.isEmpty(), "Should have test extensions");
        
        int afterLoadCacheSize = ExtensionFactory.getCacheSize();
        log.info("After load cache size: {}", afterLoadCacheSize);
        assertTrue(afterLoadCacheSize >= initialCacheSize, "Cache size should increase after loading");
        
        // 清理缓存
        ExtensionFactory.clearCache();
        
        int afterClearCacheSize = ExtensionFactory.getCacheSize();
        log.info("After clear cache size: {}", afterClearCacheSize);
        assertEquals(0, afterClearCacheSize, "Cache should be empty after clear");
        
        // 打印扩展信息
        ExtensionFactory.printExtensionInfo();
    }
    
    @Test
    void testExtensionLoaderDirectly() {
        log.info("--- Testing ExtensionLoader Directly ---");
        
        // 获取ExtensionLoader
        ExtensionLoader<TestInterface> loader = ExtensionLoader.getExtensionLoader(TestInterface.class);
        assertNotNull(loader, "ExtensionLoader should not be null");
        
        // 测试支持的扩展
        Set<String> extensions = loader.getSupportedExtensions();
        log.info("Supported extensions: {}", extensions);
        assertFalse(extensions.isEmpty(), "Should have extensions");
        assertTrue(extensions.contains("default"), "Should contain default extension");
        
        // 测试默认扩展
        TestInterface defaultExt = loader.getDefaultExtension();
        assertNotNull(defaultExt, "Default extension should not be null");
        log.info("Default extension: {}", defaultExt.getName());
        
        // 测试获取特定扩展
        for (String extName : extensions) {
            TestInterface ext = loader.getExtension(extName);
            assertNotNull(ext, "Extension should not be null: " + extName);
            log.info("Extension [{}]: {}", extName, ext.getName());
            
            // 测试缓存 - 再次获取应该是同一个实例
            TestInterface ext2 = loader.getExtension(extName);
            assertSame(ext, ext2, "Should return cached instance");
        }
        
        // 测试hasExtension
        assertTrue(loader.hasExtension("default"), "Should have default extension");
        assertFalse(loader.hasExtension("nonexistent"), "Should not have nonexistent extension");
    }
    
    @Test
    void testInvalidExtensions() {
        log.info("--- Testing Invalid Extensions ---");
        
        ExtensionLoader<TestInterface> loader = ExtensionLoader.getExtensionLoader(TestInterface.class);
        
        // 测试获取不存在的扩展
        assertThrows(IllegalStateException.class, () -> {
            loader.getExtension("nonexistent");
        }, "Should throw exception for nonexistent extension");
        
        // 测试null或空扩展名
        assertThrows(IllegalArgumentException.class, () -> {
            loader.getExtension(null);
        }, "Should throw exception for null extension name");
        
        assertThrows(IllegalArgumentException.class, () -> {
            loader.getExtension("");
        }, "Should throw exception for empty extension name");
    }
} 