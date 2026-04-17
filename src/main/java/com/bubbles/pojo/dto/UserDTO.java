package com.bubbles.pojo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO implements Serializable {
    @NotNull
    private Long id;
    @NotEmpty
    @Pattern(regexp="^\\${1,10}")
    private String username;
    @Pattern(regexp="^\\${1,10}")
    private String nickname;
    @Email
    private String email;

}
