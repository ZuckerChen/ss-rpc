# SS-RPC Git工作流指南

本文档详细说明了SS-RPC项目的Git工作流程，包括日常开发、代码审查和发布流程。

## 分支策略概览

```
main                    # 生产分支，稳定版本
├── develop            # 开发主分支
│   ├── feature/*      # 功能开发分支
│   ├── bugfix/*       # Bug修复分支
│   └── refactor/*     # 重构分支
├── release/*          # 发布准备分支
└── hotfix/*           # 紧急修复分支
```

## 分支说明

### 主要分支

#### `main` 分支
- **目的**：生产就绪的稳定代码
- **保护级别**：最高保护，只能通过PR合并
- **合并来源**：`release/*` 和 `hotfix/*` 分支
- **自动化**：合并后自动创建Git tag和发布

#### `develop` 分支
- **目的**：集成开发中的所有功能
- **保护级别**：高保护，需要PR和代码审查
- **合并来源**：`feature/*`、`bugfix/*`、`refactor/*` 分支
- **发布时机**：当功能完备且稳定时，创建 `release/*` 分支

### 临时分支

#### `feature/*` 分支
- **命名规范**：`feature/模块-功能描述`
- **示例**：`feature/core-annotations`、`feature/network-nio`
- **生命周期**：从创建到合并到develop
- **删除时机**：合并后立即删除

#### `bugfix/*` 分支
- **命名规范**：`bugfix/模块-问题描述`
- **示例**：`bugfix/registry-connection-leak`
- **生命周期**：修复完成并合并后删除

#### `release/*` 分支
- **命名规范**：`release/v版本号`
- **示例**：`release/v1.0.0`、`release/v1.1.0`
- **目的**：发布前的最后准备和bug修复
- **删除时机**：发布完成后保留

#### `hotfix/*` 分支
- **命名规范**：`hotfix/紧急修复描述`
- **示例**：`hotfix/critical-memory-leak`
- **来源**：从main分支创建
- **合并**：同时合并到main和develop

## 日常开发工作流

### 1. 开始新功能开发

```bash
# 1. 更新本地develop分支
git checkout develop
git pull upstream develop

# 2. 创建功能分支
git checkout -b feature/core-annotations

# 3. 开发和提交
# ... 进行开发工作 ...
git add .
git commit -m "feat(core): add @RpcService annotation

Add basic RPC service annotation with name and version attributes
- Support service name configuration
- Support version specification
- Add basic validation logic

Relates to #15"

# 4. 推送到远程仓库
git push origin feature/core-annotations
```

### 2. 持续开发和提交

```bash
# 小功能完成时提交
git add .
git commit -m "feat(core): add @RpcReference annotation processor"

# 修复小问题时提交
git commit -m "fix(core): resolve annotation scanning null pointer"

# 重构代码时提交
git commit -m "refactor(core): extract annotation validation logic"

# 添加测试时提交
git commit -m "test(core): add unit tests for RPC annotations"
```

### 3. 保持分支同步

```bash
# 定期同步develop分支的最新变更
git checkout develop
git pull upstream develop
git checkout feature/core-annotations
git rebase develop

# 如果有冲突，解决冲突后继续
git add .
git rebase --continue
```

### 4. 创建Pull Request

```bash
# 推送最新代码
git push origin feature/core-annotations

# 在GitHub上创建PR
# 标题: feat(core): implement RPC annotations
# 描述: 按照PR模板填写详细信息
```

## 代码审查流程

### 审查者职责

1. **代码质量检查**
   - 代码风格是否符合CheckStyle规范
   - 是否有明显的bug或逻辑错误
   - 异常处理是否合理
   - 是否有潜在的性能问题

2. **架构一致性检查**
   - 是否符合项目整体架构
   - 是否遵循设计原则
   - 模块间依赖是否合理

3. **测试完整性检查**
   - 是否包含必要的单元测试
   - 测试覆盖率是否达标
   - 测试用例是否有效

### 审查流程

```bash
# 1. 审查者拉取PR分支
git fetch origin
git checkout feature/core-annotations

# 2. 本地构建和测试
mvn clean compile
mvn test
mvn checkstyle:check

# 3. 代码审查
# - 在GitHub上逐行审查代码
# - 提出改进建议
# - 标记需要修改的地方

# 4. 审查结果
# - Approve: 代码质量良好，可以合并
# - Request Changes: 需要修改后重新审查
# - Comment: 提出建议但不阻止合并
```

## 发布流程

### 版本发布流程

```bash
# 1. 创建发布分支
git checkout develop
git pull upstream develop
git checkout -b release/v1.0.0

# 2. 更新版本号
# - 更新pom.xml中的version
# - 更新README.md中的版本信息
# - 更新CHANGELOG.md

# 3. 发布前测试
mvn clean package
# 运行完整的测试套件
# 进行集成测试
# 性能测试

# 4. 修复发布相关的bug（如果有）
git commit -m "fix(release): resolve integration test failure"

# 5. 合并到main分支
git checkout main
git merge release/v1.0.0
git tag v1.0.0
git push upstream main --tags

# 6. 合并回develop分支
git checkout develop
git merge release/v1.0.0
git push upstream develop

# 7. 删除发布分支（可选）
git branch -d release/v1.0.0
git push origin --delete release/v1.0.0
```

