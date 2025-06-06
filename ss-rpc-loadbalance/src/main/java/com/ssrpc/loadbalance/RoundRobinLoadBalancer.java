package com.ssrpc.loadbalance;

import com.ssrpc.protocol.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡器实现.
 * 
 * 按照轮询方式依次选择服务实例，保证请求均匀分布
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class RoundRobinLoadBalancer implements LoadBalancer {
    
    private static final Logger log = LoggerFactory.getLogger(RoundRobinLoadBalancer.class);
    
    private final AtomicInteger counter = new AtomicInteger(0);
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
        
        // 轮询选择
        int index = getNextIndex(healthyInstances.size());
        ServiceInstance selected = healthyInstances.get(index);
        
        // 记录统计信息
        stats.recordSelection(selected.getInstanceId());
        
        log.debug("Round-robin load balancer selected instance: {} (index: {})", 
                selected.getInstanceId(), index);
        return selected;
    }
    
    /**
     * 获取下一个索引
     */
    private int getNextIndex(int size) {
        if (size <= 0) {
            return 0;
        }
        
        int current = counter.getAndIncrement();
        // 防止整数溢出
        if (current < 0) {
            counter.set(0);
            current = 0;
        }
        
        return current % size;
    }
    
    @Override
    public LoadBalanceType getType() {
        return LoadBalanceType.ROUND_ROBIN;
    }
    
    @Override
    public LoadBalanceStats getStats() {
        return stats;
    }
    
    @Override
    public void reset() {
        counter.set(0);
        stats.reset();
        log.debug("Round-robin load balancer stats reset");
    }
    
    /**
     * 获取当前计数器值
     * 
     * @return 当前计数器值
     */
    public int getCurrentCounter() {
        return counter.get();
    }
} 