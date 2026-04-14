package com.bubbles;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;

@SpringBootTest
public class JwtTest {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    public void testGen() {
        // 构建认证对象
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "testuser",
                "password123",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // 生成 JWT 令牌
        Instant now = Instant.now();
        org.springframework.security.oauth2.jwt.JwtClaimsSet claimsSet = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                .subject(authentication.getName())
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .claim("authorities", "ROLE_USER")
                .build();

        Jwt jwt = jwtEncoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(claimsSet));
        System.out.println("Generated token: " + jwt.getTokenValue());
        System.out.println("Token type: Bearer");
        System.out.println("Expires at: " + jwt.getExpiresAt());
    }
}
