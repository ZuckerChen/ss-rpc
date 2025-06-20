package com.ssrpc.spring.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RPC配置属性
 * 
 * @author shuang.kou
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "ss-rpc")
public class RpcProperties {
    
    /**
     * 是否启用RPC
     */
    private boolean enabled = true;
    
    /**
     * 服务端配置
     */
    private Server server = new Server();
    
    /**
     * 客户端配置
     */
    private Client client = new Client();
    
    /**
     * 注册中心配置
     */
    private Registry registry = new Registry();
    
    /**
     * 序列化配置
     */
    private Serialization serialization = new Serialization();
    
    /**
     * 负载均衡配置
     */
    private LoadBalance loadBalance = new LoadBalance();
    
    /**
     * 服务端配置
     */
    public static class Server {
        /**
         * 服务端口
         */
        private int port = 9999;
        
        /**
         * 服务主机
         */
        private String host = "localhost";
        
        /**
         * 工作线程数
         */
        private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;
        
        /**
         * Boss线程数
         */
        private int bossThreads = 1;
        
        /**
         * 连接超时时间（毫秒）
         */
        private int connectTimeout = 5000;
        
        // getters and setters
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getWorkerThreads() { return workerThreads; }
        public void setWorkerThreads(int workerThreads) { this.workerThreads = workerThreads; }
        
        public int getBossThreads() { return bossThreads; }
        public void setBossThreads(int bossThreads) { this.bossThreads = bossThreads; }
        
        public int getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; }
    }
    
    /**
     * 客户端配置
     */
    public static class Client {
        /**
         * 连接超时时间（毫秒）
         */
        private int connectTimeout = 5000;
        
        /**
         * 请求超时时间（毫秒）
         */
        private int requestTimeout = 10000;
        
        /**
         * 重试次数
         */
        private int retryTimes = 3;
        
        // getters and setters
        public int getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; }
        
        public int getRequestTimeout() { return requestTimeout; }
        public void setRequestTimeout(int requestTimeout) { this.requestTimeout = requestTimeout; }
        
        public int getRetryTimes() { return retryTimes; }
        public void setRetryTimes(int retryTimes) { this.retryTimes = retryTimes; }
    }
    
    /**
     * 注册中心配置
     */
    public static class Registry {
        /**
         * 注册中心类型
         */
        private String type = "memory";
        
        /**
         * 注册中心地址
         */
        private String address = "";
        
        /**
         * 连接超时时间（毫秒）
         */
        private int connectTimeout = 5000;
        
        /**
         * 会话超时时间（毫秒）
         */
        private int sessionTimeout = 30000;
        
        // getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        
        public int getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; }
        
        public int getSessionTimeout() { return sessionTimeout; }
        public void setSessionTimeout(int sessionTimeout) { this.sessionTimeout = sessionTimeout; }
    }
    
    /**
     * 序列化配置
     */
    public static class Serialization {
        /**
         * 序列化类型
         */
        private String type = "json";
        
        // getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
    
    /**
     * 负载均衡配置
     */
    public static class LoadBalance {
        /**
         * 负载均衡算法
         */
        private String algorithm = "round_robin";
        
        // getters and setters
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    }
    
    // 主配置类的getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public Server getServer() { return server; }
    public void setServer(Server server) { this.server = server; }
    
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    
    public Registry getRegistry() { return registry; }
    public void setRegistry(Registry registry) { this.registry = registry; }
    
    public Serialization getSerialization() { return serialization; }
    public void setSerialization(Serialization serialization) { this.serialization = serialization; }
    
    public LoadBalance getLoadBalance() { return loadBalance; }
    public void setLoadBalance(LoadBalance loadBalance) { this.loadBalance = loadBalance; }
} 