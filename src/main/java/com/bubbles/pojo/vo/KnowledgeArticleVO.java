package com.bubbles.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识库文章返回格式")
public class KnowledgeArticleVO implements Serializable {

    @Schema(description = "主键值")
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章内容")
    private String content;

    @Schema(description = "摘要")
    private String excerpt;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "原作者")
    private String author;

    @Schema(description = "来源链接")
    private String sourceUrl;

    @Schema(description = "质量评级 1-5")
    private Integer quality;

    @Schema(description = "字数")
    private Integer wordCount;

    @Schema(description = "状态 1=启用 0=停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "修改时间")
    private LocalDateTime updatedAt;
}
