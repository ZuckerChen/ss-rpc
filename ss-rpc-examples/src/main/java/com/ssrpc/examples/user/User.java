package com.ssrpc.examples.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户实体类.
 * 
 * 用于演示SS-RPC框架的基本功能
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    private Long id;
    private String name;
    private String email;
} 