package com.bubbles.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleCategoryDTO {
    private Long id;
    private String categoryName;
    private String categoryAlias;
}
