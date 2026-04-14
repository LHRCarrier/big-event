package com.bubbles.server.service.impl;

import com.bubbles.pojo.entity.User;
import com.bubbles.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 从数据库中查询用户
        User user = userService.search(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 构建UserDetails对象
        UserBuilder builder = org.springframework.security.core.userdetails.User.builder();
        builder.username(user.getUsername());
        builder.password(user.getPassword()); // 密码已经是MD5加密的，直接使用
        builder.roles("USER"); // 默认角色

        return builder.build();
    }
}
