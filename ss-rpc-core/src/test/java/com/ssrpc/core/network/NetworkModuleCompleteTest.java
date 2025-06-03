package com.ssrpc.core.network;

import com.ssrpc.core.config.NetworkConfig;
import com.ssrpc.core.invoker.ServiceInvoker;
import com.ssrpc.core.network.netty.NettyClient;
import com.ssrpc.core.network.netty.NettyServer;
import com.ssrpc.core.registry.ServiceInvokerRegistry;
import com.ssrpc.core.rpc.RpcRequest;
import com.ssrpc.core.rpc.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Network模块完整功能测试
 * 
 * 验证SS-RPC网络通信模块的所有核心功能：
 * 1. 基础连接和通信
 * 2. 心跳检测机制
 * 3. 异步调用处理
 * 4. 连接池管理
 * 5. 异常处理和恢复
 * 6. 性能和并发测试
 * 
 * @author chenzhang
 * @since 1.0.0
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NetworkModuleCompleteTest {
    
    private static final int SERVER_PORT = 18080;
    private static final String SERVER_ADDRESS = "localhost:" + SERVER_PORT;
    
    private NettyServer server;
    private NettyClient client;
    private NetworkConfig serverConfig;
    private NetworkConfig clientConfig;
    private MockServiceRegistry serviceRegistry;
    
    @BeforeEach
    void setUp() throws Exception {
        log.info("=== 开始设置测试环境 ===");
        
        // 配置服务端
        serverConfig = NetworkConfig.defaultConfig();
        serverConfig.setServerPort(SERVER_PORT);
        serverConfig.setHeartbeatEnabled(true);
        serverConfig.setHeartbeatInterval(10);      // 心跳间隔10秒
        serverConfig.setHeartbeatTimeout(5);        // 心跳超时5秒 (< 10秒)
        
        // 配置客户端
        clientConfig = NetworkConfig.defaultConfig();
        clientConfig.setConnectTimeout(3000);
        clientConfig.setRequestTimeout(5000);
        clientConfig.setHeartbeatEnabled(true);
        clientConfig.setHeartbeatInterval(8);       // 心跳间隔8秒
        clientConfig.setHeartbeatTimeout(4);        // 心跳超时4秒 (< 8秒)
        
        // 创建服务注册表并注册测试服务
        serviceRegistry = new MockServiceRegistry();
        serviceRegistry.registerInvoker("EchoService", "1.0.0", new EchoServiceInvoker());
        serviceRegistry.registerInvoker("CalculatorService", "1.0.0", new CalculatorServiceInvoker());
        serviceRegistry.registerInvoker("SlowService", "1.0.0", new SlowServiceInvoker());
        
        // 启动服务端
        server = new NettyServer(serverConfig, serviceRegistry);
        server.start(SERVER_PORT);
        
        // 等待服务端完全启动
        Thread.sleep(1000);
        assertTrue(server.isStarted(), "服务端应该启动成功");
        
        // 启动客户端
        client = new NettyClient(clientConfig);
        client.start();
        assertTrue(client.isStarted(), "客户端应该启动成功");
        
        log.info("=== 测试环境设置完成 ===");
    }
    
    @AfterEach
    void tearDown() {
        log.info("=== 开始清理测试环境 ===");
        
        if (client != null) {
            client.shutdown();
        }
        
        if (server != null) {
            server.shutdown();
        }
        
        log.info("=== 测试环境清理完成 ===");
    }
    
    @Test
    @Order(1)
    @DisplayName("测试基础连接和通信")
    void testBasicConnection() throws Exception {
        log.info("=== 测试基础连接和通信 ===");
        
        // 创建RPC请求
        RpcRequest request = new RpcRequest(
            "EchoService", 
            "echo", 
            new Class[]{String.class}, 
            new Object[]{"Hello Network Module"}
        );
        
        // 发送请求
        CompletableFuture<RpcResponse> future = client.sendRequest(SERVER_ADDRESS, request);
        assertNotNull(future, "Future不应该为null");
        
        // 等待响应
        RpcResponse response = future.get(5, TimeUnit.SECONDS);
        
        // 验证响应
        assertNotNull(response, "响应不应该为null");
        assertEquals(request.getRequestId(), response.getRequestId(), "请求ID应该匹配");
        assertTrue(response.isSuccess(), "响应应该成功");
        assertEquals("Echo: Hello Network Module", response.getResult(), "响应结果应该正确");
        
        log.info("基础连接测试通过");
    }
    
    @Test
    @Order(2)
    @DisplayName("测试心跳机制")
    void testHeartbeat() throws Exception {
        log.info("=== 测试心跳机制 ===");
        
        // 创建心跳请求
        RpcRequest heartbeat = new RpcRequest().setAsHeartbeat();
        assertTrue(heartbeat.isHeartbeat(), "应该是心跳请求");
        
        // 发送心跳
        CompletableFuture<RpcResponse> future = client.sendRequest(SERVER_ADDRESS, heartbeat);
        RpcResponse response = future.get(3, TimeUnit.SECONDS);
        
        // 验证心跳响应
        assertNotNull(response, "心跳响应不应该为null");
        assertEquals(heartbeat.getRequestId(), response.getRequestId(), "请求ID应该匹配");
        assertTrue(response.isSuccess(), "心跳响应应该成功");
        assertEquals("pong", response.getResult(), "心跳响应应该是pong");
        
        log.info("心跳机制测试通过");
    }
    
    @Test
    @Order(3)
    @DisplayName("测试并发请求处理")
    void testConcurrentRequests() throws Exception {
        log.info("=== 测试并发请求处理 ===");
        
        int concurrentCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentCount);
        CountDownLatch latch = new CountDownLatch(concurrentCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // 提交并发请求
        for (int i = 0; i < concurrentCount; i++) {
            final int requestIndex = i;
            executor.submit(() -> {
                try {
                    RpcRequest request = new RpcRequest(
                        "CalculatorService", 
                        "add", 
                        new Class[]{int.class, int.class}, 
                        new Object[]{requestIndex, requestIndex * 2}
                    );
                    
                    CompletableFuture<RpcResponse> future = client.sendRequest(SERVER_ADDRESS, request);
                    RpcResponse response = future.get(10, TimeUnit.SECONDS);
                    
                    if (response.isSuccess()) {
                        int result = (Integer) response.getResult();
                        int expected = requestIndex + (requestIndex * 2);
                        if (result == expected) {
                            successCount.incrementAndGet();
                            log.debug("并发请求{}成功: {} + {} = {}", requestIndex, requestIndex, requestIndex * 2, result);
                        } else {
                            log.error("并发请求{}结果错误: 期望{}, 实际{}", requestIndex, expected, result);
                            failureCount.incrementAndGet();
                        }
                    } else {
                        log.error("并发请求{}失败: {}", requestIndex, response.getStatusMessage());
                        failureCount.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    log.error("并发请求{}异常", requestIndex, e);
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有请求完成
        assertTrue(latch.await(30, TimeUnit.SECONDS), "所有并发请求应该在30秒内完成");
        
        // 验证结果
        assertEquals(concurrentCount, successCount.get(), "所有并发请求都应该成功");
        assertEquals(0, failureCount.get(), "不应该有失败的请求");
        
        executor.shutdown();
        log.info("并发请求测试通过: 成功{}, 失败{}", successCount.get(), failureCount.get());
    }
    
    @Test
    @Order(4)
    @DisplayName("测试服务不存在的情况")
    void testServiceNotFound() throws Exception {
        log.info("=== 测试服务不存在的情况 ===");
        
        RpcRequest request = new RpcRequest(
            "NonExistentService", 
            "unknownMethod", 
            new Class[]{}, 
            new Object[]{}
        );
        
        CompletableFuture<RpcResponse> future = client.sendRequest(SERVER_ADDRESS, request);
        RpcResponse response = future.get(5, TimeUnit.SECONDS);
        
        // 验证错误响应
        assertNotNull(response, "响应不应该为null");
        assertFalse(response.isSuccess(), "应该返回失败响应");
        assertTrue(response.getStatusMessage().contains("服务未找到") || 
                  response.getStatusMessage().contains("Service not found") ||
                  response.getStatusCode() == RpcResponse.ResponseStatus.SERVICE_NOT_FOUND.getCode(),
                  "应该包含服务未找到的错误信息");
        
        log.info("服务不存在测试通过");
    }
    
    @Test
    @Order(5)
    @DisplayName("测试请求超时")
    void testRequestTimeout() throws Exception {
        log.info("=== 测试请求超时 ===");
        
        // 创建一个会导致超时的请求
        RpcRequest request = new RpcRequest(
            "SlowService", 
            "slowMethod", 
            new Class[]{long.class}, 
            new Object[]{8000L} // 8秒延迟，超过5秒超时时间
        );
        
        CompletableFuture<RpcResponse> future = client.sendRequest(SERVER_ADDRESS, request);
        
        // 验证超时异常 - CompletableFuture.get()会将TimeoutException包装在ExecutionException中
        ExecutionException executionException = assertThrows(ExecutionException.class, () -> {
            future.get(6, TimeUnit.SECONDS);
        }, "应该抛出ExecutionException包装的超时异常");
        
        // 验证根本原因是TimeoutException
        assertTrue(executionException.getCause() instanceof TimeoutException, 
                  "ExecutionException的cause应该是TimeoutException");
        assertTrue(executionException.getCause().getMessage().contains("Request timeout"), 
                  "异常消息应该包含超时信息");
        
        log.info("请求超时测试通过");
    }
    
    @Test
    @Order(6)
    @DisplayName("测试连接状态检查")
    void testConnectionStatus() throws Exception {
        log.info("=== 测试连接状态检查 ===");
        
        // 初始状态
        assertFalse(client.isConnected(SERVER_ADDRESS), "初始状态应该未连接");
        
        // 发送一个请求建立连接
        RpcRequest request = new RpcRequest().setAsHeartbeat();
        CompletableFuture<RpcResponse> future = client.sendRequest(SERVER_ADDRESS, request);
        future.get(3, TimeUnit.SECONDS);
        
        // 验证连接状态
        assertTrue(client.isConnected(SERVER_ADDRESS), "发送请求后应该已连接");
        
        log.info("连接状态检查测试通过");
    }
    
    @Test
    @Order(7)
    @DisplayName("测试同步调用")
    void testSynchronousCall() throws Exception {
        log.info("=== 测试同步调用 ===");
        
        RpcRequest request = new RpcRequest(
            "EchoService", 
            "echo", 
            new Class[]{String.class}, 
            new Object[]{"Sync Call Test"}
        );
        
        // 同步调用
        RpcResponse response = client.sendRequestSync(SERVER_ADDRESS, request);
        
        // 验证响应
        assertNotNull(response, "同步响应不应该为null");
        assertTrue(response.isSuccess(), "同步调用应该成功");
        assertEquals("Echo: Sync Call Test", response.getResult(), "同步调用结果应该正确");
        
        log.info("同步调用测试通过");
    }
    
    /**
     * 模拟服务注册表
     */
    private static class MockServiceRegistry implements ServiceInvokerRegistry {
        private final ConcurrentHashMap<String, ServiceInvoker> invokers = new ConcurrentHashMap<>();
        
        @Override
        public void registerInvoker(String serviceName, String version, ServiceInvoker invoker) {
            String key = serviceName + ":" + version;
            invokers.put(key, invoker);
            log.debug("注册服务: {}", key);
        }
        
        @Override
        public ServiceInvoker getInvoker(String serviceName, String version) {
            String key = serviceName + ":" + version;
            return invokers.get(key);
        }
        
        @Override
        public ServiceInvoker removeInvoker(String serviceName, String version) {
            String key = serviceName + ":" + version;
            return invokers.remove(key);
        }
        
        @Override
        public boolean containsService(String serviceName, String version) {
            String key = serviceName + ":" + version;
            return invokers.containsKey(key);
        }
        
        @Override
        public int getServiceCount() {
            return invokers.size();
        }
        
        @Override
        public void clear() {
            invokers.clear();
        }
    }
    
    /**
     * Echo服务实现
     */
    private static class EchoServiceInvoker implements ServiceInvoker {
        @Override
        public Object invoke(String methodName, Class<?>[] parameterTypes, Object[] parameters) {
            if ("echo".equals(methodName) && parameters.length == 1) {
                return "Echo: " + parameters[0];
            }
            throw new IllegalArgumentException("Unknown method: " + methodName);
        }
        
        @Override
        public Class<?> getServiceType() {
            return String.class;
        }
        
        @Override
        public Object getServiceInstance() {
            return this;
        }
        
        @Override
        public String getServiceName() {
            return "EchoService";
        }
        
        @Override
        public String getVersion() {
            return "1.0.0";
        }
    }
    
    /**
     * 计算器服务实现
     */
    private static class CalculatorServiceInvoker implements ServiceInvoker {
        @Override
        public Object invoke(String methodName, Class<?>[] parameterTypes, Object[] parameters) {
            switch (methodName) {
                case "add":
                    return (Integer) parameters[0] + (Integer) parameters[1];
                case "subtract":
                    return (Integer) parameters[0] - (Integer) parameters[1];
                case "multiply":
                    return (Integer) parameters[0] * (Integer) parameters[1];
                case "divide":
                    return (Integer) parameters[0] / (Integer) parameters[1];
                default:
                    throw new IllegalArgumentException("Unknown method: " + methodName);
            }
        }
        
        @Override
        public Class<?> getServiceType() {
            return Integer.class;
        }
        
        @Override
        public Object getServiceInstance() {
            return this;
        }
        
        @Override
        public String getServiceName() {
            return "CalculatorService";
        }
        
        @Override
        public String getVersion() {
            return "1.0.0";
        }
    }
    
    /**
     * 慢服务实现（用于测试超时）
     */
    private static class SlowServiceInvoker implements ServiceInvoker {
        @Override
        public Object invoke(String methodName, Class<?>[] parameterTypes, Object[] parameters) {
            if ("slowMethod".equals(methodName)) {
                try {
                    long delay = (Long) parameters[0];
                    Thread.sleep(delay);
                    return "Slow method completed after " + delay + "ms";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted", e);
                }
            }
            throw new IllegalArgumentException("Unknown method: " + methodName);
        }
        
        @Override
        public Class<?> getServiceType() {
            return String.class;
        }
        
        @Override
        public Object getServiceInstance() {
            return this;
        }
        
        @Override
        public String getServiceName() {
            return "SlowService";
        }
        
        @Override
        public String getVersion() {
            return "1.0.0";
        }
    }
} 