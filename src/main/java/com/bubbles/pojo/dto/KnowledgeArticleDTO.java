package com.bubbles.pojo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeArticleDTO {
    private Long id;

    @NotEmpty
    @Size(min = 2, max = 200)
    private String title;

    @NotEmpty
    private String content;

    @Size(max = 500)
    private String excerpt;

    private String category;

    private String tags;

    @Size(max = 100)
    private String author;

    @Size(max = 500)
    private String sourceUrl;

    private Integer quality;

    private Integer wordCount;

    private Integer status;
}
