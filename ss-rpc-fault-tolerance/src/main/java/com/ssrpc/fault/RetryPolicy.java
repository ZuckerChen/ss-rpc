package com.ssrpc.fault;

import java.util.function.Predicate;

/**
 * 重试策略接口.
 * 
 * 定义RPC调用失败时的重试机制，包括重试次数、重试间隔、重试条件等
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public interface RetryPolicy {
    
    /**
     * 判断是否应该重试
     * 
     * @param attempt 当前尝试次数（从1开始）
     * @param exception 发生的异常
     * @return true表示应该重试，false表示不重试
     */
    boolean shouldRetry(int attempt, Throwable exception);
    
    /**
     * 获取下次重试的延迟时间（毫秒）
     * 
     * @param attempt 当前尝试次数（从1开始）
     * @return 延迟时间，0表示立即重试
     */
    long getRetryDelay(int attempt);
    
    /**
     * 获取最大重试次数
     * 
     * @return 最大重试次数，0表示不重试
     */
    int getMaxRetries();
    
    /**
     * 获取最大重试时间（毫秒）
     * 
     * @return 最大重试时间，超过此时间则停止重试
     */
    long getMaxRetryTime();
    
    /**
     * 创建无重试策略
     */
    static RetryPolicy noRetry() {
        return new NoRetryPolicy();
    }
    
    /**
     * 创建固定次数重试策略
     */
    static RetryPolicy fixedRetry(int maxRetries) {
        return new FixedRetryPolicy(maxRetries, 0);
    }
    
    /**
     * 创建固定次数和间隔的重试策略
     */
    static RetryPolicy fixedRetry(int maxRetries, long retryDelay) {
        return new FixedRetryPolicy(maxRetries, retryDelay);
    }
    
    /**
     * 创建指数退避重试策略
     */
    static RetryPolicy exponentialBackoff(int maxRetries, long initialDelay, long maxDelay) {
        return new ExponentialBackoffRetryPolicy(maxRetries, initialDelay, maxDelay);
    }
    
    /**
     * 重试延迟函数接口
     */
    @FunctionalInterface
    interface RetryDelayFunction {
        /**
         * 计算重试延迟
         * 
         * @param attempt 当前尝试次数
         * @return 延迟时间（毫秒）
         */
        long getDelay(int attempt);
    }
    
    /**
     * 无重试策略实现
     */
    class NoRetryPolicy implements RetryPolicy {
        
        @Override
        public boolean shouldRetry(int attempt, Throwable exception) {
            return false;
        }
        
        @Override
        public long getRetryDelay(int attempt) {
            return 0;
        }
        
        @Override
        public int getMaxRetries() {
            return 0;
        }
        
        @Override
        public long getMaxRetryTime() {
            return 0;
        }
    }
    
    /**
     * 固定重试策略实现
     */
    class FixedRetryPolicy implements RetryPolicy {
        
        private final int maxRetries;
        private final long retryDelay;
        private final long maxRetryTime;
        private final Predicate<Throwable> retryCondition;
        
        public FixedRetryPolicy(int maxRetries, long retryDelay) {
            this(maxRetries, retryDelay, Long.MAX_VALUE, createDefaultRetryCondition());
        }
        
        public FixedRetryPolicy(int maxRetries, long retryDelay, long maxRetryTime, 
                               Predicate<Throwable> retryCondition) {
            this.maxRetries = maxRetries;
            this.retryDelay = retryDelay;
            this.maxRetryTime = maxRetryTime;
            this.retryCondition = retryCondition;
        }
        
        @Override
        public boolean shouldRetry(int attempt, Throwable exception) {
            return attempt <= maxRetries && retryCondition.test(exception);
        }
        
        @Override
        public long getRetryDelay(int attempt) {
            return retryDelay;
        }
        
        @Override
        public int getMaxRetries() {
            return maxRetries;
        }
        
        @Override
        public long getMaxRetryTime() {
            return maxRetryTime;
        }
        
        /**
         * 创建默认的重试条件
         */
        private static Predicate<Throwable> createDefaultRetryCondition() {
            return exception -> {
                // 网络异常、超时异常等可以重试
                return exception instanceof java.net.ConnectException ||
                       exception instanceof java.net.SocketTimeoutException ||
                       exception instanceof java.util.concurrent.TimeoutException;
            };
        }
    }
    
    /**
     * 指数退避重试策略实现
     */
    class ExponentialBackoffRetryPolicy implements RetryPolicy {
        
        private final int maxRetries;
        private final long initialDelay;
        private final long maxDelay;
        private final double multiplier;
        private final long maxRetryTime;
        private final Predicate<Throwable> retryCondition;
        
        public ExponentialBackoffRetryPolicy(int maxRetries, long initialDelay, long maxDelay) {
            this(maxRetries, initialDelay, maxDelay, 2.0, Long.MAX_VALUE, createDefaultRetryCondition());
        }
        
        public ExponentialBackoffRetryPolicy(int maxRetries, long initialDelay, long maxDelay,
                                           double multiplier, long maxRetryTime,
                                           Predicate<Throwable> retryCondition) {
            this.maxRetries = maxRetries;
            this.initialDelay = initialDelay;
            this.maxDelay = maxDelay;
            this.multiplier = multiplier;
            this.maxRetryTime = maxRetryTime;
            this.retryCondition = retryCondition;
        }
        
        @Override
        public boolean shouldRetry(int attempt, Throwable exception) {
            return attempt <= maxRetries && retryCondition.test(exception);
        }
        
        @Override
        public long getRetryDelay(int attempt) {
            if (attempt <= 1) {
                return 0;
            }
            
            // 计算指数退避延迟：initialDelay * multiplier^(attempt-2)
            long delay = (long) (initialDelay * Math.pow(multiplier, attempt - 2));
            
            // 限制最大延迟
            return Math.min(delay, maxDelay);
        }
        
        @Override
        public int getMaxRetries() {
            return maxRetries;
        }
        
        @Override
        public long getMaxRetryTime() {
            return maxRetryTime;
        }
        
        /**
         * 创建默认的重试条件
         */
        private static Predicate<Throwable> createDefaultRetryCondition() {
            return exception -> {
                // 网络异常、超时异常等可以重试
                return exception instanceof java.net.ConnectException ||
                       exception instanceof java.net.SocketTimeoutException ||
                       exception instanceof java.util.concurrent.TimeoutException;
            };
        }
    }
} 