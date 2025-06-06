package com.ssrpc.loadbalance;

import com.ssrpc.protocol.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 轮询负载均衡器.
 * 
 * 按照轮询策略依次选择服务实例
 * 保证每个实例都能被平均分配请求
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class RoundRobinLoadBalancer implements LoadBalancer {
    
    private static final Logger log = LoggerFactory.getLogger(RoundRobinLoadBalancer.class);
    
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    
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
        
        // 轮询选择一个实例
        int index = currentIndex.getAndIncrement() % healthyInstances.size();
        ServiceInstance selected = healthyInstances.get(index);
        
        log.debug("Selected instance {} from {} healthy instances using round-robin strategy (index: {})", 
                 selected.getInstanceId(), healthyInstances.size(), index);
        
        return selected;
    }
    
    @Override
    public LoadBalanceType getType() {
        return LoadBalanceType.ROUND_ROBIN;
    }
    
    @Override
    public void reset() {
        currentIndex.set(0);
        log.debug("Round-robin load balancer index reset");
    }
    
    @Override
    public String toString() {
        return "RoundRobinLoadBalancer{" +
                "type=" + getType() +
                ", currentIndex=" + currentIndex.get() +
                '}';
    }
} 