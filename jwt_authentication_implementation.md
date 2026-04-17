# Spring Boot 项目中使用 spring-boot-starter-oauth2-authorization-server 实现 JWT 认证

## 1. 前提条件和环境设置要求

### 1.1 系统要求
- JDK 17 或更高版本
- Maven 3.6 或更高版本
- Spring Boot 3.0 或更高版本
- IDE（如 IntelliJ IDEA、Eclipse 等）

### 1.2 环境准备
1. 安装 JDK 17 或更高版本
2. 安装 Maven 3.6 或更高版本
3. 配置 JAVA_HOME 和 MAVEN_HOME 环境变量
4. 确保可以通过命令行访问 `java` 和 `mvn` 命令

## 2. 依赖配置

### 2.1 Maven 依赖配置
在 `pom.xml` 文件中添加以下依赖：

```xml
<!-- Spring Boot 核心依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>

<!-- Web 依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>

<!-- OAuth2 授权服务器依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-authorization-server</artifactId>
</dependency>

<!-- 验证依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Lombok 依赖 -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.30</version>
    <scope>compile</scope>
</dependency>

<!-- 测试依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 2.2 Gradle 依赖配置
在 `build.gradle` 文件中添加以下依赖：

```gradle
// Spring Boot 核心依赖
implementation 'org.springframework.boot:spring-boot-starter'

// Web 依赖
implementation 'org.springframework.boot:spring-boot-starter-webmvc'

// OAuth2 授权服务器依赖
implementation 'org.springframework.boot:spring-boot-starter-oauth2-authorization-server'

// 验证依赖
implementation 'org.springframework.boot:spring-boot-starter-validation'

// Lombok 依赖
compileOnly 'org.projectlombok:lombok:1.18.30'
annotationProcessor 'org.projectlombok:lombok:1.18.30'

// 测试依赖
testImplementation 'org.springframework.boot:spring-boot-starter-test'
```

## 3. OAuth2 授权服务器配置

### 3.1 创建授权服务器配置类
创建 `AuthorizationServerConfig.java` 文件：

```java
package com.bubbles.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import java.util.UUID;

@Configuration
public class AuthorizationServerConfig {

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("big-event-client")
                .clientSecret("{noop}big-event-secret")
                .redirectUri("http://localhost:8080/login/oauth2/code/big-event-client")
                .scope("read")
                .scope("write")
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();

        return new InMemoryRegisteredClientRepository(registeredClient);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:8080")
                .build();
    }
}
```

### 3.2 配置说明
- `RegisteredClient`：定义客户端信息，包括客户端 ID、客户端密钥、重定向 URI、授权范围和授权类型
- `AuthorizationServerSettings`：配置授权服务器的设置，包括 issuer URI

## 4. JWT 令牌生成和验证设置

### 4.1 创建 JWT 自定义器配置
创建 `JwtCustomizerConfiguration.java` 文件：

```java
package com.bubbles.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.stream.Collectors;

@Configuration
public class JwtCustomizerConfiguration {

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return (context) -> {
            // 仅对 Access Token 进行定制
            if (context.getTokenType().getValue().equals("access_token")) {
                Authentication principal = context.getPrincipal();
                // 添加自定义 claims
                context.getClaims().claims(claims -> {
                    // 添加权限列表
                    String authorities = principal.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.joining(","));
                    claims.put("authorities", authorities);
                    // 添加其他信息，例如用户ID（需要从 principal 中提取）
                    // claims.put("user_id", principal.getDetails());
                });
            }
        };
    }
}
```

### 4.2 配置 JWT 属性
创建 `JwtProperties.java` 文件：

```java
package com.bubbles.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bubbles.jwt")
public class JwtProperties {
    /**
     * 用户端令牌
     */
    private String userSecretKey;
    private long userTtl;
    private String userTokenName;

    /**
     * 管理端令牌
     */
    private String adminSecretKey;
    private long adminTtl;
    private String adminTokenName;
}
```

### 4.3 在 application.yml 中配置 JWT 属性

```yaml
bubbles:
  jwt:
    user-secret-key: bubblesbubblesbubblesbubblesbubblesbubbles
    user-ttl: 7200000
    user-token-name: authentication
    admin-secret-key: bubblesbubblesbubblesbubblesbubblesbubbles
    admin-ttl: 7200000
    admin-token-name: admin_token
