package com.ssrpc.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 服务实例信息.
 * 
 * 表示一个具体的服务实例，包含地址、端口、权重等信息
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class ServiceInstance implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 服务名称
     */
    private String serviceName;
    
    /**
     * 服务版本
     */
    private String version;
    
    /**
     * 服务主机地址
     */
    private String host;
    
    /**
     * 服务端口
     */
    private int port;
    
    /**
     * 实例权重，用于负载均衡
     */
    private int weight;
    
    /**
     * 实例元数据
     */
    private Map<String, String> metadata;
    
    /**
     * 实例唯一标识
     */
    private String instanceId;
    
    /**
     * 实例是否健康
     */
    private boolean healthy;
    
    /**
     * 无参构造器
     */
    public ServiceInstance() {
        this.metadata = new HashMap<>();
        this.healthy = true;
        this.weight = 1;
    }
    
    /**
     * 全参构造器
     */
    public ServiceInstance(String serviceName, String version, String host, int port, int weight) {
        this();
        this.serviceName = serviceName;
        this.version = version;
        this.host = host;
        this.port = port;
        this.weight = weight;
        this.instanceId = generateInstanceId();
    }
    
    /**
     * 生成实例ID
     */
    private String generateInstanceId() {
        return serviceName + ":" + version + "@" + host + ":" + port;
    }
    
    /**
     * 添加元数据
     */
    public void addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
    
    /**
     * 获取元数据
     */
    public String getMetadata(String key) {
        return this.metadata != null ? this.metadata.get(key) : null;
    }
    
    // Getter and Setter methods
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public int getWeight() {
        return weight;
    }
    
    public void setWeight(int weight) {
        this.weight = weight;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    public String getInstanceId() {
        return instanceId;
    }
    
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
    
    public boolean isHealthy() {
        return healthy;
    }
    
    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }
    
    /**
     * 获取服务地址
     */
    public String getAddress() {
        return host + ":" + port;
    }
    
    /**
     * 获取服务键（serviceName:version）
     */
    public String getServiceKey() {
        return serviceName + ":" + version;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceInstance that = (ServiceInstance) o;
        return port == that.port &&
                Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(version, that.version) &&
                Objects.equals(host, that.host);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(serviceName, version, host, port);
    }
    
    @Override
    public String toString() {
        return "ServiceInstance{" +
                "serviceName='" + serviceName + '\'' +
                ", version='" + version + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", weight=" + weight +
                ", healthy=" + healthy +
                ", instanceId='" + instanceId + '\'' +
                '}';
    }
} 