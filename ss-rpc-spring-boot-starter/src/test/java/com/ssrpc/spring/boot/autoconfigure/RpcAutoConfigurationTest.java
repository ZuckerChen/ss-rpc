package com.ssrpc.spring.boot.autoconfigure;

import com.ssrpc.registry.api.ServiceDiscovery;
import com.ssrpc.registry.api.ServiceRegistry;
import com.ssrpc.registry.factory.RegistryManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RPC自动配置测试
 * 
 * @author shuang.kou
 * @since 1.0.0
 */
class RpcAutoConfigurationTest {
    
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RpcAutoConfiguration.class));
    
    @Test
    void testAutoConfigurationEnabled() {
        this.contextRunner
                .withPropertyValues("ss-rpc.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(RpcProperties.class);
                    assertThat(context).hasSingleBean(ServiceRegistry.class);
                    assertThat(context).hasSingleBean(ServiceDiscovery.class);
                    assertThat(context).hasSingleBean(RegistryManager.class);
                    assertThat(context).hasSingleBean(RpcServiceProcessor.class);
                    assertThat(context).hasSingleBean(RpcReferenceProcessor.class);
                });
    }
    
    @Test
    void testAutoConfigurationDisabled() {
        this.contextRunner
                .withPropertyValues("ss-rpc.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ServiceRegistry.class);
                    assertThat(context).doesNotHaveBean(ServiceDiscovery.class);
                    assertThat(context).doesNotHaveBean(RegistryManager.class);
                    assertThat(context).doesNotHaveBean(RpcServiceProcessor.class);
                    assertThat(context).doesNotHaveBean(RpcReferenceProcessor.class);
                });
    }
    
    @Test
    void testCustomProperties() {
        this.contextRunner
                .withPropertyValues(
                        "ss-rpc.enabled=true",
                        "ss-rpc.server.port=8888",
                        "ss-rpc.server.host=0.0.0.0",
                        "ss-rpc.registry.type=memory",
                        "ss-rpc.serialization.type=json",
                        "ss-rpc.load-balance.algorithm=random"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(RpcProperties.class);
                    
                    RpcProperties properties = context.getBean(RpcProperties.class);
                    assertThat(properties.getServer().getPort()).isEqualTo(8888);
                    assertThat(properties.getServer().getHost()).isEqualTo("0.0.0.0");
                    assertThat(properties.getRegistry().getType()).isEqualTo("memory");
                    assertThat(properties.getSerialization().getType()).isEqualTo("json");
                    assertThat(properties.getLoadBalance().getAlgorithm()).isEqualTo("random");
                });
    }
    
    @Test
    void testDefaultProperties() {
        this.contextRunner
                .withPropertyValues("ss-rpc.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(RpcProperties.class);
                    
                    RpcProperties properties = context.getBean(RpcProperties.class);
                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.getServer().getPort()).isEqualTo(9999);
                    assertThat(properties.getServer().getHost()).isEqualTo("localhost");
                    assertThat(properties.getRegistry().getType()).isEqualTo("memory");
                    assertThat(properties.getSerialization().getType()).isEqualTo("json");
                    assertThat(properties.getLoadBalance().getAlgorithm()).isEqualTo("round_robin");
                });
    }
} 