```

### 4.4 JWT 令牌生成流程

#### 4.4.1 使用 OAuth2TokenGenerator 生成 JWT 令牌

在 `UserController` 类中，我们可以使用 `OAuth2TokenGenerator` 来生成 JWT 令牌。首先，需要注入 `OAuth2TokenGenerator` 实例：

```java
@Autowired
private OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
```

然后，在登录方法中，使用 `OAuth2TokenGenerator` 生成 JWT 令牌：

```java
@PostMapping("/user/login")
@Operation(summary = "登录",description = "")
public Result<UserLoginVO> login(String username,String password){
    User user = userService.search(username);
    if(user==null){
        return Result.error("用户名错误");
    }
    String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

    if(user.getPassword().equals(md5Password)){
        // 构建认证对象
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username,
                password,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 生成 JWT 令牌
        OAuth2TokenContext tokenContext = OAuth2TokenContext.builder()
                .principal(authentication)
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();

        OAuth2Token token = tokenGenerator.generate(tokenContext);

        UserLoginVO userLoginVO = new UserLoginVO();
        userLoginVO.setId(user.getId());
        userLoginVO.setUserName(user.getUsername());
        userLoginVO.setName(user.getUsername());
        userLoginVO.setToken(token.getTokenValue());
        return Result.success(userLoginVO);

    }
    return  Result.error("密码错误");
}
```

#### 4.4.2 使用 JwtEncoder 生成 JWT 令牌

除了使用 `OAuth2TokenGenerator`，我们还可以直接使用 `JwtEncoder` 来生成 JWT 令牌：

```java
@Autowired
private JwtEncoder jwtEncoder;

@PostMapping("/user/login")
@Operation(summary = "登录",description = "")
public Result<UserLoginVO> login(String username,String password){
    User user = userService.search(username);
    if(user==null){
        return Result.error("用户名错误");
    }
    String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

    if(user.getPassword().equals(md5Password)){
        // 构建认证对象
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username,
                password,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 生成 JWT 令牌
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", user.getId());
        claims.put("username", user.getUsername());

        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(now.plusSeconds(7200)) // 2小时过期
                .subject(username)
                .claims(map -> map.putAll(claims))
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claimsSet)).getTokenValue();

        UserLoginVO userLoginVO = new UserLoginVO();
        userLoginVO.setId(user.getId());
        userLoginVO.setUserName(user.getUsername());
        userLoginVO.setName(user.getUsername());
        userLoginVO.setToken(token);
        return Result.success(userLoginVO);

    }
    return  Result.error("密码错误");
}
```

#### 4.4.3 完整的 JWT 令牌生成流程

1. **用户认证**：用户提供用户名和密码，系统验证用户身份
2. **构建认证对象**：创建 `UsernamePasswordAuthenticationToken` 对象，包含用户信息和权限
3. **设置安全上下文**：将认证对象设置到 `SecurityContextHolder` 中
4. **生成 JWT 令牌**：
   - 方法一：使用 `OAuth2TokenGenerator` 生成令牌
   - 方法二：使用 `JwtEncoder` 生成令牌
5. **返回令牌**：将生成的令牌返回给客户端

#### 4.4.4 JWT 令牌的结构

一个典型的 JWT 令牌包含三个部分：

1. **Header**：包含令牌类型和签名算法
   ```json
   {
     "alg": "HS256",
     "typ": "JWT"
   }
   ```

2. **Payload**：包含声明（claims），如用户信息、过期时间等
   ```json
   {
     "sub": "test",
     "user_id": 4,
     "username": "test",
     "iat": 1617295622,
     "exp": 1617302822
   }
   ```

3. **Signature**：使用密钥对 Header 和 Payload 进行签名，确保令牌的完整性和真实性
   ```
   HMACSHA256(
     base64UrlEncode(header) + "." +
     base64UrlEncode(payload),
     secret
   )
   ```

#### 4.4.5 JWT 令牌的验证流程

1. **提取令牌**：从请求头中提取 JWT 令牌
2. **解析令牌**：使用 `JwtDecoder` 解析令牌，验证签名和过期时间
3. **提取声明**：从令牌中提取声明，如用户 ID、用户名等
4. **验证权限**：根据提取的声明验证用户权限
5. **设置上下文**：将用户信息设置到 `SecurityContextHolder` 中，供后续请求使用

## 5. 资源服务器的安全配置

### 5.1 创建安全配置类
创建 `SecurityConfiguration.java` 文件：

```java
package com.bubbles.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/user/user/login").permitAll()
                    .requestMatchers("/user/user/register").permitAll()
                    .requestMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**").permitAll()
                    .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
