package com.bubbles.server.service;

import com.bubbles.pojo.entity.User;
import com.bubbles.pojo.vo.UserVO;

public interface UserService {
    /**
     * 用户注册
     * @param user
     */
    void register(User user);

    /**
     * 根据用户名查询
     * @param username
     * @return
     */
    User search(String username);

    /**
     * 查询用户信息
     * @param user
     * @return
     */
    UserVO listInfo(User user);
}
