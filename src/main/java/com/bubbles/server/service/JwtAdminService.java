package com.bubbles.server.service;

import java.util.Map;

public interface JwtAdminService {
    String createToken(Map<String, Object> claims, long adminTtl);
    Map<String, Object> parseToken(String token);
}
