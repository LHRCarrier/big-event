package com.bubbles.server.service.impl;

import com.bubbles.pojo.vo.UserVO;
import com.bubbles.server.mapper.UserMapper;
import com.bubbles.server.service.UserService;
import com.bubbles.pojo.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    /**
     * 用户注册
     * @param user
     *
     */
    public void register(User user) {
        userMapper.add(user);
    }

    /**
     * 根据用户名查询用户
     * @param username
     * @return
     */
    public User search(String username) {
        return userMapper.searchByUsername(username);
    }

    /**
     * 条件查询用户信息
     * @param user
     * @return
     */
    public UserVO listInfo(User user) {
        return userMapper.getInfoConditional(user);
    }
}