### 紧急修复流程

```bash
# 1. 从main创建hotfix分支
git checkout main
git pull upstream main
git checkout -b hotfix/critical-memory-leak

# 2. 修复问题
git commit -m "fix(core): resolve critical memory leak in connection pool

- Fix connection objects not being properly released
- Add connection pool monitoring
- Update connection cleanup logic

Fixes #234"

# 3. 测试修复
mvn clean test

# 4. 合并到main
git checkout main
git merge hotfix/critical-memory-leak
git tag v1.0.1
git push upstream main --tags

# 5. 合并到develop
git checkout develop
git merge hotfix/critical-memory-leak
git push upstream develop

# 6. 删除hotfix分支
git branch -d hotfix/critical-memory-leak
```

## 提交消息规范

### 基本格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 示例提交消息

```bash
# 新功能
feat(serialization): add Protobuf serialization support

Implement Protobuf serialization for better performance
- Add ProtobufSerializer class
- Support schema evolution
- Add configuration options
- Include performance benchmarks

Performance improvement: 40% faster than JSON
Breaking change: requires protobuf-java dependency

Closes #56

# Bug修复
fix(registry): resolve Zookeeper session timeout

- Increase default session timeout to 60s
- Add session reconnection logic
- Improve error handling and logging

Fixes #78

# 文档更新
docs(api): update serialization configuration guide

- Add Protobuf setup instructions
- Include performance comparison table
- Fix typos in code examples

# 重构
refactor(core): extract service discovery interface

- Create ServiceDiscovery interface
- Move Zookeeper implementation to separate class
- Improve testability and extensibility

No breaking changes

# 测试
test(loadbalance): add comprehensive load balancing tests

- Test round-robin algorithm
- Test weighted random algorithm
- Add performance benchmarks
- Include edge case scenarios

Coverage increased from 75% to 92%
```

## Git配置建议

### 全局配置

```bash
# 设置用户信息
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# 设置默认编辑器
git config --global core.editor "code --wait"

# 设置换行符处理
git config --global core.autocrlf input  # Linux/Mac
git config --global core.autocrlf true   # Windows

# 设置默认分支名
git config --global init.defaultBranch main

# 启用颜色输出
git config --global color.ui auto

# 设置合并工具
git config --global merge.tool vscode
```

### 项目配置

```bash
# 设置上游仓库
git remote add upstream https://github.com/ORIGINAL_OWNER/ss-rpc.git

# 配置push默认行为
git config push.default simple

# 设置自动rebase
git config pull.rebase true
```

## 常用Git命令

### 日常操作

```bash
# 查看状态
git status
git log --oneline -10

# 分支操作
git branch -a                    # 查看所有分支
git branch -d feature/old-branch # 删除本地分支
git push origin --delete feature/old-branch # 删除远程分支

# 同步操作
git fetch upstream              # 获取上游更新
git pull upstream develop      # 拉取上游develop分支

# 提交操作
git add -A                      # 暂存所有变更
git commit --amend              # 修改最后一次提交
git commit --no-verify          # 跳过pre-commit hook（不推荐）

# 重置操作
git reset --soft HEAD~1         # 撤销最后一次提交，保留变更
git reset --hard HEAD~1         # 撤销最后一次提交，丢弃变更
```

### 高级操作

```bash
# 交互式rebase
git rebase -i HEAD~3            # 重新整理最近3次提交

# 挑选提交
git cherry-pick commit-hash     # 将指定提交应用到当前分支

# 暂存工作
git stash                       # 暂存当前工作
git stash pop                   # 恢复暂存的工作

# 查看差异
git diff                        # 查看工作目录变更
git diff --staged               # 查看暂存区变更
git diff main..develop          # 比较分支差异
```

## 最佳实践总结

### ✅ 推荐做法

1. **小而频繁的提交**：每完成一个小功能就提交
2. **清晰的提交消息**：遵循约定式提交规范
3. **定期同步**：经常从develop分支rebase最新代码
4. **完整的测试**：每个PR都包含相应的测试
5. **代码审查**：所有代码都要经过审查才能合并

### ❌ 避免的做法

1. **大而粗的提交**：不要把多个功能混在一次提交中
2. **直接推送到主分支**：永远通过PR合并代码
3. **跳过测试**：不要提交未经测试的代码
4. **忽略冲突**：及时解决合并冲突
5. **不清理分支**：及时删除已合并的功能分支

通过遵循这些工作流程和最佳实践，我们可以确保SS-RPC项目的代码质量和开发效率，同时为社区贡献者提供清晰的协作指南。 