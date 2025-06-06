package com.ssrpc.core.rpc;

import java.io.Serializable;

/**
 * RPC响应对象.
 * 
 * 包含远程调用的结果信息
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class RpcResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 响应状态码
     */
    public static final byte SUCCESS = 0;
    public static final byte ERROR = 1;
    
    /**
     * 请求ID，与RpcRequest对应
     */
    private String requestId;
    
    /**
     * 响应状态：0-成功，1-失败
     */
    private byte status = SUCCESS;
    
    /**
     * 返回结果
     */
    private Object result;
    
    /**
     * 异常信息
     */
    private String errorMessage;
    
    /**
     * 异常堆栈
     */
    private String stackTrace;
    
    /**
     * 服务端处理时间（毫秒）
     */
    private long processTime;
    
    /**
     * 无参构造器
     */
    public RpcResponse() {
    }
    
    /**
     * 全参构造器
     */
    public RpcResponse(String requestId, byte status, Object result, String errorMessage, 
                      String stackTrace, long processTime) {
        this.requestId = requestId;
        this.status = status;
        this.result = result;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
        this.processTime = processTime;
    }
    
    /**
     * 创建成功响应
     */
    public static RpcResponse success(String requestId, Object result) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(requestId);
        response.setStatus(SUCCESS);
        response.setResult(result);
        return response;
    }
    
    /**
     * 创建失败响应
     */
    public static RpcResponse error(String requestId, String errorMessage) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(requestId);
        response.setStatus(ERROR);
        response.setErrorMessage(errorMessage);
        return response;
    }
    
    /**
     * 创建异常响应
     */
    public static RpcResponse error(String requestId, Throwable throwable) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(requestId);
        response.setStatus(ERROR);
        response.setErrorMessage(throwable.getMessage());
        response.setStackTrace(getStackTrace(throwable));
        return response;
    }
    
    private static String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    // Getter and Setter methods
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public byte getStatus() {
        return status;
    }
    
    public void setStatus(byte status) {
        this.status = status;
    }
    
    public Object getResult() {
        return result;
    }
    
    public void setResult(Object result) {
        this.result = result;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getStackTrace() {
        return stackTrace;
    }
    
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
    
    public long getProcessTime() {
        return processTime;
    }
    
    public void setProcessTime(long processTime) {
        this.processTime = processTime;
    }
    
    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return status == SUCCESS;
    }
    
    /**
     * 是否失败
     */
    public boolean isError() {
        return status == ERROR;
    }
    
    @Override
    public String toString() {
        return "RpcResponse{" +
                "requestId='" + requestId + '\'' +
                ", status=" + status +
                ", result=" + result +
                ", errorMessage='" + errorMessage + '\'' +
                ", processTime=" + processTime +
                '}';
    }
} 