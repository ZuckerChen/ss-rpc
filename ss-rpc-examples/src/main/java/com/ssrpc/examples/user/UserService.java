package com.ssrpc.examples.user;

import java.util.concurrent.CompletableFuture;

/**
 * 用户服务接口
 * 
 * @author SS-RPC Team
 * @since 1.0.0
 */
public interface UserService {
    
    /**
     * 根据ID获取用户
     * @param id 用户ID
     * @return 用户信息
     */
    User getUserById(Long id);
    
    /**
     * 异步获取用户
     * @param id 用户ID
     * @return 异步用户信息
     */
    CompletableFuture<User> getUserAsync(Long id);
    
    /**
     * 创建用户
     * @param user 用户信息
     * @return 创建的用户
     */
    User createUser(User user);
} 