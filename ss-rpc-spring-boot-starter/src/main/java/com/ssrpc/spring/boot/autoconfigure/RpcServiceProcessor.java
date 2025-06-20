package com.ssrpc.spring.boot.autoconfigure;

import com.ssrpc.core.annotation.RpcService;
import com.ssrpc.protocol.ServiceInstance;
import com.ssrpc.registry.api.ServiceRegistry;
import com.ssrpc.registry.exception.RegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * RPC服务处理器
 * 扫描@RpcService注解的Bean并注册到注册中心
 * 
 * @author shuang.kou
 * @since 1.0.0
 */
public class RpcServiceProcessor implements BeanPostProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(RpcServiceProcessor.class);
    
    private final ServiceRegistry serviceRegistry;
    private final RpcProperties rpcProperties;
    
    public RpcServiceProcessor(ServiceRegistry serviceRegistry, RpcProperties rpcProperties) {
        this.serviceRegistry = serviceRegistry;
        this.rpcProperties = rpcProperties;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        RpcService rpcService = AnnotationUtils.findAnnotation(beanClass, RpcService.class);
        
        if (rpcService != null) {
            try {
                registerService(bean, rpcService);
                log.info("Successfully registered RPC service: {} with bean name: {}", 
                        beanClass.getName(), beanName);
            } catch (Exception e) {
                log.error("Failed to register RPC service: {} with bean name: {}", 
                        beanClass.getName(), beanName, e);
                throw new RuntimeException("Failed to register RPC service", e);
            }
        }
        
        return bean;
    }
    
    /**
     * 注册服务到注册中心
     */
    private void registerService(Object serviceBean, RpcService rpcService) throws RegistryException {
        // 获取服务接口
        Class<?> serviceInterface = getServiceInterface(serviceBean, rpcService);
        
        // 创建服务实例
        ServiceInstance serviceInstance = createServiceInstance(serviceInterface, rpcService);
        
        // 注册到注册中心
        serviceRegistry.register(serviceInstance);
        
        log.info("Registered service instance: {} for interface: {}", 
                serviceInstance.getInstanceId(), serviceInterface.getName());
    }
    
    /**
     * 获取服务接口
     */
    private Class<?> getServiceInterface(Object serviceBean, RpcService rpcService) {
        // 查找实现的接口
        Class<?>[] interfaces = serviceBean.getClass().getInterfaces();
        if (interfaces.length == 0) {
            throw new IllegalArgumentException("Service bean must implement at least one interface");
        }
        
        if (interfaces.length > 1) {
            throw new IllegalArgumentException("Service bean implements multiple interfaces, cannot determine which one to use");
        }
        
        return interfaces[0];
    }
    
    /**
     * 创建服务实例
     */
    private ServiceInstance createServiceInstance(Class<?> serviceInterface, RpcService rpcService) {
        ServiceInstance instance = new ServiceInstance();
        
        // 设置基本信息
        String serviceName = rpcService.value().isEmpty() ? serviceInterface.getName() : rpcService.value();
        instance.setServiceName(serviceName);
        instance.setVersion(rpcService.version());
        instance.setInstanceId(generateInstanceId());
        
        // 设置网络信息
        instance.setHost(getLocalHost());
        instance.setPort(rpcProperties.getServer().getPort());
        
        // 设置元数据
        instance.getMetadata().put("protocol", "ss-rpc");
        instance.getMetadata().put("serialization", rpcProperties.getSerialization().getType());
        instance.getMetadata().put("loadBalance", rpcProperties.getLoadBalance().getAlgorithm());
        instance.getMetadata().put("weight", String.valueOf(rpcService.weight()));
        
        // 设置健康状态
        instance.setHealthy(true);
        
        return instance;
    }
    
    /**
     * 生成实例ID
     */
    private String generateInstanceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 获取本地主机地址
     */
    private String getLocalHost() {
        try {
            String configuredHost = rpcProperties.getServer().getHost();
            if (!"localhost".equals(configuredHost) && !"0.0.0.0".equals(configuredHost)) {
                return configuredHost;
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("Failed to get local host address, using localhost", e);
            return "localhost";
        }
    }
} 