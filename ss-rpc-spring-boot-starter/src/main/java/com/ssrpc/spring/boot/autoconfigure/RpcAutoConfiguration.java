package com.ssrpc.spring.boot.autoconfigure;

import com.ssrpc.loadbalance.LoadBalancerFactory;
import com.ssrpc.registry.api.ServiceDiscovery;
import com.ssrpc.registry.api.ServiceRegistry;
import com.ssrpc.registry.factory.RegistryFactory;
import com.ssrpc.registry.factory.RegistryManager;
import com.ssrpc.core.spi.ExtensionLoader;
import com.ssrpc.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * RPC自动配置类
 * 
 * @author shuang.kou
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(RpcProperties.class)
@ConditionalOnProperty(prefix = "ss-rpc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RpcAutoConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(RpcAutoConfiguration.class);
    
    private final RpcProperties rpcProperties;
    private RegistryManager registryManager;
    
    public RpcAutoConfiguration(RpcProperties rpcProperties) {
        this.rpcProperties = rpcProperties;
    }
    
    @PostConstruct
    public void init() {
        log.info("Initializing SS-RPC with properties: {}", rpcProperties);
        
        // 初始化序列化器
        initSerializer();
        
        // 初始化负载均衡器
        initLoadBalancer();
        
        // 初始化注册中心
        initRegistry();
        
        log.info("SS-RPC initialization completed");
    }
    
    @PreDestroy
    public void destroy() {
        if (registryManager != null && registryManager.isStarted()) {
            try {
                registryManager.stop();
                log.info("SS-RPC registry manager stopped");
            } catch (Exception e) {
                log.error("Error stopping registry manager", e);
            }
        }
    }
    
    /**
     * 初始化序列化器
     */
    private void initSerializer() {
        String serializationType = rpcProperties.getSerialization().getType();
        log.info("Initializing serializer with type: {}", serializationType);
        
        // 预热序列化器
        try {
            ExtensionLoader<Serializer> loader = ExtensionLoader.getExtensionLoader(Serializer.class);
            Serializer serializer = loader.getExtension(serializationType);
            log.info("Serializer initialized successfully: {}", serializationType);
        } catch (Exception e) {
            log.warn("Failed to initialize serializer: {}, falling back to default", serializationType, e);
        }
    }
    
    /**
     * 初始化负载均衡器
     */
    private void initLoadBalancer() {
        String algorithm = rpcProperties.getLoadBalance().getAlgorithm();
        log.info("Initializing load balancer with algorithm: {}", algorithm);
        
        try {
            LoadBalancerFactory.getLoadBalancer(algorithm);
            log.info("Load balancer initialized successfully: {}", algorithm);
        } catch (Exception e) {
            log.warn("Failed to initialize load balancer: {}, falling back to default", algorithm, e);
        }
    }
    
    /**
     * 初始化注册中心
     */
    private void initRegistry() {
        String registryType = rpcProperties.getRegistry().getType();
        log.info("Initializing registry with type: {}", registryType);
        
        try {
            registryManager = new RegistryManager(registryType);
            registryManager.start();
            log.info("Registry manager started successfully: {}", registryType);
        } catch (Exception e) {
            log.error("Failed to initialize registry: {}", registryType, e);
            throw new RuntimeException("Failed to initialize registry", e);
        }
    }
    
    /**
     * 服务注册中心Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public ServiceRegistry serviceRegistry() {
        if (registryManager == null) {
            throw new IllegalStateException("Registry manager not initialized");
        }
        return registryManager.getServiceRegistry();
    }
    
    /**
     * 服务发现Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public ServiceDiscovery serviceDiscovery() {
        if (registryManager == null) {
            throw new IllegalStateException("Registry manager not initialized");
        }
        return registryManager.getServiceDiscovery();
    }
    
    /**
     * 注册中心管理器Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public RegistryManager registryManager() {
        if (registryManager == null) {
            throw new IllegalStateException("Registry manager not initialized");
        }
        return registryManager;
    }
    
    /**
     * RPC服务处理器Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public RpcServiceProcessor rpcServiceProcessor(ServiceRegistry serviceRegistry) {
        return new RpcServiceProcessor(serviceRegistry, rpcProperties);
    }
    
    /**
     * RPC引用处理器Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public RpcReferenceProcessor rpcReferenceProcessor(ServiceDiscovery serviceDiscovery) {
        return new RpcReferenceProcessor(serviceDiscovery, rpcProperties);
    }
} 