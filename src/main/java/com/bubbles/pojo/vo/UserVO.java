package com.bubbles.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

//io.swagger.annotations.ApiModel → io.swagger.v3.oas.annotations.media.Schema
//
//io.swagger.annotations.ApiModelProperty → io.swagger.v3.oas.annotations.media.Schema
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录返回的数据格式")
public class UserVO implements Serializable {

    @Schema(description = "主键值")
    private Long id;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "昵称")
    private String nickName;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "用户头像")
    private String userPic;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}