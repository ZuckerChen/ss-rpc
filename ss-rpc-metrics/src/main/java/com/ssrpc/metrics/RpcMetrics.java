package com.ssrpc.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * RPC监控指标管理类.
 * 
 * 负责收集和管理RPC调用的各种指标，包括调用次数、成功率、响应时间等
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@Slf4j
public class RpcMetrics {
    
    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    
    public RpcMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    /**
     * 记录RPC调用开始
     * 
     * @param serviceName 服务名称
     * @param methodName 方法名称
     * @return 计时器Sample
     */
    public Timer.Sample startTimer(String serviceName, String methodName) {
        String timerName = createTimerName(serviceName, methodName);
        Timer timer = timers.computeIfAbsent(timerName, name -> 
            Timer.builder("rpc.call.duration")
                .description("RPC call duration")
                .tag("service", serviceName)
                .tag("method", methodName)
                .register(meterRegistry)
        );
        return Timer.start(meterRegistry);
    }
    
    /**
     * 记录RPC调用结束
     * 
     * @param sample 计时器Sample
     * @param serviceName 服务名称
     * @param methodName 方法名称
     * @param success 是否成功
     */
    public void stopTimer(Timer.Sample sample, String serviceName, String methodName, boolean success) {
        String timerName = createTimerName(serviceName, methodName);
        Timer timer = timers.get(timerName);
        if (timer != null && sample != null) {
            sample.stop(timer);
        }
        
        // 记录调用次数
        incrementCounter(serviceName, methodName, success ? "success" : "failure");
    }
    
    /**
     * 增加计数器
     * 
     * @param serviceName 服务名称
     * @param methodName 方法名称
     * @param status 状态
     */
    public void incrementCounter(String serviceName, String methodName, String status) {
        String counterName = createCounterName(serviceName, methodName, status);
        Counter counter = counters.computeIfAbsent(counterName, name ->
            Counter.builder("rpc.call.total")
                .description("Total RPC calls")
                .tag("service", serviceName)
                .tag("method", methodName)
                .tag("status", status)
                .register(meterRegistry)
        );
        counter.increment();
    }
    
    /**
     * 记录异常
     * 
     * @param serviceName 服务名称
     * @param methodName 方法名称
     * @param exception 异常
     */
    public void recordException(String serviceName, String methodName, Throwable exception) {
        String exceptionType = exception.getClass().getSimpleName();
        String counterName = createExceptionCounterName(serviceName, methodName, exceptionType);
        Counter counter = counters.computeIfAbsent(counterName, name ->
            Counter.builder("rpc.call.exceptions")
                .description("RPC call exceptions")
                .tag("service", serviceName)
                .tag("method", methodName)
                .tag("exception", exceptionType)
                .register(meterRegistry)
        );
        counter.increment();
    }
    
    /**
     * 创建计时器名称
     */
    private String createTimerName(String serviceName, String methodName) {
        return String.format("timer_%s_%s", serviceName, methodName);
    }
    
    /**
     * 创建计数器名称
     */
    private String createCounterName(String serviceName, String methodName, String status) {
        return String.format("counter_%s_%s_%s", serviceName, methodName, status);
    }
    
    /**
     * 创建异常计数器名称
     */
    private String createExceptionCounterName(String serviceName, String methodName, String exceptionType) {
        return String.format("exception_%s_%s_%s", serviceName, methodName, exceptionType);
    }
    
    /**
     * 获取监控指标注册表
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
} 