# SS-RPC 贡献指南

感谢您对SS-RPC项目的贡献！本指南将帮助您了解如何参与项目开发。

## 开发流程

### 1. Fork & Clone

```bash
# Fork项目到您的GitHub账号
# 然后克隆到本地
git clone https://github.com/YOUR_USERNAME/ss-rpc.git
cd ss-rpc

# 添加上游仓库
git remote add upstream https://github.com/ORIGINAL_OWNER/ss-rpc.git
```

### 2. 分支管理

#### 主要分支
- `main`: 稳定发布版本，只接受PR合并
- `develop`: 开发主分支，日常开发基于此分支

#### 功能分支命名规范
```bash
feature/模块-功能描述        # 新功能
bugfix/模块-问题描述        # Bug修复
hotfix/紧急修复描述         # 紧急修复
docs/文档更新描述           # 文档更新
refactor/重构模块描述       # 代码重构
```

#### 示例分支名
```bash
feature/core-annotations
feature/network-communication
feature/serialization-json
bugfix/registry-connection-leak
hotfix/memory-overflow
docs/api-documentation
refactor/connection-pool
```

### 3. 开发工作流

```bash
# 1. 创建功能分支
git checkout develop
git pull upstream develop
git checkout -b feature/your-feature-name

# 2. 开发和提交
# ... 进行开发 ...
git add .
git commit -m "feat(core): add RPC service discovery mechanism"

# 3. 推送分支
git push origin feature/your-feature-name

# 4. 创建Pull Request
# 在GitHub上创建PR到develop分支
```

## 提交规范

### 提交信息格式

使用约定式提交格式：

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Type类型定义

| Type | 描述 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat(core): add service registry` |
| `fix` | Bug修复 | `fix(serialization): resolve null pointer` |
| `docs` | 文档更新 | `docs(readme): update quick start guide` |
| `style` | 代码格式 | `style(core): format code with checkstyle` |
| `refactor` | 代码重构 | `refactor(network): optimize connection pool` |
| `test` | 测试相关 | `test(core): add unit tests for annotations` |
| `chore` | 构建/工具 | `chore(deps): upgrade netty to 4.1.75` |
| `perf` | 性能优化 | `perf(serialization): improve JSON performance` |

### Scope范围定义

| Scope | 描述 |
|-------|------|
| `core` | ss-rpc-core核心模块 |
| `protocol` | ss-rpc-protocol协议模块 |
| `serialization` | ss-rpc-serialization序列化模块 |
| `registry` | ss-rpc-registry注册中心模块 |
| `loadbalance` | ss-rpc-loadbalance负载均衡模块 |
| `spring` | ss-rpc-spring-boot-starter模块 |
| `examples` | ss-rpc-examples示例模块 |
| `build` | 构建系统 |
| `ci` | 持续集成 |

### 提交信息示例

```bash
# 好的提交信息
feat(core): implement @RpcService annotation processor

Add annotation processor to scan and register RPC services
- Support service name and version configuration
- Integrate with Spring application context
- Add validation for service interface

Closes #15

---

fix(registry): resolve Zookeeper connection timeout

- Increase default connection timeout to 30s
- Add connection retry mechanism
- Improve error logging

Fixes #23

---

docs(api): add serialization configuration examples

- Document JSON serialization setup
- Add Protobuf integration guide
- Include performance comparison

---

# 不好的提交信息
fix: bug fix        # 太模糊
update code        # 没有描述具体改动
WIP               # 不应该提交到主分支
```

### 提交粒度指导

#### ✅ 好的提交粒度
- 实现一个完整的小功能
- 修复一个特定的bug
- 添加一组相关的测试
- 更新相关文档

#### ❌ 避免的提交粒度
- 半完成的功能
- 混合多个不相关的改动
- 破坏代码编译的提交
- 过于庞大的单次提交

## 代码审查

### Pull Request要求

1. **标题清晰**：简洁描述PR目的
2. **描述详细**：说明改动内容、原因、影响
3. **测试完整**：包含相应的单元测试
4. **文档更新**：如有API变更，需更新文档
5. **无冲突**：解决与目标分支的冲突

### PR模板示例

```markdown
## 改动类型
- [ ] 新功能
- [ ] Bug修复
- [ ] 文档更新
- [ ] 代码重构
- [ ] 性能优化

## 改动描述
简要描述此PR的改动内容和目的。

## 详细改动
- 改动点1
- 改动点2
- 改动点3

## 测试
- [ ] 单元测试通过
- [ ] 集成测试通过
- [ ] 手动测试完成

## 检查清单
- [ ] 代码符合项目规范
- [ ] 包含必要的测试
- [ ] 文档已更新
- [ ] 无breaking changes（或已在描述中说明）

## 相关Issue
Closes #issue_number
```

## 代码规范

### 代码质量要求

1. **遵循CheckStyle规范**：项目根目录的`checkstyle.xml`
2. **测试覆盖率**：新代码测试覆盖率不低于80%
3. **JavaDoc文档**：公共API必须有完整文档
4. **异常处理**：合理的异常处理和日志记录

### 性能要求

1. **内存使用**：避免内存泄漏，合理使用对象池
2. **响应时间**：核心功能响应时间不超过10ms
3. **并发安全**：多线程环境下的线程安全

## 发布流程

### 版本号规范

使用语义版本控制（SemVer）：`MAJOR.MINOR.PATCH`

- `MAJOR`：不兼容的API改动
- `MINOR`：向后兼容的功能新增
- `PATCH`：向后兼容的问题修复

### 发布分支

```bash
# 创建发布分支
git checkout develop
git checkout -b release/v1.0.0

# 准备发布
# - 更新版本号
# - 更新CHANGELOG
# - 完成测试

# 合并到main
git checkout main
git merge release/v1.0.0
git tag v1.0.0

# 合并回develop
git checkout develop
git merge release/v1.0.0
```

## 社区参与

### 问题报告

使用GitHub Issues报告问题，包含：
- 问题描述
- 重现步骤
- 期望行为
- 环境信息
- 相关日志

### 功能建议

1. 先在Issues中讨论
2. 获得社区反馈
3. 设计技术方案
4. 实现和测试

### 沟通渠道

- GitHub Issues：问题报告和功能讨论
- GitHub Discussions：技术交流和答疑
- README联系方式：项目维护者联系方式

## 开发环境

### 环境要求

- JDK 17+
- Maven 3.8+
- Git 2.20+

### 构建和测试

```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 代码质量检查
mvn checkstyle:check

# 完整构建
mvn clean package
```

感谢您的贡献！如有疑问，请随时在Issues中提问。 