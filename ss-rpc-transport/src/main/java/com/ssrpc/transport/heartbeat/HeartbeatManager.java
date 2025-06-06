package com.ssrpc.transport.heartbeat;

import com.ssrpc.core.rpc.RpcRequest;
import com.ssrpc.core.rpc.RpcResponse;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 心跳管理器
 * 
 * 负责管理心跳发送和检测，维护连接健康状态
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class HeartbeatManager {
    
    private static final Logger log = LoggerFactory.getLogger(HeartbeatManager.class);
    
    private final ScheduledExecutorService scheduler;
    
    public HeartbeatManager() {
        this.scheduler = new ScheduledThreadPoolExecutor(
            2, 
            new HeartbeatThreadFactory()
        );
    }
    
    /**
     * 启动定时心跳发送
     * 
     * @param task 心跳任务
     * @param intervalSeconds 心跳间隔（秒）
     */
    public void startHeartbeat(Runnable task, int intervalSeconds) {
        scheduler.scheduleWithFixedDelay(
            task, 
            intervalSeconds, 
            intervalSeconds, 
            TimeUnit.SECONDS
        );
        log.info("Heartbeat started with interval {} seconds", intervalSeconds);
    }
    
    /**
     * 停止心跳管理器
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("HeartbeatManager shutdown completed");
    }
    
    /**
     * 心跳线程工厂
     */
    private static class HeartbeatThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "ss-rpc-heartbeat-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
} 