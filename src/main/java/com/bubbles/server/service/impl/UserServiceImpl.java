package com.bubbles.server.service.impl;

import com.bubbles.common.context.BaseContext;
import com.bubbles.common.exception.BaseException;
import com.bubbles.pojo.dto.PasswordUpdateDTO;
import com.bubbles.pojo.dto.UserDTO;
import com.bubbles.pojo.vo.UserVO;
import com.bubbles.server.mapper.UserMapper;
import com.bubbles.server.service.UserService;
import com.bubbles.pojo.entity.User;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

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
    public UserVO listInfo(User user) {return userMapper.getInfoConditional(user);}

    /**
     * 更新用户信息
     * @param userDTO
     */
    public void update(UserDTO userDTO) {
        User user = new User();
        BeanUtils.copyProperties(userDTO,user);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);
    }

    /**
     * 更新用户头像
     * @param avatarUrl
     */
    public void updateAvatar(String avatarUrl) {
        Long currentId = BaseContext.getCurrentId();
        User user = new User();
        user.setId(currentId);
        UserVO userVO = userMapper.getInfoConditional(user);
        userVO.setUserPic(avatarUrl);
        BeanUtils.copyProperties(userVO,user);
        userMapper.update(user);
    }

    /**
     * 修改密码
     * @param passwordUpdateDTO
     */
    public void updatePassword(PasswordUpdateDTO passwordUpdateDTO) {
        if( !(passwordUpdateDTO.getPassword()).equals(passwordUpdateDTO.getConfirmNewPassword()) ){
            throw new BaseException("两次输入的密码不一致");
        }
        Long currentId = BaseContext.getCurrentId();
        User user = new User();
        user.setId(currentId);
        //根据用户id查询密码
        String validatedPassword = userMapper.getPasswordById(currentId);
        //传入的密码进行加密处理
        String password = DigestUtils.md5DigestAsHex((passwordUpdateDTO.getPassword()).getBytes());
        String OldPassword = DigestUtils.md5DigestAsHex((passwordUpdateDTO.getOldPassword()).getBytes());
        if(!OldPassword.equals(validatedPassword)  ){
            throw new BaseException("原密码错误!");
        };
        user.setPassword(password);
        userMapper.update(user);
    }

}
