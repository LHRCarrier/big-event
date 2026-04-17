package com.bubbles.server.service;

import com.bubbles.pojo.dto.PasswordUpdateDTO;
import com.bubbles.pojo.dto.UserDTO;
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

    /**
     * 更新用户信息
     * @param userDTO
     */
    void update(UserDTO userDTO);

    /**
     * 更新用户头像
     * @param avatarUrl
     */
    void updateAvatar(String avatarUrl);

    /**
     * 根据用户id修改对应密码:当前用户
     * @param currentId
     */
    void updatePassword(PasswordUpdateDTO currentId);
}
