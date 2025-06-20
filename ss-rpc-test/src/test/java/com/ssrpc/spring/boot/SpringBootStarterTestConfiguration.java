package com.ssrpc.spring.boot;

import com.ssrpc.core.annotation.EnableRpc;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot Starter测试配置类
 * 
 * @author shuang.kou
 * @since 1.0.0
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EnableRpc
@ComponentScan(basePackages = "com.ssrpc.spring.boot")
public class SpringBootStarterTestConfiguration {
    
    @Bean
    public TestUserServiceImpl testUserService() {
        return new TestUserServiceImpl();
    }
    
    @Bean
    public TestUserController testUserController() {
        return new TestUserController();
    }
} 