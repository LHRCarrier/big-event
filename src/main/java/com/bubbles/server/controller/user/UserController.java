package com.bubbles.server.controller.user;

import com.bubbles.common.Result;
import com.bubbles.pojo.vo.UserLoginVO;
import com.bubbles.server.service.UserService;
import com.bubbles.pojo.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
@Tag(name="用户相关接口",description = "包含注册，登录，及个人信息的增删改查")

public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtEncoder jwtEncoder;

    @PostMapping("/user/register")
    @Operation(summary="用户注册",description = "")
    public Result register(String username, String password){
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

        User u = userService.search(username);

        if(u==null){
            User user = new User();
            user.setPassword(md5Password);
            user.setUsername(username);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            userService.register(user);
            return Result.success("注册成功");
        }else{
            return Result.error("用户名已占用");
        }
    }
    @PostMapping("/user/login")
    @Operation(summary = "登录",description = "")
    public Result<Map<String, String>> login(String username, String password){
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
            org.springframework.security.oauth2.jwt.JwtClaimsSet claimsSet = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                    .subject(authentication.getName())
                    .issuedAt(now)
                    .expiresAt(now.plus(1, ChronoUnit.HOURS))
                    .claim("authorities", "ROLE_USER")
                    .build();

            Jwt jwt = jwtEncoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(claimsSet));
            Map<String, String> tokenMap = new HashMap<>();
            tokenMap.put("access_token", jwt.getTokenValue());
            tokenMap.put("token_type", "Bearer");
            tokenMap.put("expires_in", String.valueOf(jwt.getExpiresAt().getEpochSecond() - System.currentTimeMillis() / 1000));
            return Result.success(tokenMap);
        }
        return Result.error("密码错误");
    }

}
