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