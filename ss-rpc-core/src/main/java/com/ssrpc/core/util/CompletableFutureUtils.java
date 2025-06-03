package com.ssrpc.core.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * CompletableFuture工具类.
 * 
 * 提供Java 8兼容的CompletableFuture操作方法
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public final class CompletableFutureUtils {
    
    private static final ScheduledExecutorService TIMEOUT_EXECUTOR = 
        new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "CompletableFuture-Timeout");
            t.setDaemon(true);
            return t;
        });
    
    private CompletableFutureUtils() {
        // 工具类，不允许实例化
    }
    
    /**
     * 创建已失败的CompletableFuture（Java 8兼容）
     * 
     * @param throwable 异常
     * @param <T> 返回类型
     * @return 已失败的CompletableFuture
     */
    public static <T> CompletableFuture<T> failedFuture(Throwable throwable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }
    
    /**
     * 为CompletableFuture添加超时（Java 8兼容）
     * 
     * @param future 原始Future
     * @param timeout 超时时间
     * @param unit 时间单位
     * @param <T> 返回类型
     * @return 带超时的CompletableFuture
     */
    public static <T> CompletableFuture<T> orTimeout(CompletableFuture<T> future, 
                                                    long timeout, TimeUnit unit) {
        if (future.isDone()) {
            return future;
        }
        
        CompletableFuture<T> result = new CompletableFuture<>();
        
        // 当原始future完成时，完成结果future
        future.whenComplete((value, throwable) -> {
            if (throwable != null) {
                result.completeExceptionally(throwable);
            } else {
                result.complete(value);
            }
        });
        
        // 设置超时
        TIMEOUT_EXECUTOR.schedule(() -> {
            if (!result.isDone()) {
                result.completeExceptionally(new TimeoutException("Future timed out after " + 
                    timeout + " " + unit.toString().toLowerCase()));
            }
        }, timeout, unit);
        
        return result;
    }
    
    /**
     * 创建已完成的CompletableFuture
     * 
     * @param value 值
     * @param <T> 返回类型
     * @return 已完成的CompletableFuture
     */
    public static <T> CompletableFuture<T> completedFuture(T value) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.complete(value);
        return future;
    }
    
    /**
     * 关闭超时执行器（在应用关闭时调用）
     */
    public static void shutdown() {
        TIMEOUT_EXECUTOR.shutdown();
    }
} 