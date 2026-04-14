package com.bubbles.server.mapper;

import com.bubbles.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    void add(User user);
    @Select("select * from user where username = #{username}")
    User searchByUsername(String username);
}
