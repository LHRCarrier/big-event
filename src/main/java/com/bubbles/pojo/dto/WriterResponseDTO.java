package com.bubbles.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI撰稿响应DTO
 * 用于接收AI服务返回的撰稿结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WriterResponseDTO {
    
    /**
     * 生成的文章唯一标识
     */
    private String articleId;
    
    /**
     * 文章标题
     */
    private String title;
    
    /**
     * 完整文章内容
     */
    private String content;
    
    /**
     * 文章摘要
     */
    private String summary;
    
    /**
     * 文章段落结构
     */
    private List<ArticleSection> sections;
    
    /**
     * 生成时间
     */
    private LocalDateTime generatedAt;
    
    /**
     * 使用的AI模型
     */
    private String modelUsed;
    
    /**
     * 文章段落模型
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticleSection {
        /**
         * 段落标题
         */
        private String title;
        
        /**
         * 段落内容
         */
        private String content;
    }
}