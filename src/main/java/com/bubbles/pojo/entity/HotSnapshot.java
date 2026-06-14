package com.bubbles.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotSnapshot {
    private Long id;
    private LocalDateTime snapshotTime;
    private String bvid;
    private String title;
    private String url;
    private String coverUrl;
    private String author;
    private String tname;
    private String partitionTag;
    private LocalDateTime pubdate;
    private String description;
    private Integer rank;
    private Long viewCount;
    private Long likeCount;
    private Long coinCount;
    private Long favoriteCount;
    private Long shareCount;
    private Long danmakuCount;
    private Long replyCount;
    private BigDecimal hotScore;
    private LocalDateTime createdAt;
}
