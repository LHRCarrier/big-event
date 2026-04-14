package com.bubbles.server.service;

import java.util.Map;

public interface JwtUserService {

    String createToken(Map<String, Object> claims, long adminTtl);
    Map<String, Object> parseToken(String token);
//    String generateToken(Map<String, Object> claims, long adminTtl);
}
