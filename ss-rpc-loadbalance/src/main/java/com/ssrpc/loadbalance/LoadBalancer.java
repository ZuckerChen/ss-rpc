package com.ssrpc.loadbalance;

import com.ssrpc.core.spi.SPI;
import com.ssrpc.protocol.ServiceInstance;

import java.util.List;

/**
 * 负载均衡器接口.
 * 
 * 根据负载均衡策略从多个服务实例中选择一个最优实例
 * 支持多种负载均衡算法：随机、轮询、加权轮询、一致性哈希等
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@SPI("round_robin")
public interface LoadBalancer {
    
    /**
     * 从服务实例列表中选择一个实例
     * 
     * @param instances 可用的服务实例列表
     * @return 选中的服务实例，如果没有可用实例则返回null
     * @throws LoadBalanceException 负载均衡异常
     */
    ServiceInstance select(List<ServiceInstance> instances) throws LoadBalanceException;
    
    /**
     * 获取负载均衡策略类型
     * 
     * @return 负载均衡类型
     */
    LoadBalanceType getType();
    
    /**
     * 获取负载均衡器名称
     * 
     * @return 负载均衡器名称
     */
    default String getName() {
        return getType().getCode();
    }
    
    /**
     * 重置负载均衡状态（例如轮询计数器）
     */
    default void reset() {
        // 默认实现为空，由具体实现决定是否需要重置
    }
    
    /**
     * 更新服务实例权重（仅支持权重相关的负载均衡策略）
     * 
     * @param instanceId 实例ID
     * @param weight 新权重
     */
    default void updateWeight(String instanceId, int weight) {
        // 默认实现为空，由具体实现决定是否支持
    }
    
    /**
     * 获取当前实例的权重（仅支持权重相关的负载均衡策略）
     * 
     * @param instanceId 实例ID
     * @return 实例权重，如果不支持权重则返回-1
     */
    default int getWeight(String instanceId) {
        return -1;
    }
    
    /**
     * 获取统计信息
     * 
     * @return 负载均衡统计信息
     */
    default LoadBalanceStats getStats() {
        return new LoadBalanceStats();
    }
    
    /**
     * 检查是否支持指定的服务实例
     * 
     * @param instance 服务实例
     * @return true表示支持，false表示不支持
     */
    default boolean supports(ServiceInstance instance) {
        return instance != null && instance.isHealthy();
    }
} 