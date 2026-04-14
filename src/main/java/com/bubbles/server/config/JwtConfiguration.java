package com.bubbles.server.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@Configuration
public class JwtConfiguration {

    @Value("${bubbles.jwt.user-secret-key}")
    private String userSecretKey;

    @Bean
    public JwtEncoder jwtEncoder() {
        // 生成密钥
        SecretKey secretKey = Keys.hmacShaKeyFor(userSecretKey.getBytes(StandardCharsets.UTF_8));

        // 创建自定义JwtEncoder
        return parameters -> {
            JwtClaimsSet claimsSet = parameters.getClaims();
            Instant now = Instant.now();

            // 手动构建claims map，处理Instant类型
            java.util.Map<String, Object> claims = new java.util.HashMap<>();
            for (java.util.Map.Entry<String, Object> entry : claimsSet.getClaims().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                // 处理Instant类型的值
                if (value instanceof Instant) {
                    claims.put(key, java.util.Date.from((Instant) value));
                } else {
                    claims.put(key, value);
                }
            }

            // 使用jjwt库生成JWT令牌
            String tokenValue = Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(java.util.Date.from(now))
                    .setExpiration(java.util.Date.from(claimsSet.getExpiresAt()))
                    .signWith(secretKey, SignatureAlgorithm.HS256)
                    .compact();

            // 返回Jwt对象
            return Jwt.withTokenValue(tokenValue)
                    .headers(headers -> headers.put("alg", "HS256"))
                    .claims(c -> c.putAll(claimsSet.getClaims()))
                    .build();
        };
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey secretKey = Keys.hmacShaKeyFor(userSecretKey.getBytes(StandardCharsets.UTF_8));
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}