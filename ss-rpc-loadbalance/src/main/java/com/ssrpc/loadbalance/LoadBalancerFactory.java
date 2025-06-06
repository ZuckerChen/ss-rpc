package com.ssrpc.loadbalance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 负载均衡器工厂.
 * 
 * 负责创建和管理不同类型的负载均衡器实例
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
    private static final ConcurrentHashMap<LoadBalanceType, LoadBalancer> LOAD_BALANCER_CACHE = 
            new ConcurrentHashMap<>();
    
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
        
        return LOAD_BALANCER_CACHE.computeIfAbsent(type, LoadBalancerFactory::createLoadBalancer);
    }
    
    /**
     * 创建新的负载均衡器实例
     * 
     * @param type 负载均衡类型
     * @return 负载均衡器实例
     * @throws LoadBalanceException 如果不支持该类型的负载均衡器
     */
    public static LoadBalancer createNewLoadBalancer(LoadBalanceType type) throws LoadBalanceException {
        if (type == null) {
            throw new LoadBalanceException(
                LoadBalanceException.ErrorCodes.INVALID_STRATEGY,
                "Load balance type cannot be null"
            );
        }
        
        return createLoadBalancer(type);
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
        
        try {
            LoadBalanceType type = LoadBalanceType.fromCode(typeCode.trim());
            return getLoadBalancer(type);
        } catch (IllegalArgumentException e) {
            throw new LoadBalanceException(
                LoadBalanceException.ErrorCodes.INVALID_STRATEGY,
                "Unsupported load balance type: " + typeCode,
                e
            );
        }
    }
    
    /**
     * 实际创建负载均衡器的方法
     */
    private static LoadBalancer createLoadBalancer(LoadBalanceType type) {
        switch (type) {
            case RANDOM:
                return new RandomLoadBalancer();
                
            case ROUND_ROBIN:
                return new RoundRobinLoadBalancer();
                
            case WEIGHTED_ROUND_ROBIN:
                return new WeightedRoundRobinLoadBalancer();
                
            case CONSISTENT_HASH:
                // TODO: 实现一致性哈希负载均衡器
                throw new LoadBalanceException(
                    LoadBalanceException.ErrorCodes.INVALID_STRATEGY,
                    "Consistent hash load balancer not implemented yet"
                );
                
            case LEAST_ACTIVE:
                // TODO: 实现最少活跃数负载均衡器
                throw new LoadBalanceException(
                    LoadBalanceException.ErrorCodes.INVALID_STRATEGY,
                    "Least active load balancer not implemented yet"
                );
                
            case ADAPTIVE:
                // TODO: 实现自适应负载均衡器
                throw new LoadBalanceException(
                    LoadBalanceException.ErrorCodes.INVALID_STRATEGY,
                    "Adaptive load balancer not implemented yet"
                );
                
            default:
                throw new LoadBalanceException(
                    LoadBalanceException.ErrorCodes.INVALID_STRATEGY,
                    "Unsupported load balance type: " + type
                );
        }
    }
    
    /**
     * 检查是否支持指定的负载均衡类型
     * 
     * @param type 负载均衡类型
     * @return true表示支持，false表示不支持
     */
    public static boolean isSupported(LoadBalanceType type) {
        if (type == null) {
            return false;
        }
        
        switch (type) {
            case RANDOM:
            case ROUND_ROBIN:
            case WEIGHTED_ROUND_ROBIN:
                return true;
                
            case CONSISTENT_HASH:
            case LEAST_ACTIVE:
            case ADAPTIVE:
                // TODO: 这些类型暂未实现
                return false;
                
            default:
                return false;
        }
    }
    
    /**
     * 获取所有支持的负载均衡类型
     * 
     * @return 支持的负载均衡类型数组
     */
    public static LoadBalanceType[] getSupportedTypes() {
        return new LoadBalanceType[] {
            LoadBalanceType.RANDOM,
            LoadBalanceType.ROUND_ROBIN,
            LoadBalanceType.WEIGHTED_ROUND_ROBIN
        };
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
     * @param type 负载均衡类型
     */
    public static void resetLoadBalancer(LoadBalanceType type) {
        LoadBalancer loadBalancer = LOAD_BALANCER_CACHE.get(type);
        if (loadBalancer != null) {
            loadBalancer.reset();
            log.debug("Reset load balancer for type: {}", type);
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