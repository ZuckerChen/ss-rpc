package com.ssrpc.spring.boot;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 测试用的用户服务接口
 * 
 * @author shuang.kou
 * @since 1.0.0
 */
public interface TestUserService {
    
    /**
     * 根据ID获取用户
     */
    TestUser getUserById(Long id);
    
    /**
     * 获取所有用户
     */
    List<TestUser> getAllUsers();
    
    /**
     * 创建用户
     */
    TestUser createUser(TestUser user);
    
    /**
     * 异步获取用户
     */
    CompletableFuture<TestUser> getUserByIdAsync(Long id);
    
    /**
     * 测试无参方法
     */
    String ping();
    
    /**
     * 测试异常处理
     */
    void throwException() throws Exception;
}

/**
 * 测试用的用户实体
 */
class TestUser implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String name;
    private String email;
    private Integer age;
    
    public TestUser() {}
    
    public TestUser(Long id, String name, String email, Integer age) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    
    @Override
    public String toString() {
        return "TestUser{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", age=" + age +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TestUser testUser = (TestUser) obj;
        return id != null ? id.equals(testUser.id) : testUser.id == null;
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
} 