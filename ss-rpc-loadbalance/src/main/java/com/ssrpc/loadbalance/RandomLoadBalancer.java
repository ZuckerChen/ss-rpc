package com.ssrpc.loadbalance;

import com.ssrpc.protocol.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

/**
 * 随机负载均衡器实现.
 * 
 * 随机选择一个可用的服务实例
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class RandomLoadBalancer implements LoadBalancer {
    
    private static final Logger log = LoggerFactory.getLogger(RandomLoadBalancer.class);
    
    private final Random random = new Random();
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
        
        // 随机选择
        int index = random.nextInt(healthyInstances.size());
        ServiceInstance selected = healthyInstances.get(index);
        
        // 记录统计信息
        stats.recordSelection(selected.getInstanceId());
        
        log.debug("Random load balancer selected instance: {}", selected.getInstanceId());
        return selected;
    }
    
    @Override
    public LoadBalanceType getType() {
        return LoadBalanceType.RANDOM;
    }
    
    @Override
    public LoadBalanceStats getStats() {
        return stats;
    }
    
    @Override
    public void reset() {
        stats.reset();
        log.debug("Random load balancer stats reset");
    }
} 