package com.bubbles.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

import java.util.stream.Collectors;

@Configuration
public class JwtCustomizerConfiguration {

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return (context) -> {
            // 仅对 Access Token 进行定制
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
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