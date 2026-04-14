package com.bubbles.pojo.entity;

import com.bubbles.common.enumeration.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class User {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String nickname;
    private String userPic;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Role role;//这个Role字段在数据库里应该设置为什么属性
}
