package com.ssrpc.config;

import java.util.Arrays;

/**
 * RPC框架配置类.
 * 
 * 包含RPC框架的各种配置参数
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class RpcConfiguration {
    
    // ===== 应用配置 =====
    
    /**
     * 应用名称
     */
    private String applicationName = "ss-rpc-application";
    
    /**
     * 应用版本
     */
    private String applicationVersion = "1.0.0";
    
    // ===== 服务端配置 =====
    
    /**
     * 是否启用服务端
     */
    private boolean serverEnabled = true;
    
    /**
     * 服务端绑定主机
     */
    private String serverHost = "0.0.0.0";
    
    /**
     * 服务端监听端口
     */
    private int serverPort = 8080;
    
    /**
     * 服务端权重
     */
    private int serverWeight = 100;
    
    // ===== 客户端配置 =====
    
    /**
     * 是否启用客户端
     */
    private boolean clientEnabled = true;
    
    /**
     * 默认请求超时时间（毫秒）
     */
    private long defaultTimeout = 5000L;
    
    /**
     * 连接超时时间（毫秒）
     */
    private long connectTimeout = 3000L;
    
    // ===== 注册中心配置 =====
    
    /**
     * 注册中心地址
     */
    private String registryAddress = "memory://localhost";
    
    /**
     * 是否自动注册
     */
    private boolean autoRegister = true;
    
    /**
     * 注册中心连接超时时间（毫秒）
     */
    private long registryTimeout = 5000L;
    
    // ===== 序列化配置 =====
    
    /**
     * 序列化类型
     */
    private String serializationType = "json";
    
    // ===== 负载均衡配置 =====
    
    /**
     * 负载均衡策略
     */
    private String loadBalanceStrategy = "round_robin";
    
    // ===== 扫描配置 =====
    
    /**
     * 需要扫描的包路径
     */
    private String[] scanPackages = {};
    
    // ===== 其他配置 =====
    
    /**
     * 是否启用开发模式
     */
    private boolean devMode = false;
    
    /**
     * 验证配置参数
     */
    public void validate() {
        if (applicationName == null || applicationName.trim().isEmpty()) {
            throw new IllegalArgumentException("Application name cannot be null or empty");
        }
        
        if (serverEnabled && (serverPort <= 0 || serverPort > 65535)) {
            throw new IllegalArgumentException("Server port must be between 1 and 65535");
        }
        
        if (defaultTimeout <= 0) {
            throw new IllegalArgumentException("Default timeout must be positive");
        }
        
        if (connectTimeout <= 0) {
            throw new IllegalArgumentException("Connect timeout must be positive");
        }
        
        if (registryAddress == null || registryAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("Registry address cannot be null or empty");
        }
        
        if (serializationType == null || serializationType.trim().isEmpty()) {
            throw new IllegalArgumentException("Serialization type cannot be null or empty");
        }
        
        if (loadBalanceStrategy == null || loadBalanceStrategy.trim().isEmpty()) {
            throw new IllegalArgumentException("Load balance strategy cannot be null or empty");
        }
    }
    
    // Getter and Setter methods
    public String getApplicationName() {
        return applicationName;
    }
    
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    
    public String getApplicationVersion() {
        return applicationVersion;
    }
    
    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }
    
    public boolean isServerEnabled() {
        return serverEnabled;
    }
    
    public void setServerEnabled(boolean serverEnabled) {
        this.serverEnabled = serverEnabled;
    }
    
    public String getServerHost() {
        return serverHost;
    }
    
    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }
    
    public int getServerPort() {
        return serverPort;
    }
    
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }
    
    public int getServerWeight() {
        return serverWeight;
    }
    
    public void setServerWeight(int serverWeight) {
        this.serverWeight = serverWeight;
    }
    
    public boolean isClientEnabled() {
        return clientEnabled;
    }
    
    public void setClientEnabled(boolean clientEnabled) {
        this.clientEnabled = clientEnabled;
    }
    
    public long getDefaultTimeout() {
        return defaultTimeout;
    }
    
    public void setDefaultTimeout(long defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }
    
    public long getConnectTimeout() {
        return connectTimeout;
    }
    
    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
    
    public String getRegistryAddress() {
        return registryAddress;
    }
    
    public void setRegistryAddress(String registryAddress) {
        this.registryAddress = registryAddress;
    }
    
    public boolean isAutoRegister() {
        return autoRegister;
    }
    
    public void setAutoRegister(boolean autoRegister) {
        this.autoRegister = autoRegister;
    }
    
    public long getRegistryTimeout() {
        return registryTimeout;
    }
    
    public void setRegistryTimeout(long registryTimeout) {
        this.registryTimeout = registryTimeout;
    }
    
    public String getSerializationType() {
        return serializationType;
    }
    
    public void setSerializationType(String serializationType) {
        this.serializationType = serializationType;
    }
    
    public String getLoadBalanceStrategy() {
        return loadBalanceStrategy;
    }
    
    public void setLoadBalanceStrategy(String loadBalanceStrategy) {
        this.loadBalanceStrategy = loadBalanceStrategy;
    }
    
    public String[] getScanPackages() {
        return scanPackages;
    }
    
    public void setScanPackages(String[] scanPackages) {
        this.scanPackages = scanPackages;
    }
    
    public boolean isDevMode() {
        return devMode;
    }
    
    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }
    
    @Override
    public String toString() {
        return "RpcConfiguration{" +
                "applicationName='" + applicationName + '\'' +
                ", applicationVersion='" + applicationVersion + '\'' +
                ", serverEnabled=" + serverEnabled +
                ", serverHost='" + serverHost + '\'' +
                ", serverPort=" + serverPort +
                ", clientEnabled=" + clientEnabled +
                ", registryAddress='" + registryAddress + '\'' +
                ", serializationType='" + serializationType + '\'' +
                ", loadBalanceStrategy='" + loadBalanceStrategy + '\'' +
                ", scanPackages=" + Arrays.toString(scanPackages) +
                ", devMode=" + devMode +
                '}';
    }
} 