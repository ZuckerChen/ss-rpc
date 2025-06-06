package com.ssrpc.loadbalance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 负载均衡类型测试.
 * 
 * @author chenzhang
 * @since 1.0.0
 */
class LoadBalanceTypeTest {

    @Test
    void testGetCode() {
        assertEquals("random", LoadBalanceType.RANDOM.getCode());
        assertEquals("round_robin", LoadBalanceType.ROUND_ROBIN.getCode());
        assertEquals("weighted_round_robin", LoadBalanceType.WEIGHTED_ROUND_ROBIN.getCode());
        assertEquals("consistent_hash", LoadBalanceType.CONSISTENT_HASH.getCode());
        assertEquals("least_active", LoadBalanceType.LEAST_ACTIVE.getCode());
        assertEquals("adaptive", LoadBalanceType.ADAPTIVE.getCode());
    }

    @Test
    void testFromCode() {
        assertEquals(LoadBalanceType.RANDOM, LoadBalanceType.fromCode("random"));
        assertEquals(LoadBalanceType.ROUND_ROBIN, LoadBalanceType.fromCode("round_robin"));
        assertEquals(LoadBalanceType.ADAPTIVE, LoadBalanceType.fromCode("adaptive"));
    }

    @Test
    void testFromCodeWithInvalidCode() {
        assertThrows(IllegalArgumentException.class, () -> {
            LoadBalanceType.fromCode("invalid_code");
        });
    }

    @Test
    void testAllTypesHaveUniqueCode() {
        LoadBalanceType[] types = LoadBalanceType.values();
        
        for (int i = 0; i < types.length; i++) {
            for (int j = i + 1; j < types.length; j++) {
                assertNotEquals(types[i].getCode(), types[j].getCode(), 
                    "LoadBalanceType codes should be unique");
            }
        }
    }
} 