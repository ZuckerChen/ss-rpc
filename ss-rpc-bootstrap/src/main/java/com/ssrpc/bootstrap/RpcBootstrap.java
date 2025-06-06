package com.ssrpc.bootstrap;

import com.ssrpc.config.RpcConfiguration;
import com.ssrpc.core.context.RpcContext;
import com.ssrpc.protocol.ServiceInstance;
import com.ssrpc.transport.api.RpcClient;
import com.ssrpc.transport.api.RpcServer;
import com.ssrpc.transport.config.NetworkConfig;
import com.ssrpc.transport.netty.NettyClient;
import com.ssrpc.transport.netty.NettyServer;
import com.ssrpc.registry.DefaultServiceInvokerRegistry;
import com.ssrpc.registry.ServiceInvokerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RPC框架引导启动器.
 * 
 * 负责整个RPC框架的初始化、启动和关闭流程
 * 提供统一的框架生命周期管理
 * 
 * @author chenzhang
 * @since 1.0.0
 */
public class RpcBootstrap {
    
    private static final Logger log = LoggerFactory.getLogger(RpcBootstrap.class);
    
    /**
     * 全局唯一实例
     */
    private static volatile RpcBootstrap INSTANCE;
    
    /**
     * 框架配置
     */
    private RpcConfiguration configuration;
    
    /**
     * 服务注册中心
     */
    private ServiceInvokerRegistry serviceRegistry;
    
    /**
     * RPC服务端
     */
    private RpcServer rpcServer;
    
    /**
     * RPC客户端
     */
    private RpcClient rpcClient;
    
    /**
     * 本地服务实例信息
     */
    private ServiceInstance localServiceInstance;
    
    /**
     * 启动状态
     */
    private final AtomicBoolean started = new AtomicBoolean(false);
    
    /**
     * 关闭钩子是否已注册
     */
    private final AtomicBoolean shutdownHookRegistered = new AtomicBoolean(false);
    
    /**
     * 私有构造方法，确保单例
     */
    private RpcBootstrap() {
        this.configuration = new RpcConfiguration();
        this.serviceRegistry = new DefaultServiceInvokerRegistry();
    }
    
