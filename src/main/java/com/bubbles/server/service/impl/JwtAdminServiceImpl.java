package com.bubbles.server.service.impl;

import com.bubbles.server.service.JwtAdminService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class JwtAdminServiceImpl implements JwtAdminService {

    // 暂时注释掉 JWT 编码器和解码器的依赖
    // @Autowired
    // @Qualifier("jwtAdminEncoder")
    // private JwtEncoder jwtEncoder;

    // @Autowired
    // @Qualifier("jwtAdminDecoder")
    // private JwtDecoder jwtDecoder;

    public String createToken(Map<String, Object> claims, long ttlMillis) {
        // 暂时使用 UUID 生成简单的令牌
        return UUID.randomUUID().toString();
    }

    public Map<String, Object> parseToken(String token) {
        // 暂时返回空 map
        return Map.of();
    }
}
