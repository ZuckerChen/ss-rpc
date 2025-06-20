package com.ssrpc.spring.boot;

import com.ssrpc.core.annotation.RpcReference;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 测试用的用户控制器
 * 
 * @author shuang.kou
 * @since 1.0.0
 */
public class TestUserController {
    
    @RpcReference(version = "1.0.0", timeout = 5000)
    private TestUserService userService;
    
    @RpcReference(version = "1.0.0", timeout = 3000, async = true)
    private TestUserService asyncUserService;
    
    public TestUser getUser(Long id) {
        return userService.getUserById(id);
    }
    
    public List<TestUser> getAllUsers() {
        return userService.getAllUsers();
    }
    
    public TestUser createUser(TestUser user) {
        return userService.createUser(user);
    }
    
    public CompletableFuture<TestUser> getUserAsync(Long id) {
        return asyncUserService.getUserByIdAsync(id);
    }
    
    public String ping() {
        return userService.ping();
    }
    
    public void testException() throws Exception {
        userService.throwException();
    }
    
    // Getter for testing
    public TestUserService getUserService() {
        return userService;
    }
    
    public TestUserService getAsyncUserService() {
        return asyncUserService;
    }
} 