package com.bubbles.server.mapper;

import com.bubbles.pojo.entity.User;
import com.bubbles.pojo.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    void add(User user);
    @Select("select * from user where username = #{username}")
    User searchByUsername(String username);

    /**
     * 条件动态查询用户信息
     * @param user
     * @return
     */
    UserVO getInfoConditional(User user);

    /**
     * 更新用户信息
     * @param user
     */
    void update(User user);

    /**
     * 查询密码
     * @param currentId
     * @return
     */
    @Select("select user.password from user where id = #{id}")
    String getPasswordById(Long currentId);
}
