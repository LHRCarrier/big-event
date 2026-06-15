package com.bubbles.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeArticle {
    private Long id;
    private String title;
    private String content;
    private String excerpt;
    private String category;
    private String tags;
    private String author;
    private String sourceUrl;
    private Integer quality;
    private Integer wordCount;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
