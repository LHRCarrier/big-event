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
public class ArticleCategory {
    private Long id;
    private String categoryName;
    private String categoryAlias;
    private Long createUser;
    private LocalDateTime createTime;
//    private Long updateUser;
    private LocalDateTime updateTime;
}
