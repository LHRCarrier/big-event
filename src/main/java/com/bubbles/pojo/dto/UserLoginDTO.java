package com.bubbles.pojo.dto;
import lombok.Data;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
@Data
@Schema(description = "用户登录时传输的数据模型")
public class UserLoginDTO implements Serializable {
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "用户密码")
    private String password;
}