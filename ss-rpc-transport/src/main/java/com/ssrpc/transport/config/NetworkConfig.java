package com.ssrpc.transport.config;

/**
 * 网络传输配置.
 * 
 * 配置Netty传输层的各种参数
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class NetworkConfig {
    
    // ===== 线程配置 =====
    
    /**
     * Worker线程数
     */
    private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;
    
    // ===== 连接配置 =====
    
    /**
     * 是否启用TCP KeepAlive
     */
    private boolean keepAlive = true;
    
    /**
     * 是否禁用Nagle算法
     */
    private boolean tcpNoDelay = true;
    
    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 3000;
    
    /**
     * 接收缓冲区大小
     */
    private int receiveBufferSize = 65536;
    
    /**
     * 发送缓冲区大小
     */
    private int sendBufferSize = 65536;
    
    // ===== 内存配置 =====
    
    /**
     * 是否使用池化内存分配器
     */
    private boolean usePooledAllocator = true;
    
    /**
     * 是否使用直接内存
     */
    private boolean useDirectBuffer = true;
    
    // ===== 心跳配置 =====
    
    /**
     * 是否启用心跳
     */
    private boolean heartbeatEnabled = true;
    
    /**
     * 心跳间隔（秒）
     */
    private int heartbeatInterval = 30;
    
    // ===== 请求配置 =====
    
    /**
     * 请求超时时间（毫秒）
     */
    private long requestTimeout = 5000L;
    
    // ===== Getter 和 Setter 方法 =====
    
    public int getWorkerThreads() {
        return workerThreads;
    }
    
    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }
    
    public boolean isKeepAlive() {
        return keepAlive;
    }
    
    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }
    
    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }
    
    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }
    
    public int getConnectTimeout() {
        return connectTimeout;
    }
    
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
    
    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }
    
    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }
    
    public int getSendBufferSize() {
        return sendBufferSize;
    }
    
    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }
    
    public boolean isUsePooledAllocator() {
        return usePooledAllocator;
    }
    
    public void setUsePooledAllocator(boolean usePooledAllocator) {
        this.usePooledAllocator = usePooledAllocator;
    }
    
    public boolean isUseDirectBuffer() {
        return useDirectBuffer;
    }
    
    public void setUseDirectBuffer(boolean useDirectBuffer) {
        this.useDirectBuffer = useDirectBuffer;
    }
    
    public boolean isHeartbeatEnabled() {
        return heartbeatEnabled;
    }
    
    public void setHeartbeatEnabled(boolean heartbeatEnabled) {
        this.heartbeatEnabled = heartbeatEnabled;
    }
    
    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }
    
    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }
    
    public long getRequestTimeout() {
        return requestTimeout;
    }
    
    public void setRequestTimeout(long requestTimeout) {
        this.requestTimeout = requestTimeout;
    }
    
    /**
     * 验证配置
     */
    public void validate() {
        if (workerThreads <= 0) {
            throw new IllegalArgumentException("Worker threads must be positive: " + workerThreads);
        }
        
        if (connectTimeout <= 0) {
            throw new IllegalArgumentException("Connect timeout must be positive: " + connectTimeout);
        }
        
        if (receiveBufferSize <= 0) {
            throw new IllegalArgumentException("Receive buffer size must be positive: " + receiveBufferSize);
        }
        
        if (sendBufferSize <= 0) {
            throw new IllegalArgumentException("Send buffer size must be positive: " + sendBufferSize);
        }
        
        if (heartbeatInterval <= 0) {
            throw new IllegalArgumentException("Heartbeat interval must be positive: " + heartbeatInterval);
        }
        
        if (requestTimeout <= 0) {
            throw new IllegalArgumentException("Request timeout must be positive: " + requestTimeout);
        }
    }
    
    @Override
    public String toString() {
        return "NetworkConfig{" +
                "workerThreads=" + workerThreads +
                ", keepAlive=" + keepAlive +
                ", tcpNoDelay=" + tcpNoDelay +
                ", connectTimeout=" + connectTimeout +
                ", receiveBufferSize=" + receiveBufferSize +
                ", sendBufferSize=" + sendBufferSize +
                ", usePooledAllocator=" + usePooledAllocator +
                ", useDirectBuffer=" + useDirectBuffer +
                ", heartbeatEnabled=" + heartbeatEnabled +
                ", heartbeatInterval=" + heartbeatInterval +
                ", requestTimeout=" + requestTimeout +
                '}';
    }
} 