    /**
     * 获取全局实例
     */
    public static RpcBootstrap getInstance() {
        if (INSTANCE == null) {
            synchronized (RpcBootstrap.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RpcBootstrap();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * 获取默认实例（别名方法）
     */
    public static RpcBootstrap create() {
        return getInstance();
    }
    
    /**
     * 使用自定义配置创建实例
     */
    public static RpcBootstrap create(RpcConfiguration configuration) {
        RpcBootstrap bootstrap = getInstance();
        bootstrap.configuration = configuration;
        return bootstrap;
    }
    
    /**
     * 设置配置
     */
    public RpcBootstrap configuration(RpcConfiguration configuration) {
        if (started.get()) {
            throw new IllegalStateException("Cannot modify configuration after bootstrap started");
        }
        this.configuration = configuration;
        return this;
    }
    
    /**
     * 设置应用名称
     */
    public RpcBootstrap applicationName(String applicationName) {
        this.configuration.setApplicationName(applicationName);
        return this;
    }
    
    /**
     * 设置服务端口
     */
    public RpcBootstrap serverPort(int port) {
        this.configuration.setServerPort(port);
        return this;
    }
    
    /**
     * 设置注册中心地址
     */
    public RpcBootstrap registryAddress(String registryAddress) {
        this.configuration.setRegistryAddress(registryAddress);
        return this;
    }
    
    /**
     * 启用服务端
     */
    public RpcBootstrap enableServer() {
        this.configuration.setServerEnabled(true);
        return this;
    }
    
    /**
     * 禁用服务端
     */
    public RpcBootstrap disableServer() {
        this.configuration.setServerEnabled(false);
        return this;
    }
    
    /**
     * 启用客户端
     */
    public RpcBootstrap enableClient() {
        this.configuration.setClientEnabled(true);
        return this;
    }
    
    /**
     * 禁用客户端
     */
    public RpcBootstrap disableClient() {
        this.configuration.setClientEnabled(false);
        return this;
    }
    
    /**
     * 启用开发模式
     */
    public RpcBootstrap devMode() {
        this.configuration.setDevMode(true);
        return this;
    }
    
    /**
     * 扫描包路径
     */
    public RpcBootstrap scanPackages(String... packages) {
        this.configuration.setScanPackages(packages);
        return this;
    }
    
    /**
     * 启动框架
     */
    public synchronized RpcBootstrap start() {
        if (started.get()) {
            log.warn("RPC framework is already started");
            return this;
        }
        
        try {
            log.info("Starting SS-RPC framework...");
            
            // 1. 验证配置
            validateConfiguration();
            
            // 2. 初始化组件
            initializeComponents();
            
            // 3. 启动服务端
            if (configuration.isServerEnabled()) {
                startServer();
            }
            
            // 4. 启动客户端
            if (configuration.isClientEnabled()) {
                startClient();
            }
            
            // 5. 注册服务实例
            if (configuration.isServerEnabled() && configuration.isAutoRegister()) {
                registerLocalInstance();
            }
            
            // 6. 注册关闭钩子
            registerShutdownHook();
            
            // 7. 标记为已启动
            started.set(true);
            
            log.info("SS-RPC framework started successfully. Config: {}", configuration);
            
        } catch (Exception e) {
            log.error("Failed to start SS-RPC framework", e);
            // 启动失败时清理资源
            cleanup();
            throw new RuntimeException("Failed to start RPC framework", e);
        }
        
        return this;
    }
    
    /**
     * 关闭框架
     */
    public synchronized void shutdown() {
        if (!started.get()) {
            log.warn("RPC framework is not started");
            return;
        }
        
        log.info("Shutting down SS-RPC framework...");
        
        try {
            // 1. 注销服务实例
            if (localServiceInstance != null) {
                unregisterLocalInstance();
            }
            
            // 2. 关闭服务端
            if (rpcServer != null) {
                rpcServer.shutdown();
            }
            
            // 3. 关闭客户端
            if (rpcClient != null) {
                rpcClient.shutdown();
            }
            
            // 4. 清理其他资源
            cleanup();
            
            started.set(false);
            
            log.info("SS-RPC framework shutdown completed");
            
        } catch (Exception e) {
            log.error("Error during framework shutdown", e);
        }
    }
    
    /**
     * 检查框架是否已启动
     */
    public boolean isStarted() {
        return started.get();
    }
    
    /**
     * 获取配置
     */
    public RpcConfiguration getConfiguration() {
        return configuration;
    }
    
    /**
     * 获取服务注册中心
     */
    public ServiceInvokerRegistry getServiceRegistry() {
        return serviceRegistry;
    }
    
    /**
     * 获取RPC服务端
     */
    public RpcServer getRpcServer() {
        return rpcServer;
    }
    
    /**
     * 获取RPC客户端
     */
    public RpcClient getRpcClient() {
        return rpcClient;
    }
    
    /**
     * 获取本地服务实例
     */
    public ServiceInstance getLocalServiceInstance() {
        return localServiceInstance;
    }
    
    // ===== 私有方法 =====
    
    /**
     * 验证配置
     */
    private void validateConfiguration() {
        if (configuration == null) {
            throw new IllegalStateException("RPC configuration is not set");
        }
        
        configuration.validate();
        
        if (!configuration.isServerEnabled() && !configuration.isClientEnabled()) {
            throw new IllegalStateException("At least one of server or client must be enabled");
        }
        
        log.debug("Configuration validation passed");
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        log.debug("Initializing RPC components...");
        
        // 初始化服务注册中心
        if (serviceRegistry == null) {
            serviceRegistry = new DefaultServiceInvokerRegistry();
        }
        
        // 设置全局上下文
        RpcContext.setGlobalAttribute("bootstrap", this);
        RpcContext.setGlobalAttribute("configuration", configuration);
        
        log.debug("RPC components initialized");
    }
    
    /**
     * 启动服务端
     */
    private void startServer() throws Exception {
        log.info("Starting RPC server on port {}...", configuration.getServerPort());
        
        // 创建服务端实例，直接传入配置参数
        rpcServer = new NettyServer(serviceRegistry);
        
        // 启动服务端
        rpcServer.start(configuration.getServerPort());
        
        // 创建本地服务实例信息
        createLocalServiceInstance();
        
        log.info("RPC server started on port {}", rpcServer.getPort());
    }
    
    /**
     * 启动客户端
     */
    private void startClient() throws Exception {
        log.info("Starting RPC client...");
        
        // 创建客户端实例（使用默认配置）
        rpcClient = new NettyClient(new NetworkConfig());
        
        // 启动客户端
        rpcClient.start();
        
        log.info("RPC client started");
    }
    
    /**
     * 创建本地服务实例信息
     */
    private void createLocalServiceInstance() {
        String host = configuration.getServerHost();
        if ("0.0.0.0".equals(host)) {
            // 获取本机IP
            host = getLocalHostAddress();
        }
        
        int port = rpcServer.getPort();
        
        localServiceInstance = new ServiceInstance(
            configuration.getApplicationName(),
            configuration.getApplicationVersion(),
            host,
            port,
            configuration.getServerWeight()
        );
        
        // 添加元数据
        localServiceInstance.addMetadata("startTime", String.valueOf(System.currentTimeMillis()));
        localServiceInstance.addMetadata("version", configuration.getApplicationVersion());
        localServiceInstance.addMetadata("devMode", String.valueOf(configuration.isDevMode()));
        
        log.debug("Created local service instance: {}", localServiceInstance);
    }
    
    /**
     * 注册本地服务实例
     */
    private void registerLocalInstance() {
        if (localServiceInstance == null) {
            log.warn("Local service instance is null, skip registration");
            return;
        }
        
        try {
            // TODO: 实现服务注册逻辑
            log.info("Registered local service instance: {}", localServiceInstance.getInstanceId());
        } catch (Exception e) {
            log.error("Failed to register local service instance", e);
        }
    }
    
    /**
     * 注销本地服务实例
     */
    private void unregisterLocalInstance() {
        if (localServiceInstance == null) {
            return;
        }
        
        try {
            // TODO: 实现服务注销逻辑
            log.info("Unregistered local service instance: {}", localServiceInstance.getInstanceId());
        } catch (Exception e) {
            log.error("Failed to unregister local service instance", e);
        }
    }
    
    /**
     * 注册关闭钩子
     */
    private void registerShutdownHook() {
        if (shutdownHookRegistered.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Received shutdown signal, shutting down RPC framework...");
                shutdown();
            }, "ss-rpc-shutdown-hook"));
            
            log.debug("Shutdown hook registered");
        }
    }
    
    /**
     * 清理资源
     */
    private void cleanup() {
        // 清理上下文
        RpcContext.clearGlobalContext();
        
        log.debug("Resources cleaned up");
    }
    
    /**
     * 获取本机IP地址
     */
    private String getLocalHostAddress() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.warn("Failed to get local host address, using 127.0.0.1", e);
            return "127.0.0.1";
        }
    }
} 