package com.bubbles.pojo.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserPageQueryDTO implements Serializable {

    private int page;

    private int pageSize;

    private Long id;

    private String username;

    private String email;

    private String nickname;


}
