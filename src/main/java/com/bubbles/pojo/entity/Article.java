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
public class Article {
    private Long id;
    private String title;
    private String content;
    private String coverImg;
    private String state;
    /** 来源平台: bilibili/zhihu/weibo/toutiao */
    private String platform;
    /** 关联话题ID (对应 topic 表) */
    private Long topicId;
    private Long categoryId;
    private Long createUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
