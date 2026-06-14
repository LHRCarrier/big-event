package com.bubbles.pojo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BiliHotResponseDTO {
    @JsonProperty("hot_type")
    private String hotType;
    private List<BiliHotItemDTO> items;
    private String source;

    @Data
    public static class BiliHotItemDTO {
        private Integer rank;
        private String title;
        @JsonProperty("view_count")
        private Long viewCount;
        @JsonProperty("cover_url")
        private String coverUrl;
        private String bvid;
        private String author;
        private String url;
        private String category;
        @JsonProperty("pubdate")
        private String pubdate;
        private String description;
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
    }
}
