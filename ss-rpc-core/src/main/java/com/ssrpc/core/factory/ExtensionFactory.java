package com.ssrpc.core.factory;

import com.ssrpc.core.spi.ExtensionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SPI扩展工厂.
 * 
 * 提供更便捷的SPI扩展访问方式，统一管理各种SPI扩展
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class ExtensionFactory {
    
    private static final Logger log = LoggerFactory.getLogger(ExtensionFactory.class);
    
    /**
     * ExtensionLoader缓存
     */
    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> LOADER_CACHE = 
            new ConcurrentHashMap<>();
    
    /**
     * 私有构造方法
     */
    private ExtensionFactory() {}
    
    /**
     * 获取扩展实例
     * 
     * @param type 扩展接口类型
     * @param name 扩展名称
     * @param <T> 扩展类型
     * @return 扩展实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T getExtension(Class<T> type, String name) {
        ExtensionLoader<T> loader = (ExtensionLoader<T>) getExtensionLoader(type);
        return loader.getExtension(name);
    }
    
    /**
     * 获取默认扩展实例
     * 
     * @param type 扩展接口类型
     * @param <T> 扩展类型
     * @return 默认扩展实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T getDefaultExtension(Class<T> type) {
        ExtensionLoader<T> loader = (ExtensionLoader<T>) getExtensionLoader(type);
        return loader.getDefaultExtension();
    }
    
    /**
     * 检查是否存在指定扩展
     * 
     * @param type 扩展接口类型
     * @param name 扩展名称
     * @return true表示存在，false表示不存在
     */
    public static boolean hasExtension(Class<?> type, String name) {
        ExtensionLoader<?> loader = getExtensionLoader(type);
        return loader.hasExtension(name);
    }
    
    /**
     * 获取所有支持的扩展名称
     * 
     * @param type 扩展接口类型
     * @return 扩展名称集合
     */
    public static Set<String> getSupportedExtensions(Class<?> type) {
        ExtensionLoader<?> loader = getExtensionLoader(type);
        return loader.getSupportedExtensions();
    }
    
    /**
     * 获取ExtensionLoader
     * 
     * @param type 扩展接口类型
     * @return ExtensionLoader实例
     */
    private static ExtensionLoader<?> getExtensionLoader(Class<?> type) {
        return LOADER_CACHE.computeIfAbsent(type, key -> {
            try {
                ExtensionLoader<?> loader = ExtensionLoader.getExtensionLoader(type);
                log.debug("Created ExtensionLoader for type: {}", type.getName());
                return loader;
            } catch (Exception e) {
                log.error("Failed to create ExtensionLoader for type: {}", type.getName(), e);
                throw new RuntimeException("Failed to create ExtensionLoader for type: " + type.getName(), e);
            }
        });
    }
    
    /**
     * 清空缓存
     */
    public static void clearCache() {
        int size = LOADER_CACHE.size();
        LOADER_CACHE.clear();
        log.info("Cleared {} cached ExtensionLoaders", size);
    }
    
    /**
     * 获取缓存大小
     * 
     * @return 缓存的ExtensionLoader数量
     */
    public static int getCacheSize() {
        return LOADER_CACHE.size();
    }
    
    /**
     * 打印所有已加载的扩展信息
     */
    public static void printExtensionInfo() {
        log.info("=== Extension Information ===");
        LOADER_CACHE.forEach((type, loader) -> {
            log.info("Type: {}", type.getName());
            Set<String> extensions = loader.getSupportedExtensions();
            log.info("  Supported extensions: {}", extensions);
            log.info("  Default extension: {}", getDefaultExtensionName(loader));
        });
        log.info("=== End of Extension Information ===");
    }
    
    /**
     * 获取默认扩展名称
     */
    private static String getDefaultExtensionName(ExtensionLoader<?> loader) {
        try {
            Object defaultExt = loader.getDefaultExtension();
            return defaultExt != null ? defaultExt.getClass().getSimpleName() : "none";
        } catch (Exception e) {
            return "none";
        }
    }
} 