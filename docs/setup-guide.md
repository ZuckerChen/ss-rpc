# SS-RPC 开发环境配置指南

## 📋 环境要求

### 必需工具
- **Java**: OpenJDK 8+ (推荐11+)
- **Maven**: 3.6+ 或 **Gradle**: 7.0+
- **IDE**: IntelliJ IDEA 或 VS Code

### 当前环境状态
✅ Java 21.0.5 - 已安装
❌ Maven - 未安装
❌ Gradle - 未安装

## 🚀 第一步：安装构建工具

### 方案一：安装Maven (推荐)

#### Windows环境
1. 下载Maven: https://maven.apache.org/download.cgi
2. 解压到目录，例如：`C:\apache-maven-3.9.6`
3. 添加环境变量：
   ```
   MAVEN_HOME=C:\apache-maven-3.9.6
   PATH=%PATH%;%MAVEN_HOME%\bin
   ```
4. 验证安装：`mvn --version`

#### 使用包管理器安装
```bash
# Windows (Chocolatey)
choco install maven

# Windows (Scoop)
scoop install maven
```

### 方案二：使用IDE内置Maven
如果使用IntelliJ IDEA，可以使用内置的Maven：
1. File → Settings → Build Tools → Maven
2. 选择"Use Maven wrapper" 或使用内置Maven

## 📂 项目结构验证

当前项目结构：
```
ss-rpc/
├── pom.xml                      # 父项目配置
├── ss-rpc-core/                 # 核心模块
├── ss-rpc-protocol/             # 协议模块  
├── ss-rpc-serialization/        # 序列化模块
├── ss-rpc-registry/             # 注册中心模块
├── ss-rpc-loadbalance/          # 负载均衡模块
├── ss-rpc-spring-boot-starter/  # Spring Boot集成
└── ss-rpc-examples/             # 示例项目
```

## 🔧 编译测试

### 使用Maven
```bash
# 清理编译
mvn clean compile

# 运行测试
mvn test

# 打包
mvn package
```

### 使用IDE
1. 导入Maven项目
2. 等待依赖下载完成
3. 右键项目 → Build Module

## 🎯 下一步行动

### 立即行动
1. **安装Maven** - 选择上述任一方案
2. **验证编译** - 执行 `mvn clean compile`
3. **解决依赖问题** - 如有编译错误，检查pom.xml配置
4. **开始开发** - 进入第2周：网络通信模块

### 常见问题
- **网络问题**: 配置Maven国内镜像
- **版本冲突**: 检查Java版本兼容性
- **权限问题**: 使用管理员身份运行

配置完成后，继续参考 [DEVELOPMENT_TASKS.md](../DEVELOPMENT_TASKS.md) 进行开发。 