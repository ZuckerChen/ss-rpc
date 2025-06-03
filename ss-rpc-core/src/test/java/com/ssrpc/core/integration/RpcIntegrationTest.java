package com.ssrpc.core.integration;

import com.ssrpc.core.config.NetworkConfig;
import com.ssrpc.core.invoker.ReflectionServiceInvoker;
import com.ssrpc.core.network.netty.NettyClient;
import com.ssrpc.core.network.netty.NettyServer;
import com.ssrpc.core.proxy.JdkProxyFactory;
import com.ssrpc.core.registry.DefaultServiceInvokerRegistry;
import com.ssrpc.core.rpc.DefaultRpcInvoker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RPC集成测试类.
 * 
 * 测试完整的RPC调用链路：客户端代理 -> 网络传输 -> 服务端调用 -> 响应返回
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class RpcIntegrationTest {
    
    private NettyServer server;
    private NettyClient client;
    private NetworkConfig config;
    private DefaultServiceInvokerRegistry serviceRegistry;
    
    // 测试服务接口
    public interface HelloService {
        String sayHello(String name);
        int add(int a, int b);
        CompletableFuture<String> asyncHello(String name);
    }
    
    // 测试服务实现
    public static class HelloServiceImpl implements HelloService {
        @Override
        public String sayHello(String name) {
            return "Hello, " + name + "!";
        }
        
        @Override
        public int add(int a, int b) {
            return a + b;
        }
        
        @Override
        public CompletableFuture<String> asyncHello(String name) {
            return CompletableFuture.completedFuture("Async Hello, " + name + "!");
        }
    }
    
    @BeforeEach
    void setUp() throws Exception {
        // 创建配置
        config = NetworkConfig.defaultConfig();
        config.setServerPort(18088);
        config.setConnectTimeout(3000);
        config.setRequestTimeout(5000);
        config.setHeartbeatEnabled(false); // 测试时关闭心跳
        
        // 创建服务注册表
        serviceRegistry = new DefaultServiceInvokerRegistry();
        
        // 注册测试服务
        HelloService helloService = new HelloServiceImpl();
        ReflectionServiceInvoker serviceInvoker = new ReflectionServiceInvoker(
            HelloService.class, 
            helloService, 
            HelloService.class.getName(), 
            "1.0.0"
        );
        serviceRegistry.registerInvoker(HelloService.class.getName(), "1.0.0", serviceInvoker);
        
        // 创建并启动服务端
        server = new NettyServer(config, serviceRegistry);
        server.start(config.getServerPort());
        
        // 等待服务端启动
        Thread.sleep(500);
        
        // 创建并启动客户端
        client = new NettyClient(config);
        client.start();
        
        // 等待客户端启动
        Thread.sleep(200);
    }
    
    @AfterEach
    void tearDown() {
        if (client != null) {
            client.shutdown();
        }
        
        if (server != null) {
            server.shutdown();
        }
    }
    
    @Test
    void testCompleteRpcCall() throws Exception {
        // 创建RPC调用器
        String serverAddress = "localhost:" + config.getServerPort();
        DefaultRpcInvoker rpcInvoker = new DefaultRpcInvoker(
            client, serverAddress, config.getRequestTimeout());
        
        // 创建代理工厂
        JdkProxyFactory proxyFactory = new JdkProxyFactory();
        
        // 创建服务代理
        HelloService helloProxy = proxyFactory.createProxy(HelloService.class, rpcInvoker);
        
        // 测试同步调用
        String result = helloProxy.sayHello("SS-RPC");
        assertEquals("Hello, SS-RPC!", result);
        
        // 测试带参数的方法
        int sum = helloProxy.add(10, 20);
        assertEquals(30, sum);
    }
    
    @Test
    void testAsyncRpcCall() throws Exception {
        // 创建RPC调用器
        String serverAddress = "localhost:" + config.getServerPort();
        DefaultRpcInvoker rpcInvoker = new DefaultRpcInvoker(
            client, serverAddress, config.getRequestTimeout());
        
        // 创建代理工厂
        JdkProxyFactory proxyFactory = new JdkProxyFactory();
        
        // 创建服务代理
        HelloService helloProxy = proxyFactory.createProxy(HelloService.class, rpcInvoker);
        
        // 测试异步调用
        CompletableFuture<String> future = helloProxy.asyncHello("Async");
        
        // 等待异步结果
        String result = future.get(3, TimeUnit.SECONDS);
        assertEquals("Async Hello, Async!", result);
    }
    
    @Test
    void testMultipleConcurrentCalls() throws Exception {
        // 创建RPC调用器
        String serverAddress = "localhost:" + config.getServerPort();
        DefaultRpcInvoker rpcInvoker = new DefaultRpcInvoker(
            client, serverAddress, config.getRequestTimeout());
        
        // 创建代理工厂
        JdkProxyFactory proxyFactory = new JdkProxyFactory();
        
        // 创建服务代理
        HelloService helloProxy = proxyFactory.createProxy(HelloService.class, rpcInvoker);
        
        // 并发调用测试
        int concurrentCount = 10;
        CompletableFuture<String>[] futures = new CompletableFuture[concurrentCount];
        
        for (int i = 0; i < concurrentCount; i++) {
            final int index = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    return helloProxy.sayHello("User" + index);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        
        // 等待所有调用完成
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        allFutures.get(10, TimeUnit.SECONDS);
        
        // 验证结果
        for (int i = 0; i < concurrentCount; i++) {
            String result = futures[i].get();
            assertEquals("Hello, User" + i + "!", result);
        }
    }
    
    @Test
    void testServiceNotFound() throws Exception {
        // 创建RPC调用器
        String serverAddress = "localhost:" + config.getServerPort();
        DefaultRpcInvoker rpcInvoker = new DefaultRpcInvoker(
            client, serverAddress, config.getRequestTimeout());
        
        // 创建代理工厂
        JdkProxyFactory proxyFactory = new JdkProxyFactory();
        
        // 创建不存在服务的代理

        
        NonExistentService proxy = proxyFactory.createProxy(NonExistentService.class, rpcInvoker);
        
        // 调用不存在的服务应该抛出异常
        assertThrows(Exception.class, () -> {
            proxy.test();
        });
    }
    
    @Test
    void testProxyFactorySupport() {
        JdkProxyFactory proxyFactory = new JdkProxyFactory();
        
        // 接口应该被支持
        assertTrue(proxyFactory.isSupported(HelloService.class));
        
        // 普通类不应该被支持
        assertFalse(proxyFactory.isSupported(String.class));
        assertFalse(proxyFactory.isSupported(HelloServiceImpl.class));
    }
    
    @Test
    void testServiceRegistry() {
        // 测试服务注册表功能
        assertTrue(serviceRegistry.containsService(HelloService.class.getName(), "1.0.0"));
        assertEquals(1, serviceRegistry.getServiceCount());
        
        // 注册另一个版本的服务
        HelloService anotherService = new HelloServiceImpl();
        ReflectionServiceInvoker anotherInvoker = new ReflectionServiceInvoker(
            HelloService.class, 
            anotherService, 
            HelloService.class.getName(), 
            "2.0.0"
        );
        serviceRegistry.registerInvoker(HelloService.class.getName(), "2.0.0", anotherInvoker);
        
        assertEquals(2, serviceRegistry.getServiceCount());
        assertTrue(serviceRegistry.containsService(HelloService.class.getName(), "2.0.0"));
    }
} 