```

### 5.2 配置说明
- `authorizeHttpRequests`：配置请求的访问权限
- `permitAll()`：允许无需认证的请求
- `anyRequest().authenticated()`：要求所有其他请求都需要认证
- `csrf.disable()`：禁用 CSRF 保护（在前后端分离的应用中通常需要禁用）

## 6. 关键组件的实用代码示例

### 6.1 UserController 类

```java
package com.bubbles.server.controller.user;

import com.bubbles.common.result.Result;
import com.bubbles.pojo.vo.UserLoginVO;
import com.bubbles.server.service.UserService;
import com.bubbles.pojo.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/user")
@Slf4j
@Tag(name = "用户相关接口", description = "包含注册，登录，及个人信息的增删改查")
public class UserController {
   @Autowired
   private UserService userService;

   @PostMapping("/user/register")
   @Operation(summary = "用户注册", description = "")
   public Result register(String username, String password) {
      String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

      User u = userService.search(username);

      if (u == null) {
         User user = new User();
         user.setPassword(md5Password);
         user.setUsername(username);
         user.setCreateTime(LocalDateTime.now());
         user.setUpdateTime(LocalDateTime.now());
         userService.register(user);
         return Result.success("注册成功");
      } else {
         return Result.error("用户名已占用");
      }
   }

   @PostMapping("/user/login")
   @Operation(summary = "登录", description = "")
   public Result<UserLoginVO> login(String username, String password) {
      User user = userService.search(username);
      if (user == null) {
         return Result.error("用户名错误");
      }
      String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

      if (user.getPassword().equals(md5Password)) {
         // 生成简单的令牌
         String token = UUID.randomUUID().toString();

         UserLoginVO userLoginVO = new UserLoginVO();
         userLoginVO.setId(user.getId());
         userLoginVO.setUserName(user.getUsername());
         userLoginVO.setName(user.getUsername());
         userLoginVO.setToken(token);
         return Result.success(userLoginVO);

      }
      return Result.error("密码错误");
   }
}
```

### 6.2 JwtTokenUserInterceptor 类

```java
package com.bubbles.server.interceptor;

import com.bubbles.common.constant.JwtClaimsConstant;
import com.bubbles.common.context.BaseContext;
import com.bubbles.common.properties.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * jwt令牌校验的拦截器（使用 Spring Security OAuth2 Resource Server 解析）
 */
@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private JwtDecoder jwtDecoder;   // 使用框架自动配置的 JwtDecoder

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("当前线程id:" + Thread.currentThread().getId());
        System.out.println("当前请求路径:" + request.getRequestURI());

        // 判断当前拦截到的是 Controller 的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            // 当前拦截到的不是动态方法，直接放行
            return true;
        }

        // 1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getUserTokenName());
        System.out.println("获取到的令牌:" + token);

        // 2、使用 JwtDecoder 校验令牌并解析 Claims
        try {
            log.info("jwt校验: {}", token);
            Jwt jwt = jwtDecoder.decode(token);                     // 解析 JWT，若无效或过期会抛出异常
            Long userId = jwt.getClaim(JwtClaimsConstant.USER_ID);  // 直接获取自定义 claim，支持类型转换
            if (userId == null) {
                throw new JwtException("Token 中缺少 user_id 字段");
            }
            log.info("当前用户id: {}", userId);
            BaseContext.setCurrentId(userId);
            // 3、通过，放行
            return true;
        } catch (JwtException ex) {
            // 4、校验失败（签名错误、过期、格式错误等），响应 401 状态码
            log.warn("JWT 校验失败: {}", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            return false;
        }
    }
}
```

### 6.3 WebMvcConfiguration 类

```java
package com.bubbles.server.config;

