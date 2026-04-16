package com.bubbles.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleDTO {
    private String title;
    private String content;
    private Long categoryId;
    private String state;
    private String coverImg;
}
