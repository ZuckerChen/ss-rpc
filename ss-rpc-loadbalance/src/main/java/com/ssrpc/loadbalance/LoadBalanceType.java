package com.ssrpc.loadbalance;

/**
 * 负载均衡类型枚举
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public enum LoadBalanceType {
    
    /**
     * 随机负载均衡
     */
    RANDOM("random"),
    
    /**
     * 轮询负载均衡
     */
    ROUND_ROBIN("round_robin"),
    
    /**
     * 加权轮询负载均衡
     */
    WEIGHTED_ROUND_ROBIN("weighted_round_robin"),
    
    /**
     * 一致性哈希负载均衡
     */
    CONSISTENT_HASH("consistent_hash"),
    
    /**
     * 最少活跃数负载均衡
     */
    LEAST_ACTIVE("least_active"),
    
    /**
     * 自适应负载均衡（AI驱动）
     */
    ADAPTIVE("adaptive");
    
    private final String code;
    
    LoadBalanceType(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    public static LoadBalanceType fromCode(String code) {
        for (LoadBalanceType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown load balance type: " + code);
    }
} 