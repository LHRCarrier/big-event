package com.bubbles.server.service.impl;

import com.bubbles.server.service.JwtUserService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class JwtUserServiceImpl implements JwtUserService {

    // 暂时注释掉 JWT 编码器和解码器的依赖
    // @Autowired
    // @Qualifier("jwtUserEncoder")
    // private JwtEncoder jwtEncoder;

    // @Autowired
    // @Qualifier("jwtUserDecoder")
    // private JwtDecoder jwtDecoder;

    // @PostConstruct
    // public void init() {
    //     System.out.println(">>> Injected JwtEncoder: " + jwtEncoder.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(jwtEncoder)));
    // }

    public String createToken(Map<String, Object> claims, long ttlMillis) {
        // 暂时使用 UUID 生成简单的令牌
        return UUID.randomUUID().toString();
    }

    public Map<String, Object> parseToken(String token) {
        // 暂时返回空 map
        return Map.of();
    }
}
