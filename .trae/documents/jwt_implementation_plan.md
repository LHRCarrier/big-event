# JWT 令牌生成与认证系统解决方案

## 1. 问题分析

### 1.1 当前系统架构
- 使用 `spring-boot-starter-oauth2-authorization-server` 作为 OAuth2 授权服务器框架
- 手动配置了 `JwtEncoder` 和 `JwtDecoder` 组件
- 存在 JWT 令牌生成过程中的冲突问题

### 1.2 根本原因
- 手动配置的 `JwtEncoder` 与框架内置编码器产生冲突
- 未使用框架提供的标准组件（如 `OAuth2TokenGenerator`）进行令牌生成
- JWT 令牌生成与解析的配置不规范

## 2. 解决方案

### 2.1 核心策略
- 移除手动配置的 `JwtEncoder` 和 `JwtDecoder`
- 使用框架提供的标准组件进行 JWT 令牌生成与解析
- 配置 OAuth2 授权服务器的标准流程
- 确保 JWT 令牌的正确生成与验证

### 2.2 具体步骤

#### 步骤 1: 调整依赖配置
- 确保 `spring-boot-starter-oauth2-authorization-server` 依赖正确配置
- 移除不必要的依赖

#### 步骤 2: 配置 OAuth2 授权服务器
- 创建 `AuthorizationServerConfig` 配置类
- 配置客户端信息
- 配置 JWT 令牌生成器

#### 步骤 3: 实现 `OAuth2TokenGenerator`
- 实现自定义的 `OAuth2TokenGenerator`
- 配置 JWT 令牌的自定义声明

#### 步骤 4: 配置安全过滤器链
- 调整 `SecurityConfiguration` 类
- 配置认证和授权规则

#### 步骤 5: 修改登录和认证流程
- 更新 `UserController` 中的登录方法
- 使用标准的 OAuth2 流程进行令牌生成

#### 步骤 6: 配置 JWT 解码器
- 配置 JWT 解码器用于令牌验证
- 确保令牌解析的正确性

## 3. 代码实现计划

### 3.1 配置文件修改
- `application.yml`: 调整 JWT 相关配置

### 3.2 新增配置类
- `AuthorizationServerConfig.java`: 配置 OAuth2 授权服务器

### 3.3 修改现有类
- `JwtConfiguration.java`: 移除手动配置的 JWT 编码器和解码器
- `SecurityConfiguration.java`: 调整安全过滤器链配置
- `UserController.java`: 更新登录方法，使用标准 OAuth2 流程
- `JwtTokenUserInterceptor.java`: 调整令牌验证逻辑

### 3.4 依赖调整
- 确保 `spring-boot-starter-oauth2-authorization-server` 版本正确
- 移除不必要的依赖

## 4. 技术要点

### 4.1 OAuth2 授权服务器配置
- 使用 `@EnableAuthorizationServer` 注解启用授权服务器
- 配置客户端信息和令牌设置

### 4.2 JWT 令牌生成
- 使用 `OAuth2TokenGenerator` 生成 JWT 令牌
- 配置 JWT 自定义声明

### 4.3 令牌验证
- 使用框架提供的 `JwtDecoder` 进行令牌验证
- 配置令牌验证规则

### 4.4 安全过滤器链
- 配置登录和注册接口的访问权限
- 配置需要认证的接口

## 5. 验证计划

### 5.1 功能验证
- 测试用户注册功能
- 测试用户登录功能
- 测试 JWT 令牌生成与验证
- 测试受保护接口的访问控制

### 5.2 性能验证
- 测试令牌生成的性能
- 测试令牌验证的性能

## 6. 风险评估

### 6.1 潜在风险
- 配置错误导致授权服务器无法启动
- JWT 令牌生成失败
- 令牌验证失败
- 与现有系统的兼容性问题

### 6.2 风险缓解措施
- 详细的配置文档
- 逐步实施变更
- 充分的测试
- 回滚计划

## 7. 预期成果

- 解决 JWT 令牌生成过程中的冲突问题
- 实现符合 spring-boot-starter-oauth2-authorization-server 最佳实践的认证授权系统
- 确保 JWT 令牌的正确生成与解析
- 提供清晰的配置和实现代码示例
