package com.ssrpc.core.invoker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReflectionServiceInvoker异步方法测试
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class ReflectionServiceInvokerAsyncTest {
    
    // 测试服务接口
    public interface AsyncTestService {
        String syncMethod(String input);
        CompletableFuture<String> asyncMethod(String input);
        CompletableFuture<Integer> asyncCalculate(int a, int b);
    }
    
    // 测试服务实现
    public static class AsyncTestServiceImpl implements AsyncTestService {
        @Override
        public String syncMethod(String input) {
            return "Sync: " + input;
        }
        
        @Override
        public CompletableFuture<String> asyncMethod(String input) {
            return CompletableFuture.completedFuture("Async: " + input);
        }
        
        @Override
        public CompletableFuture<Integer> asyncCalculate(int a, int b) {
            return CompletableFuture.completedFuture(a + b);
        }
    }
    
    private ReflectionServiceInvoker invoker;
    
    @BeforeEach
    void setUp() {
        AsyncTestService service = new AsyncTestServiceImpl();
        invoker = new ReflectionServiceInvoker(
            AsyncTestService.class,
            service,
            "AsyncTestService",
            "1.0.0"
        );
    }
    
    @Test
    @DisplayName("测试同步方法调用")
    void testSyncMethod() throws Exception {
        Object result = invoker.invoke("syncMethod", 
            new Class[]{String.class}, 
            new Object[]{"test"});
        
        assertEquals("Sync: test", result);
    }
    
    @Test
    @DisplayName("测试异步方法调用 - 应该等待Future完成并返回实际结果")
    void testAsyncMethod() throws Exception {
        Object result = invoker.invoke("asyncMethod", 
            new Class[]{String.class}, 
            new Object[]{"test"});
        
        // 异步方法应该返回实际结果，而不是CompletableFuture对象
        assertNotNull(result);
        assertFalse(result instanceof CompletableFuture, "结果不应该是CompletableFuture对象");
        assertEquals("Async: test", result);
    }
    
    @Test
    @DisplayName("测试异步计算方法")
    void testAsyncCalculate() throws Exception {
        Object result = invoker.invoke("asyncCalculate", 
            new Class[]{int.class, int.class}, 
            new Object[]{10, 20});
        
        // 异步方法应该返回实际结果
        assertNotNull(result);
        assertFalse(result instanceof CompletableFuture, "结果不应该是CompletableFuture对象");
        assertEquals(30, result);
    }
} 