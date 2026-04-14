package com.bubbles.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Artical {
    private Long id;
    private String title;
    private String content;
    private String coverImg;
    private String state;
    private Long categoryId;
    private Long createUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
