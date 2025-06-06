package com.ssrpc.loadbalance;

import com.ssrpc.protocol.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 加权轮询负载均衡器实现.
 * 
 * 根据服务实例的权重进行轮询选择，权重越高被选中的概率越大
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class WeightedRoundRobinLoadBalancer implements LoadBalancer {
    
    private static final Logger log = LoggerFactory.getLogger(WeightedRoundRobinLoadBalancer.class);
    
    /**
     * 实例当前权重缓存
     * key: instanceId, value: current weight
     */
    private final ConcurrentMap<String, Integer> currentWeights = new ConcurrentHashMap<>();
    
    private final LoadBalanceStats stats = new LoadBalanceStats();
    
    @Override
    public ServiceInstance select(List<ServiceInstance> instances) throws LoadBalanceException {
        if (instances == null || instances.isEmpty()) {
            throw new LoadBalanceException(
                LoadBalanceException.ErrorCodes.NO_AVAILABLE_INSTANCE,
                "No available service instances"
            );
        }
        
        // 过滤健康的实例
        List<ServiceInstance> healthyInstances = instances.stream()
                .filter(instance -> instance != null && instance.isHealthy())
                .collect(java.util.stream.Collectors.toList());
        
        if (healthyInstances.isEmpty()) {
            throw new LoadBalanceException(
                LoadBalanceException.ErrorCodes.NO_AVAILABLE_INSTANCE,
                "No healthy service instances available"
            );
        }
        
        ServiceInstance selected = selectByWeight(healthyInstances);
        
        // 记录统计信息
        stats.recordSelection(selected.getInstanceId());
        
        log.debug("Weighted round-robin load balancer selected instance: {} (weight: {})", 
                selected.getInstanceId(), selected.getWeight());
        return selected;
    }
    
    /**
     * 基于权重选择实例
     */
    private ServiceInstance selectByWeight(List<ServiceInstance> instances) {
        int totalWeight = 0;
        ServiceInstance maxCurrentWeightInstance = null;
        int maxCurrentWeight = Integer.MIN_VALUE;
        
        // 计算总权重并找到当前权重最大的实例
        for (ServiceInstance instance : instances) {
            String instanceId = instance.getInstanceId();
            int weight = Math.max(instance.getWeight(), 1); // 确保权重至少为1
            
            // 获取当前权重
            Integer currentWeight = currentWeights.get(instanceId);
            if (currentWeight == null) {
                currentWeight = 0;
            }
            
            // 增加当前权重
            currentWeight += weight;
            currentWeights.put(instanceId, currentWeight);
            
            totalWeight += weight;
            
            // 找到当前权重最大的实例
            if (currentWeight > maxCurrentWeight) {
                maxCurrentWeight = currentWeight;
                maxCurrentWeightInstance = instance;
            }
        }
        
        if (maxCurrentWeightInstance == null) {
            // 这种情况不应该发生，但为了安全起见
            return instances.get(0);
        }
        
        // 减少选中实例的当前权重
        String selectedInstanceId = maxCurrentWeightInstance.getInstanceId();
        int newCurrentWeight = maxCurrentWeight - totalWeight;
        currentWeights.put(selectedInstanceId, newCurrentWeight);
        
        return maxCurrentWeightInstance;
    }
    
    @Override
    public LoadBalanceType getType() {
        return LoadBalanceType.WEIGHTED_ROUND_ROBIN;
    }
    
    @Override
    public LoadBalanceStats getStats() {
        return stats;
    }
    
    @Override
    public void reset() {
        currentWeights.clear();
        stats.reset();
        log.debug("Weighted round-robin load balancer stats reset");
    }
    
    @Override
    public void updateWeight(String instanceId, int weight) {
        if (instanceId != null && weight > 0) {
            // 重置当前权重，让新权重生效
            currentWeights.remove(instanceId);
            log.debug("Updated weight for instance {}: {}", instanceId, weight);
        }
    }
    
    @Override
    public int getWeight(String instanceId) {
        // 从实例本身获取权重，这里返回当前权重
        return currentWeights.getOrDefault(instanceId, 0);
    }
    
    /**
     * 获取所有实例的当前权重
     * 
     * @return 实例当前权重映射
     */
    public ConcurrentMap<String, Integer> getCurrentWeights() {
        return new ConcurrentHashMap<>(currentWeights);
    }
} 