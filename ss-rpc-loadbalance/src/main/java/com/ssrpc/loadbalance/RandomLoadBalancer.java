package com.ssrpc.loadbalance;

import com.ssrpc.protocol.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 随机负载均衡器.
 * 
 * 从可用的服务实例中随机选择一个实例
 * 简单高效，适合大部分场景
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class RandomLoadBalancer implements LoadBalancer {
    
    private static final Logger log = LoggerFactory.getLogger(RandomLoadBalancer.class);
    
    private final Random random = new Random();
    
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
        
        // 随机选择一个实例
        int index = random.nextInt(healthyInstances.size());
        ServiceInstance selected = healthyInstances.get(index);
        
        log.debug("Selected instance {} from {} healthy instances using random strategy", 
                 selected.getInstanceId(), healthyInstances.size());
        
        return selected;
    }
    
    @Override
    public LoadBalanceType getType() {
        return LoadBalanceType.RANDOM;
    }
    
    @Override
    public String toString() {
        return "RandomLoadBalancer{" +
                "type=" + getType() +
                '}';
    }
} 