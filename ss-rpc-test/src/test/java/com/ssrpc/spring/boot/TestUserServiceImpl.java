package com.ssrpc.spring.boot;

import com.ssrpc.core.annotation.RpcService;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 测试用的用户服务实现类
 * 
 * @author shuang.kou
 * @since 1.0.0
 */
@RpcService(version = "1.0.0", weight = 100)
public class TestUserServiceImpl implements TestUserService {
    
    private final ConcurrentMap<Long, TestUser> userStorage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    public TestUserServiceImpl() {
        // 初始化一些测试数据
        userStorage.put(1L, new TestUser(1L, "张三", "zhangsan@example.com", 25));
        userStorage.put(2L, new TestUser(2L, "李四", "lisi@example.com", 30));
        userStorage.put(3L, new TestUser(3L, "王五", "wangwu@example.com", 28));
    }
    
    @Override
    public TestUser getUserById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        TestUser user = userStorage.get(id);
        if (user == null) {
            throw new RuntimeException("User not found with id: " + id);
        }
        
        return user;
    }
    
    @Override
    public List<TestUser> getAllUsers() {
        return Arrays.asList(userStorage.values().toArray(new TestUser[0]));
    }
    
    @Override
    public TestUser createUser(TestUser user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        if (user.getId() == null) {
            user.setId(idGenerator.getAndIncrement());
        }
        
        userStorage.put(user.getId(), user);
        return user;
    }
    
    @Override
    public CompletableFuture<TestUser> getUserByIdAsync(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 模拟异步处理延迟
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return getUserById(id);
        });
    }
    
    @Override
    public String ping() {
        return "pong";
    }
    
    @Override
    public void throwException() throws Exception {
        throw new Exception("Test exception from service");
    }
} 