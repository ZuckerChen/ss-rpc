package com.ssrpc.core.rpc;

import java.io.Serializable;
import java.util.UUID;

/**
 * RPC请求对象.
 * 
 * 包含调用远程服务所需的完整信息
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class RpcRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 请求ID，用于标识唯一请求
     */
    private String requestId;
    
    /**
     * 接口名称
     */
    private String interfaceName;
    
    /**
     * 方法名称
     */
    private String methodName;
    
    /**
     * 参数类型数组
     */
    private Class<?>[] parameterTypes;
    
    /**
     * 参数值数组
     */
    private Object[] parameters;
    
    /**
     * 客户端版本
     */
    private String version;
    
    /**
     * 超时时间（毫秒）
     */
    private long timeout;
    
    /**
     * 是否为心跳请求
     */
    private boolean heartbeat = false;
    
    /**
     * 无参构造器
     */
    public RpcRequest() {
        this.requestId = UUID.randomUUID().toString();
    }
    
    /**
     * 全参构造器
     */
    public RpcRequest(String requestId, String interfaceName, String methodName, 
                     Class<?>[] parameterTypes, Object[] parameters, String version, long timeout) {
        this.requestId = requestId;
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.parameters = parameters;
        this.version = version;
        this.timeout = timeout;
    }
    
    // Getter and Setter methods
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getInterfaceName() {
        return interfaceName;
    }
    
    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
    
    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }
    
    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }
    
    public Object[] getParameters() {
        return parameters;
    }
    
    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public long getTimeout() {
        return timeout;
    }
    
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    public boolean isHeartbeat() {
        return heartbeat;
    }
    
    public void setHeartbeat(boolean heartbeat) {
        this.heartbeat = heartbeat;
    }
    
    /**
     * 设置为心跳请求
     */
    public RpcRequest setAsHeartbeat() {
        this.heartbeat = true;
        this.interfaceName = "HEARTBEAT";
        this.methodName = "ping";
        this.parameterTypes = new Class[0];
        this.parameters = new Object[0];
        this.version = "1.0.0";
        return this;
    }
    
    @Override
    public String toString() {
        return "RpcRequest{" +
                "requestId='" + requestId + '\'' +
                ", interfaceName='" + interfaceName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", version='" + version + '\'' +
                ", timeout=" + timeout +
                ", heartbeat=" + heartbeat +
                '}';
    }
} 