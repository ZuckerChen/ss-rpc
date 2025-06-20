package com.ssrpc.spring.boot.autoconfigure;

import com.ssrpc.core.annotation.RpcReference;
import com.ssrpc.registry.api.ServiceDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

/**
 * RPC引用处理器
 * 扫描@RpcReference注解的字段并注入代理对象
 * 
 * @author shuang.kou
 * @since 1.0.0
 */
public class RpcReferenceProcessor implements BeanPostProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(RpcReferenceProcessor.class);
    
    private final ServiceDiscovery serviceDiscovery;
    private final RpcProperties rpcProperties;
    
    public RpcReferenceProcessor(ServiceDiscovery serviceDiscovery, RpcProperties rpcProperties) {
        this.serviceDiscovery = serviceDiscovery;
        this.rpcProperties = rpcProperties;
    }
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        Field[] fields = beanClass.getDeclaredFields();
        
        for (Field field : fields) {
            RpcReference rpcReference = AnnotationUtils.findAnnotation(field, RpcReference.class);
            if (rpcReference != null) {
                try {
                    injectRpcReference(bean, field, rpcReference);
                    log.info("Successfully injected RPC reference for field: {} in bean: {}", 
                            field.getName(), beanName);
                } catch (Exception e) {
                    log.error("Failed to inject RPC reference for field: {} in bean: {}", 
                            field.getName(), beanName, e);
                    throw new RuntimeException("Failed to inject RPC reference", e);
                }
            }
        }
        
        return bean;
    }
    
    /**
     * 注入RPC引用
     */
    private void injectRpcReference(Object bean, Field field, RpcReference rpcReference) 
            throws IllegalAccessException {
        
        Class<?> fieldType = field.getType();
        
        // 创建代理对象
        Object proxy = createProxy(fieldType, rpcReference);
        
        // 注入代理对象
        field.setAccessible(true);
        field.set(bean, proxy);
        
        log.info("Created and injected RPC proxy for interface: {} with version: {}", 
                fieldType.getName(), rpcReference.version());
    }
    
    /**
     * 创建RPC代理对象
     */
    private Object createProxy(Class<?> interfaceClass, RpcReference rpcReference) {
        return Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class[]{interfaceClass},
                new RpcInvocationHandler(interfaceClass, rpcReference, serviceDiscovery, rpcProperties)
        );
    }
} 