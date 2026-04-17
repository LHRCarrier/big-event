package com.bubbles.server.controller.user;

import com.bubbles.common.result.Result;
import com.bubbles.common.context.BaseContext;
import com.bubbles.pojo.dto.PasswordUpdateDTO;
import com.bubbles.pojo.dto.UserDTO;
import com.bubbles.pojo.dto.UserLoginDTO;
import com.bubbles.pojo.vo.UserLoginVO;
import com.bubbles.pojo.vo.UserVO;
import com.bubbles.server.service.UserService;
import com.bubbles.pojo.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 用户注册
     * @param username
     * @param password
     * @return
     */
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

    /**
     * 用户登录
     * @param userLoginDTO
     *
     * @return
     */
    @PostMapping("/user/login")
    @Operation(summary = "登录",description = "")
    public Result<UserLoginVO> login(UserLoginDTO userLoginDTO){
        User user = userService.search(userLoginDTO.getUsername());
        if(user==null){
            return Result.error("用户名错误");
        }
        String md5Password = DigestUtils.md5DigestAsHex((userLoginDTO.getPassword()).getBytes());

        if(user.getPassword().equals(md5Password)){
            // 构建认证对象
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userLoginDTO.getUsername(),
                    userLoginDTO.getPassword(),
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
                    .claim("userId", user.getId())
                    .build();

            Jwt jwt = jwtEncoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(claimsSet));
            Map<String, String> tokenMap = new HashMap<>();
            tokenMap.put("access_token", jwt.getTokenValue());
            tokenMap.put("token_type", "Bearer");
            tokenMap.put("expires_in", String.valueOf(jwt.getExpiresAt().getEpochSecond() - System.currentTimeMillis() / 1000));
            String token = tokenMap.get("access_token");

            UserLoginVO userLoginVO = UserLoginVO.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .token(token)
                    .build();
            return Result.success(userLoginVO);
        }
        return Result.error("密码错误");
    }

    /**
     * 获取当前用户的详细信息
     * @return
     */
    @GetMapping("/user/userInfo")
    @Operation(summary = "获取用户详细信息",description = "获取用户详细信息")
    public Result<UserVO> getUserInfo(){
        Long userId = BaseContext.getCurrentId();
        log.info("正在查询当前用户信息 ,用户id{}...",userId);
        User user =new User();
        user.setId(userId);
        UserVO userVO = userService.listInfo(user);
        return Result.success(userVO);
    }

    /**
     * 更新用户信息:当前用户
     * @param userDTO
     * @return
     */
    @PutMapping("/user/update")
    @Operation(summary = "更新用户基本信息",description = "更新用户头像")
    public Result updateInfo(@RequestBody @Validated UserDTO userDTO){
        userService.update(userDTO);
        return Result.success("信息编辑成功");
    }

    /**
     * 修改头像
     * @param avatarUrl
     * @return
     */
    @PutMapping("/user/updateAvatar")
    @Operation(summary = "更新用户头像",description = "更新用户头像")
    public Result updateAvatar(String avatarUrl){
        userService.updateAvatar(avatarUrl);
        return Result.success("头像修改成功");
    }

    /**
     * 修改用户密码
     * @param passwordUpdateDTO
     * @return
     */
    @PutMapping("/user/updatePassword")
    @Operation(summary = "修改密码",description = "修改密码")
    public Result updatePassword(@Validated PasswordUpdateDTO passwordUpdateDTO){
        userService.updatePassword(passwordUpdateDTO);
        return  Result.success("密码已修改成功!");
    }
}