import com.bubbles.server.interceptor.JwtTokenUserInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    /**
     * 注册自定义拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns("/user/user/register", "/user/user/login");
    }

    /**
     * 设置静态资源映射
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始设置静态资源映射...");
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
```

## 7. 验证 JWT 功能的测试程序

### 7.1 测试注册接口

```bash
# 使用 curl 测试注册接口
curl -X POST "http://localhost:8080/user/user/register" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=test&password=123456"
```

### 7.2 测试登录接口

```bash
# 使用 curl 测试登录接口
curl -X POST "http://localhost:8080/user/user/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=test&password=123456"
```

### 7.3 测试受保护的接口

```bash
# 使用 curl 测试受保护的接口（需要携带令牌）
curl -X GET "http://localhost:8080/user/some-protected-endpoint" \
  -H "Authorization: Bearer <token>"
```

## 8. 常见问题和故障排除指南

### 8.1 常见问题

1. **服务器启动失败，提示找不到 JwtEncoder 或 JwtDecoder**
   - 原因：手动配置的 JwtEncoder 和 JwtDecoder 与框架内置的编码器产生冲突
   - 解决方法：移除手动配置的 JwtEncoder 和 JwtDecoder，使用框架自动配置的组件

2. **登录接口返回 401 错误**
   - 原因：请求被拦截器拦截，令牌验证失败
   - 解决方法：确保登录接口被排除在拦截器之外，检查令牌生成和验证逻辑

3. **授权服务器启动失败，提示 authorizationGrantTypes cannot be empty**
   - 原因：RegisteredClient 对象没有设置授权类型
   - 解决方法：在创建 RegisteredClient 对象时添加授权类型

4. **Lombok 注解没有被正确处理**
   - 原因：Lombok 依赖配置不正确
   - 解决方法：确保 Lombok 依赖的 scope 设置为 compile

### 8.2 故障排除步骤

1. 检查依赖配置是否正确
2. 检查授权服务器配置是否完整
3. 检查安全配置是否正确
4. 检查拦截器配置是否正确
5. 检查 JWT 令牌生成和验证逻辑是否正确
6. 查看服务器日志，了解具体的错误信息
7. 使用调试工具，如 Postman，测试接口

## 9. 生产环境中 JWT 实现的最佳实践

### 9.1 密钥管理
- 使用强密钥：密钥长度至少为 32 字节
- 定期轮换密钥：避免长期使用同一个密钥
- 安全存储密钥：使用环境变量或密钥管理服务存储密钥，避免硬编码在配置文件中

### 9.2 令牌管理
- 设置合理的令牌过期时间：根据业务需求设置合适的过期时间
- 实现令牌刷新机制：使用刷新令牌更新访问令牌
- 实现令牌撤销机制：在用户登出或密码变更时撤销令牌

### 9.3 安全措施
- 使用 HTTPS：在生产环境中使用 HTTPS 保护通信
- 限制令牌作用域：根据用户权限设置合适的令牌作用域
- 实现速率限制：防止暴力破解和 DoS 攻击
- 监控异常登录：及时发现和处理异常登录行为

### 9.4 性能优化
- 使用缓存：缓存令牌验证结果，减少重复验证
- 优化数据库查询：减少令牌验证过程中的数据库查询
- 使用异步处理：处理令牌生成和验证等耗时操作

## 10. 运行和验证 "bigevent" 应用程序

### 10.1 运行应用程序

1. 确保数据库连接配置正确
2. 构建项目：
   ```bash
   mvn clean package -DskipTests
   ```
3. 运行应用程序：
   ```bash
   java -jar target/big-event-0.0.1-SNAPSHOT.jar
   ```

### 10.2 验证 JWT 认证功能

1. 注册用户：
   ```bash
   curl -X POST "http://localhost:8080/user/user/register" \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "username=test&password=123456"
   ```

2. 登录获取令牌：
   ```bash
   curl -X POST "http://localhost:8080/user/user/login" \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "username=test&password=123456"
   ```

3. 使用令牌访问受保护的接口：
   ```bash
   curl -X GET "http://localhost:8080/user/some-protected-endpoint" \
     -H "Authorization: Bearer <token>"
   ```

### 10.3 验证授权服务器功能

1. 访问授权服务器的发现端点：
   ```bash
   curl http://localhost:8080/.well-known/openid-configuration
   ```

2. 使用授权码模式获取令牌：
   - 访问授权端点：`http://localhost:8080/oauth2/authorize?client_id=big-event-client&response_type=code&redirect_uri=http://localhost:8080/login/oauth2/code/big-event-client&scope=read write`
   - 使用授权码获取令牌：
     ```bash
     curl -X POST "http://localhost:8080/oauth2/token" \
       -H "Content-Type: application/x-www-form-urlencoded" \
       -d "grant_type=authorization_code&code=<authorization_code>&redirect_uri=http://localhost:8080/login/oauth2/code/big-event-client&client_id=big-event-client&client_secret=big-event-secret"
     ```

## 总结

通过本指南，您已经了解了如何在 Spring Boot 项目中使用 spring-boot-starter-oauth2-authorization-server 依赖来实现 JWT 认证。我们以 "bigevent" 项目为例，详细介绍了从环境设置到代码实现的完整流程，包括授权服务器配置、JWT 令牌生成和验证、安全配置等关键步骤。

在实际项目中，您可以根据业务需求和安全要求，进一步调整和优化 JWT 认证实现。同时，您也可以参考 Spring Security 和 OAuth2 的官方文档，获取更多关于 JWT 认证的详细信息。

希望本指南对您有所帮助，祝您在 Spring Boot 项目中成功实现 JWT 认证！