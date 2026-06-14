package com.bubbles.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotTopicVO {
    private String bvid;
    private String title;
    private String url;
    private String coverUrl;
    private String author;
    private String tname;
    private String partitionTag;
    private LocalDateTime pubDate;
    private String description;
    private Long viewCount;
    private Long likeCount;
    private Long coinCount;
    private Long favoriteCount;
    private Long shareCount;
    private Long danmakuCount;
    private Long replyCount;
    private Double score;
    private boolean alreadyProcessed;
}
