package com.ssrpc.core.rpc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * RPC请求对象.
 * 
 * 封装了RPC调用所需的所有信息，包括服务名、方法名、参数、版本等
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RpcRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 请求唯一标识，用于异步调用时匹配请求和响应
     */
    private String requestId;
    
    /**
     * 服务接口名称（通常是接口的全限定名）
     */
    private String serviceName;
    
    /**
     * 调用的方法名称
     */
    private String methodName;
    
    /**
     * 方法参数类型数组
     */
    private Class<?>[] parameterTypes;
    
    /**
     * 方法参数值数组
     */
    private Object[] parameters;
    
    /**
     * 服务版本，用于服务版本控制
     */
    private String version = "1.0.0";
    
    /**
     * 请求超时时间（毫秒）
     */
    private long timeout = 5000L;
    
    /**
     * 请求创建时间（毫秒时间戳）
     */
    private long createTime;
    
    /**
     * 请求附件，用于传递额外的上下文信息
     */
    private Map<String, String> attachments;
    
    /**
     * 客户端地址信息
     */
    private String clientAddress;
    
    /**
     * 是否为单向调用（不需要响应）
     */
    private boolean oneWay = false;
    
    /**
     * 请求类型：0-普通请求，1-心跳请求
     */
    private byte requestType = 0;
    
    /**
     * 构造方法：创建普通RPC请求
     */
    public RpcRequest(String serviceName, String methodName, 
                     Class<?>[] parameterTypes, Object[] parameters) {
        this.requestId = generateRequestId();
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.parameters = parameters;
        this.createTime = System.currentTimeMillis();
        this.attachments = new HashMap<>();
    }
    
    /**
     * 构造方法：创建带版本的RPC请求
     */
    public RpcRequest(String serviceName, String methodName, 
                     Class<?>[] parameterTypes, Object[] parameters, String version) {
        this(serviceName, methodName, parameterTypes, parameters);
        this.version = version;
    }
    
    /**
     * 生成唯一请求ID
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 添加附件信息
     */
    public RpcRequest addAttachment(String key, String value) {
        if (this.attachments == null) {
            this.attachments = new HashMap<>();
        }
        this.attachments.put(key, value);
        return this;
    }
    
    /**
     * 获取附件信息
     */
    public String getAttachment(String key) {
        return this.attachments != null ? this.attachments.get(key) : null;
    }
    
    /**
     * 检查是否为心跳请求
     */
    public boolean isHeartbeat() {
        return this.requestType == 1;
    }
    
    /**
     * 设置为心跳请求
     */
    public RpcRequest setAsHeartbeat() {
        this.requestType = 1;
        this.serviceName = "HeartbeatService";
        this.methodName = "ping";
        if (this.requestId == null || this.requestId.isEmpty()) {
            this.requestId = generateRequestId();
        }
        this.createTime = System.currentTimeMillis();
        return this;
    }
    
    /**
     * 检查请求是否超时
     */
    public boolean isTimeout() {
        return System.currentTimeMillis() - createTime > timeout;
    }
    
    /**
     * 获取服务的唯一标识（服务名 + 版本）
     */
    public String getServiceKey() {
        return serviceName + ":" + version;
    }
    
    @Override
    public String toString() {
        return "RpcRequest{" +
                "requestId='" + requestId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", version='" + version + '\'' +
                ", timeout=" + timeout +
                ", oneWay=" + oneWay +
                ", requestType=" + requestType +
                '}';
    }
} 