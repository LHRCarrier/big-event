package com.bubbles.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 话题实体 —— 对应 topic 表
 * 跨平台话题归一化核心实体，替代 bvid 作为系统的中心抽象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Topic {
    private Long id;
    private String title;
    /** JSON array of alias strings */
    private String aliases;
    /** JSON array of keyword strings */
    private String keywords;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private String status;
    private Long mergedInto;
    private LocalDateTime createdAt;
}
