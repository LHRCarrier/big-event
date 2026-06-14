package com.bubbles.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WriteFromHotRequestDTO {
    private String title;
    private String partition;
    private String category;
    private String author;
    @JsonProperty("view_count")
    private Long viewCount;
    @JsonProperty("like_count")
    private Long likeCount;
    @JsonProperty("coin_count")
    private Long coinCount;
    @JsonProperty("favorite_count")
    private Long favoriteCount;
    @JsonProperty("share_count")
    private Long shareCount;
    @JsonProperty("danmaku_count")
    private Long danmakuCount;
    @JsonProperty("reply_count")
    private Long replyCount;
    private String bvid;
    @JsonProperty("cover_url")
    private String coverUrl;
    @JsonProperty("hot_score")
    private Double hotScore;
    private Integer rank;
    private Integer length;
    private String style;
    private String audience;
    @JsonProperty("generate_summary")
    private Boolean generateSummary;
    private String description;
}
