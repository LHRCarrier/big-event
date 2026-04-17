package com.bubbles.pojo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@NotNull
@Pattern(regexp="^\\${5,16}")
public class PasswordUpdateDTO {
    private String oldPassword;
    private String password;
    private String confirmNewPassword;
}
