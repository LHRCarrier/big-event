package com.bubbles.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
//io.swagger.annotations.ApiModel → io.swagger.v3.oas.annotations.media.Schema
//
//io.swagger.annotations.ApiModelProperty → io.swagger.v3.oas.annotations.media.Schema
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录返回的数据格式")
public class UserLoginVO implements Serializable {

    @Schema(description = "主键值")
    private Long id;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "jwt令牌")
    private String token;
}
