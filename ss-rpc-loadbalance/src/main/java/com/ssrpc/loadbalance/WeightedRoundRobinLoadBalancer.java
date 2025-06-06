package com.ssrpc.loadbalance;

import com.ssrpc.protocol.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 加权轮询负载均衡器.
 * 
 * 根据服务实例的权重进行轮询选择，权重越高的实例被选中的概率越大
 * 适合服务实例性能差异较大的场景
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class WeightedRoundRobinLoadBalancer implements LoadBalancer {
    
    private static final Logger log = LoggerFactory.getLogger(WeightedRoundRobinLoadBalancer.class);
    
    /**
     * 实例权重信息
     */
    private static class WeightInfo {
        private final int weight;          // 实例权重
        private final AtomicInteger current;   // 当前权重值
        
        public WeightInfo(int weight) {
            this.weight = weight;
            this.current = new AtomicInteger(0);
        }
        
        public int getWeight() {
            return weight;
        }
        
        public int getCurrentWeight() {
            return current.get();
        }
        
        public int addAndGet(int delta) {
            return current.addAndGet(delta);
        }
        
        public void set(int value) {
            current.set(value);
        }
    }
    
    private final ConcurrentHashMap<String, WeightInfo> weightMap = new ConcurrentHashMap<>();
    
    @Override
    public ServiceInstance select(List<ServiceInstance> instances) throws LoadBalanceException {
        if (instances == null || instances.isEmpty()) {
            throw new LoadBalanceException(
                LoadBalanceException.ErrorCodes.NO_AVAILABLE_INSTANCE,
                "No available service instances"
            );
        }
        
        // 过滤出健康的实例
        List<ServiceInstance> healthyInstances = instances.stream()
                .filter(ServiceInstance::isHealthy)
                .collect(Collectors.toList());
        
        if (healthyInstances.isEmpty()) {
            throw new LoadBalanceException(
                LoadBalanceException.ErrorCodes.NO_AVAILABLE_INSTANCE,
                "No healthy service instances available"
            );
        }
        
        // 如果只有一个实例，直接返回
        if (healthyInstances.size() == 1) {
            ServiceInstance selected = healthyInstances.get(0);
            return selected;
        }
        
        // 使用加权轮询算法选择实例
        ServiceInstance selected = selectByWeight(healthyInstances);
        
        log.debug("Selected instance {} with weight {} from {} healthy instances using weighted round-robin strategy", 
                 selected.getInstanceId(), selected.getWeight(), healthyInstances.size());
        
        return selected;
    }
    
    /**
     * 加权轮询选择算法
     * 使用平滑加权轮询算法（Smooth Weighted Round Robin）
     */
    private ServiceInstance selectByWeight(List<ServiceInstance> instances) {
        // 计算总权重
        int totalWeight = 0;
        ServiceInstance maxWeightInstance = null;
        int maxCurrentWeight = Integer.MIN_VALUE;
        
        for (ServiceInstance instance : instances) {
            String instanceId = instance.getInstanceId();
            int weight = instance.getWeight();
            
            // 确保权重为正数
            if (weight <= 0) {
                weight = 1;
            }
            
            // 获取或创建权重信息
            int finalWeight = weight;
            WeightInfo weightInfo = weightMap.computeIfAbsent(instanceId, k -> new WeightInfo(finalWeight));
            
            // 更新权重信息（如果实例权重发生变化）
            if (weightInfo.getWeight() != weight) {
                weightMap.put(instanceId, new WeightInfo(weight));
                weightInfo = weightMap.get(instanceId);
            }
            
            totalWeight += weight;
            
            // 增加当前权重
            int currentWeight = weightInfo.addAndGet(weight);
            
            // 找到当前权重最大的实例
            if (currentWeight > maxCurrentWeight) {
                maxCurrentWeight = currentWeight;
                maxWeightInstance = instance;
            }
        }
        
        if (maxWeightInstance == null) {
            // 防御性编程，理论上不会到达这里
            return instances.get(0);
        }
        
        // 将选中实例的当前权重减去总权重
        String selectedInstanceId = maxWeightInstance.getInstanceId();
        WeightInfo selectedWeightInfo = weightMap.get(selectedInstanceId);
        if (selectedWeightInfo != null) {
            selectedWeightInfo.addAndGet(-totalWeight);
        }
        
        return maxWeightInstance;
    }
    
    @Override
    public LoadBalanceType getType() {
        return LoadBalanceType.WEIGHTED_ROUND_ROBIN;
    }
    
    @Override
    public void reset() {
        weightMap.clear();
        log.debug("Weighted round-robin load balancer reset");
    }
    
    /**
     * 更新实例权重
     */
    public void updateWeight(String instanceId, int weight) {
        if (weight <= 0) {
            throw new LoadBalanceException(
                LoadBalanceException.ErrorCodes.INVALID_WEIGHT,
                "Weight must be positive: " + weight
            );
        }
        
        weightMap.compute(instanceId, (key, oldWeightInfo) -> {
            if (oldWeightInfo == null) {
                return new WeightInfo(weight);
            } else {
                // 保持当前权重值，只更新基础权重
                WeightInfo newWeightInfo = new WeightInfo(weight);
                newWeightInfo.set(oldWeightInfo.getCurrentWeight());
                return newWeightInfo;
            }
        });
        
        log.debug("Updated weight for instance {} to {}", instanceId, weight);
    }
    
    /**
     * 获取实例权重
     */
    public int getWeight(String instanceId) {
        WeightInfo weightInfo = weightMap.get(instanceId);
        return weightInfo != null ? weightInfo.getWeight() : -1;
    }
    
    /**
     * 获取所有实例的权重信息
     */
    public java.util.Map<String, String> getWeightInfos() {
        return weightMap.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    java.util.Map.Entry::getKey,
                    entry -> {
                        WeightInfo info = entry.getValue();
                        return String.format("weight=%d, current=%d", 
                               info.getWeight(), info.getCurrentWeight());
                    }
                ));
    }
    
    @Override
    public String toString() {
        return "WeightedRoundRobinLoadBalancer{" +
                "type=" + getType() +
                ", instanceCount=" + weightMap.size() +
                '}';
    }
} 