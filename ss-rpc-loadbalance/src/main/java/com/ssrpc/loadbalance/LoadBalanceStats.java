package com.ssrpc.loadbalance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负载均衡统计信息.
 * 
 * 记录负载均衡器的选择统计数据
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class LoadBalanceStats {
    
    /** 总选择次数 */
    private long totalSelections = 0;
    
    /** 实例选择次数统计 */
    private Map<String, Long> instanceSelections = new ConcurrentHashMap<>();
    
    /** 最后选择时间 */
    private long lastSelectTime = 0;
    
    public long getTotalSelections() {
        return totalSelections;
    }
    
    public void setTotalSelections(long totalSelections) {
        this.totalSelections = totalSelections;
    }
    
    public Map<String, Long> getInstanceSelections() {
        return instanceSelections;
    }
    
    public void setInstanceSelections(Map<String, Long> instanceSelections) {
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
        return "LoadBalanceStats{" +
                "totalSelections=" + totalSelections +
                ", instanceCount=" + instanceSelections.size() +
                ", lastSelectTime=" + lastSelectTime +
                '}';
    }
} 