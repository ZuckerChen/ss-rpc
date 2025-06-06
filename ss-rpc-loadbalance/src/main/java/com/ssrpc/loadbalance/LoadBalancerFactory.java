package com.ssrpc.loadbalance;

import com.ssrpc.core.spi.ExtensionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负载均衡器工厂.
 * 
 * 基于SPI机制创建和管理不同类型的负载均衡器实例
 * 支持负载均衡器复用和单例模式
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class LoadBalancerFactory {
    
    private static final Logger log = LoggerFactory.getLogger(LoadBalancerFactory.class);
    
    /**
     * 负载均衡器实例缓存
     * key: LoadBalanceType, value: LoadBalancer实例
     */
    private static final ConcurrentHashMap<String, LoadBalancer> LOAD_BALANCER_CACHE = 
            new ConcurrentHashMap<>();
    
    /**
     * SPI扩展加载器
     */
    private static final ExtensionLoader<LoadBalancer> EXTENSION_LOADER = 
            ExtensionLoader.getExtensionLoader(LoadBalancer.class);
    
    /**
     * 私有构造方法，防止实例化
     */
    private LoadBalancerFactory() {}
    
    /**
     * 获取负载均衡器实例（单例模式）
     * 
     * @param type 负载均衡类型
     * @return 负载均衡器实例
     * @throws LoadBalanceException 如果不支持该类型的负载均衡器
     */
    public static LoadBalancer getLoadBalancer(LoadBalanceType type) throws LoadBalanceException {
        if (type == null) {
            throw new LoadBalanceException(
                LoadBalanceException.ErrorCodes.INVALID_STRATEGY,
                "Load balance type cannot be null"
            );
        }
        
        return getLoadBalancer(type.getCode());
    }
    
    /**
     * 根据字符串类型获取负载均衡器
     * 
     * @param typeCode 负载均衡类型代码
     * @return 负载均衡器实例
     * @throws LoadBalanceException 如果不支持该类型的负载均衡器
     */
    public static LoadBalancer getLoadBalancer(String typeCode) throws LoadBalanceException {
        if (typeCode == null || typeCode.trim().isEmpty()) {
            throw new LoadBalanceException(
                LoadBalanceException.ErrorCodes.INVALID_STRATEGY,
                "Load balance type code cannot be null or empty"
            );
        }
        
        return LOAD_BALANCER_CACHE.computeIfAbsent(typeCode, LoadBalancerFactory::createLoadBalancer);
    }
    
    /**
     * 创建新的负载均衡器实例
     * 
     * @param typeCode 负载均衡类型代码
     * @return 负载均衡器实例
     * @throws LoadBalanceException 如果不支持该类型的负载均衡器
     */
    public static LoadBalancer createNewLoadBalancer(String typeCode) throws LoadBalanceException {
        if (typeCode == null || typeCode.trim().isEmpty()) {
            throw new LoadBalanceException(
                LoadBalanceException.ErrorCodes.INVALID_STRATEGY,
                "Load balance type code cannot be null or empty"
            );
        }
        
        return createLoadBalancer(typeCode);
    }
    
    /**
     * 实际创建负载均衡器的方法
     */
    private static LoadBalancer createLoadBalancer(String typeCode) {
        try {
            if (!EXTENSION_LOADER.hasExtension(typeCode)) {
                throw new LoadBalanceException(
                    LoadBalanceException.ErrorCodes.INVALID_STRATEGY,
                    "Unsupported load balance type: " + typeCode
                );
            }
            
            LoadBalancer loadBalancer = EXTENSION_LOADER.getExtension(typeCode);
            log.info("Created load balancer: {} ({})", typeCode, loadBalancer.getClass().getSimpleName());
            return loadBalancer;
            
        } catch (Exception e) {
            throw new LoadBalanceException(
                LoadBalanceException.ErrorCodes.INVALID_STRATEGY,
                "Failed to create load balancer for type: " + typeCode,
                e
            );
        }
    }
    
    /**
     * 获取默认负载均衡器
     * 
     * @return 默认负载均衡器实例
     */
    public static LoadBalancer getDefaultLoadBalancer() {
        try {
            return EXTENSION_LOADER.getDefaultExtension();
        } catch (Exception e) {
            log.warn("Failed to get default load balancer, fallback to round_robin", e);
            return getLoadBalancer("round_robin");
        }
    }
    
    /**
     * 检查是否支持指定的负载均衡类型
     * 
     * @param typeCode 负载均衡类型代码
     * @return true表示支持，false表示不支持
     */
    public static boolean isSupported(String typeCode) {
        if (typeCode == null || typeCode.trim().isEmpty()) {
            return false;
        }
        
        return EXTENSION_LOADER.hasExtension(typeCode);
    }
    
    /**
     * 获取所有支持的负载均衡类型
     * 
     * @return 支持的负载均衡类型集合
     */
    public static Set<String> getSupportedTypes() {
        return EXTENSION_LOADER.getSupportedExtensions();
    }
    
    /**
     * 清空缓存的负载均衡器实例
     */
    public static void clearCache() {
        int size = LOAD_BALANCER_CACHE.size();
        LOAD_BALANCER_CACHE.clear();
        log.info("Cleared {} cached load balancer instances", size);
    }
    
    /**
     * 获取缓存中的负载均衡器数量
     * 
     * @return 缓存的负载均衡器数量
     */
    public static int getCacheSize() {
        return LOAD_BALANCER_CACHE.size();
    }
    
    /**
     * 重置指定类型的负载均衡器
     * 
     * @param typeCode 负载均衡类型代码
     */
    public static void resetLoadBalancer(String typeCode) {
        LoadBalancer loadBalancer = LOAD_BALANCER_CACHE.get(typeCode);
        if (loadBalancer != null) {
            loadBalancer.reset();
            log.debug("Reset load balancer for type: {}", typeCode);
        }
    }
    
    /**
     * 重置所有缓存的负载均衡器
     */
    public static void resetAllLoadBalancers() {
        LOAD_BALANCER_CACHE.values().forEach(LoadBalancer::reset);
        log.debug("Reset all cached load balancers");
    }
} 