package com.ssrpc.core.rpc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * RPC响应对象.
 * 
 * 封装了RPC调用的响应信息，包括结果、异常、状态等
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RpcResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 对应请求的唯一标识
     */
    private String requestId;
    
    /**
     * 响应状态码
     */
    private int statusCode = ResponseStatus.SUCCESS.getCode();
    
    /**
     * 响应状态信息
     */
    private String statusMessage = ResponseStatus.SUCCESS.getMessage();
    
    /**
     * 调用结果（成功时包含方法返回值）
     */
    private Object result;
    
    /**
     * 异常信息（失败时包含异常）
     */
    private Throwable exception;
    
    /**
     * 响应创建时间（毫秒时间戳）
     */
    private long createTime;
    
    /**
     * 服务端处理耗时（毫秒）
     */
    private long processTime;
    
    /**
     * 响应附件，用于传递额外的上下文信息
     */
    private Map<String, String> attachments;
    
    /**
     * 服务端地址信息
     */
    private String serverAddress;
    
    /**
     * 响应类型：0-普通响应，1-心跳响应
     */
    private byte responseType = 0;
    
    /**
     * 构造方法：创建成功响应
     */
    public RpcResponse(String requestId, Object result) {
        this.requestId = requestId;
        this.result = result;
        this.statusCode = ResponseStatus.SUCCESS.getCode();
        this.statusMessage = ResponseStatus.SUCCESS.getMessage();
        this.createTime = System.currentTimeMillis();
        this.attachments = new HashMap<>();
    }
    
    /**
     * 构造方法：创建失败响应
     */
    public RpcResponse(String requestId, Throwable exception) {
        this.requestId = requestId;
        this.exception = exception;
        this.statusCode = ResponseStatus.ERROR.getCode();
        this.statusMessage = exception.getMessage();
        this.createTime = System.currentTimeMillis();
        this.attachments = new HashMap<>();
    }
    
    /**
     * 创建成功响应的静态方法
     */
    public static RpcResponse success(String requestId, Object result) {
        return new RpcResponse(requestId, result);
    }
    
    /**
     * 创建失败响应的静态方法
     */
    public static RpcResponse fail(String requestId, Throwable exception) {
        return new RpcResponse(requestId, exception);
    }
    
    /**
     * 创建失败响应的静态方法（带错误消息）
     */
    public static RpcResponse fail(String requestId, String errorMessage) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(requestId);
        response.setStatusCode(ResponseStatus.ERROR.getCode());
        response.setStatusMessage(errorMessage);
        response.setCreateTime(System.currentTimeMillis());
        response.setAttachments(new HashMap<>());
        return response;
    }
    
    /**
     * 创建心跳响应
     */
    public static RpcResponse heartbeat(String requestId) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(requestId);
        response.setResponseType((byte) 1);
        response.setStatusCode(ResponseStatus.SUCCESS.getCode());
        response.setStatusMessage("pong");
        response.setResult("pong");
        response.setCreateTime(System.currentTimeMillis());
        return response;
    }
    
    /**
     * 添加附件信息
     */
    public RpcResponse addAttachment(String key, String value) {
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
     * 检查是否成功响应
     */
    public boolean isSuccess() {
        return this.statusCode == ResponseStatus.SUCCESS.getCode() && this.exception == null;
    }
    
    /**
     * 检查是否为心跳响应
     */
    public boolean isHeartbeat() {
        return this.responseType == 1;
    }
    
    /**
     * 设置处理时间
     */
    public RpcResponse setProcessTime(long startTime) {
        this.processTime = System.currentTimeMillis() - startTime;
        return this;
    }
    
    /**
     * 设置响应为成功状态
     */
    public RpcResponse setSuccess(boolean success) {
        if (success) {
            this.statusCode = ResponseStatus.SUCCESS.getCode();
            this.statusMessage = ResponseStatus.SUCCESS.getMessage();
            this.exception = null;
        } else {
            this.statusCode = ResponseStatus.ERROR.getCode();
            this.statusMessage = ResponseStatus.ERROR.getMessage();
        }
        return this;
    }
    
    /**
     * 设置错误消息
     */
    public RpcResponse setErrorMessage(String errorMessage) {
        this.statusCode = ResponseStatus.ERROR.getCode();
        this.statusMessage = errorMessage;
        return this;
    }
    
    /**
     * 响应状态枚举
     */
    public enum ResponseStatus {
        SUCCESS(200, "Success"),
        ERROR(500, "Internal Server Error"),
        TIMEOUT(408, "Request Timeout"),
        SERVICE_NOT_FOUND(404, "Service Not Found"),
        METHOD_NOT_FOUND(405, "Method Not Found"),
        SERIALIZATION_ERROR(406, "Serialization Error"),
        NETWORK_ERROR(503, "Network Error");
        
        private final int code;
        private final String message;
        
        ResponseStatus(int code, String message) {
            this.code = code;
            this.message = message;
        }
        
        public int getCode() {
            return code;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    @Override
    public String toString() {
        return "RpcResponse{" +
                "requestId='" + requestId + '\'' +
                ", statusCode=" + statusCode +
                ", statusMessage='" + statusMessage + '\'' +
                ", hasResult=" + (result != null) +
                ", hasException=" + (exception != null) +
                ", processTime=" + processTime +
                ", responseType=" + responseType +
                '}';
    }
} 