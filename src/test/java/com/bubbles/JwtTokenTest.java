package com.bubbles;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@SpringBootTest
public class JwtTokenTest {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    public void testJwtTokenGeneration() {
        // 生成 JWT 令牌
        Instant now = Instant.now();
        org.springframework.security.oauth2.jwt.JwtClaimsSet claimsSet = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                .subject("testuser")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .claim("authorities", "ROLE_USER")
                .build();

        Jwt jwt = jwtEncoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(claimsSet));
        System.out.println("Generated token: " + jwt.getTokenValue());
        System.out.println("Token type: Bearer");
        System.out.println("Expires at: " + jwt.getExpiresAt());
        System.out.println("Subject: " + jwt.getSubject());
        System.out.println("Authorities: " + jwt.getClaim("authorities"));
    }
}
