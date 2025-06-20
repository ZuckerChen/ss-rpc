package com.ssrpc.spring.boot;

import com.ssrpc.core.annotation.RpcReference;
import com.ssrpc.core.annotation.RpcService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RPC注解功能测试
 * 
 * @author shuang.kou
 * @since 1.0.0
 */
@DisplayName("RPC注解功能测试")
class RpcAnnotationTest {
    
    @Test
    @DisplayName("测试@RpcService注解")
    void testRpcServiceAnnotation() {
        // 验证服务实现类有@RpcService注解
        assertTrue(TestUserServiceImpl.class.isAnnotationPresent(RpcService.class));
        
        RpcService rpcService = TestUserServiceImpl.class.getAnnotation(RpcService.class);
        assertNotNull(rpcService);
        assertEquals("1.0.0", rpcService.version());
        assertEquals(100, rpcService.weight());
    }
    
    @Test
    @DisplayName("测试@RpcReference注解")
    void testRpcReferenceAnnotation() throws NoSuchFieldException {
        // 验证控制器字段有@RpcReference注解
        Field userServiceField = TestUserController.class.getDeclaredField("userService");
        assertTrue(userServiceField.isAnnotationPresent(RpcReference.class));
        
        RpcReference rpcReference = userServiceField.getAnnotation(RpcReference.class);
        assertNotNull(rpcReference);
        assertEquals("1.0.0", rpcReference.version());
        assertEquals(5000, rpcReference.timeout());
        assertFalse(rpcReference.async());
        
        // 验证异步服务字段注解
        Field asyncUserServiceField = TestUserController.class.getDeclaredField("asyncUserService");
        assertTrue(asyncUserServiceField.isAnnotationPresent(RpcReference.class));
        
        RpcReference asyncRpcReference = asyncUserServiceField.getAnnotation(RpcReference.class);
        assertNotNull(asyncRpcReference);
        assertEquals("1.0.0", asyncRpcReference.version());
        assertEquals(3000, asyncRpcReference.timeout());
        assertTrue(asyncRpcReference.async());
    }
    
    @Test
    @DisplayName("测试服务接口方法")
    void testServiceInterfaceMethods() {
        // 验证服务接口的方法定义
        Method[] methods = TestUserService.class.getDeclaredMethods();
        assertTrue(methods.length > 0);
        
        // 验证特定方法存在
        assertDoesNotThrow(() -> {
            TestUserService.class.getMethod("getUserById", Long.class);
            TestUserService.class.getMethod("getAllUsers");
            TestUserService.class.getMethod("createUser", TestUser.class);
            TestUserService.class.getMethod("getUserByIdAsync", Long.class);
            TestUserService.class.getMethod("ping");
            TestUserService.class.getMethod("throwException");
        });
    }
    
    @Test
    @DisplayName("测试服务实现类功能")
    void testServiceImplementation() {
        TestUserServiceImpl service = new TestUserServiceImpl();
        
        // 测试ping方法
        String pingResult = service.ping();
        assertEquals("pong", pingResult);
        
        // 测试获取用户
        TestUser user = service.getUserById(1L);
        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertEquals("张三", user.getName());
        
        // 测试获取所有用户
        List<TestUser> users = service.getAllUsers();
        assertNotNull(users);
        assertEquals(3, users.size());
        
        // 测试创建用户
        TestUser newUser = new TestUser(null, "新用户", "new@example.com", 30);
        TestUser createdUser = service.createUser(newUser);
        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
        assertEquals("新用户", createdUser.getName());
        
        // 测试异常情况
        assertThrows(IllegalArgumentException.class, () -> service.getUserById(null));
        assertThrows(RuntimeException.class, () -> service.getUserById(999L));
        assertThrows(IllegalArgumentException.class, () -> service.createUser(null));
        assertThrows(Exception.class, () -> service.throwException());
    }
    
    @Test
    @DisplayName("测试异步方法")
    void testAsyncMethod() throws Exception {
        TestUserServiceImpl service = new TestUserServiceImpl();
        
        // 测试异步获取用户
        CompletableFuture<TestUser> future = service.getUserByIdAsync(1L);
        assertNotNull(future);
        
        // 等待异步结果
        TestUser user = future.get();
        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertEquals("张三", user.getName());
    }
    
    @Test
    @DisplayName("测试TestUser实体类")
    void testTestUserEntity() {
        TestUser user1 = new TestUser(1L, "测试用户", "test@example.com", 25);
        TestUser user2 = new TestUser(1L, "另一个用户", "another@example.com", 30);
        TestUser user3 = new TestUser(2L, "不同用户", "different@example.com", 28);
        
        // 测试equals方法（基于ID）
        assertEquals(user1, user2); // 相同ID
        assertNotEquals(user1, user3); // 不同ID
        
        // 测试hashCode方法
        assertEquals(user1.hashCode(), user2.hashCode()); // 相同ID应该有相同hashCode
        
        // 测试toString方法
        String userString = user1.toString();
        assertTrue(userString.contains("TestUser"));
        assertTrue(userString.contains("id=1"));
        assertTrue(userString.contains("name='测试用户'"));
        
        // 测试getter/setter
        user1.setName("更新的名字");
        assertEquals("更新的名字", user1.getName());
        
        user1.setAge(35);
        assertEquals(35, user1.getAge());
        
        user1.setEmail("updated@example.com");
        assertEquals("updated@example.com", user1.getEmail());
    }
    
    @Test
    @DisplayName("测试控制器方法定义")
    void testControllerMethods() {
        // 验证控制器的方法定义
        assertDoesNotThrow(() -> {
            TestUserController.class.getMethod("getUser", Long.class);
            TestUserController.class.getMethod("getAllUsers");
            TestUserController.class.getMethod("createUser", TestUser.class);
            TestUserController.class.getMethod("getUserAsync", Long.class);
            TestUserController.class.getMethod("ping");
            TestUserController.class.getMethod("testException");
            TestUserController.class.getMethod("getUserService");
            TestUserController.class.getMethod("getAsyncUserService");
        });
    }
} 