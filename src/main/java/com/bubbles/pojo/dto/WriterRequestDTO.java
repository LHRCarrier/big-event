package com.bubbles.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI撰稿请求DTO
 * 用于向AI服务发送撰稿请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WriterRequestDTO {
    
    /**
     * 要撰写的话题或关键词
     */
    private String topic;
    
    /**
     * 文章预期长度（字数），null表示不限制
     */
    @Builder.Default
    private Integer length = null;
    
    /**
     * 文章风格：neutral(中性)/formal(正式)/casual(轻松)/technical(技术)
     */
    @Builder.Default
    private String style = "neutral";
    
    /**
     * 目标受众：general(大众)/professional(专业人士)/student(学生)
     */
    @Builder.Default
    private String audience = "general";
    
    /**
     * 参考信息列表，帮助AI了解更多背景
     */
    @Builder.Default
    private List<String> references = List.of();
    
    /**
     * 是否生成文章摘要
     */
    @Builder.Default
    private Boolean generateSummary = true;

    /**
     * 是否使用知识库检索高质量参考文章作为风格锚点
     */
    @Builder.Default
    private Boolean useKnowledge = true;
}