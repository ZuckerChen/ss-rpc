package com.ssrpc.loadbalance;

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
    default LoadBalanceStatistics getStatistics() {
        return new LoadBalanceStatistics();
    }
    
    /**
     * 负载均衡统计信息
     */
    class LoadBalanceStatistics {
        /** 总选择次数 */
        private long totalSelections = 0;
        
        /** 实例选择次数统计 */
        private java.util.Map<String, Long> instanceSelections = new java.util.concurrent.ConcurrentHashMap<>();
        
        /** 最后选择时间 */
        private long lastSelectTime = 0;
        
        public long getTotalSelections() {
            return totalSelections;
        }
        
        public void setTotalSelections(long totalSelections) {
            this.totalSelections = totalSelections;
        }
        
        public java.util.Map<String, Long> getInstanceSelections() {
            return instanceSelections;
        }
        
        public void setInstanceSelections(java.util.Map<String, Long> instanceSelections) {
            this.instanceSelections = instanceSelections;
        }
        
        public long getLastSelectTime() {
            return lastSelectTime;
        }
        
        public void setLastSelectTime(long lastSelectTime) {
            this.lastSelectTime = lastSelectTime;
        }
        
        /**
         * 记录一次选择
         */
        public void recordSelection(String instanceId) {
            totalSelections++;
            instanceSelections.merge(instanceId, 1L, Long::sum);
            lastSelectTime = System.currentTimeMillis();
        }
        
        /**
         * 获取实例选择次数
         */
        public long getInstanceSelectionCount(String instanceId) {
            return instanceSelections.getOrDefault(instanceId, 0L);
        }
        
        /**
         * 重置统计信息
         */
        public void reset() {
            totalSelections = 0;
            instanceSelections.clear();
            lastSelectTime = 0;
        }
        
        @Override
        public String toString() {
            return "LoadBalanceStatistics{" +
                    "totalSelections=" + totalSelections +
                    ", instanceCount=" + instanceSelections.size() +
                    ", lastSelectTime=" + lastSelectTime +
                    '}';
        }
    }
} 