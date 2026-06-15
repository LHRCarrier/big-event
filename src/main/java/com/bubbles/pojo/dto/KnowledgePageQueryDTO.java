package com.bubbles.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgePageQueryDTO {
    private int page;
    private int pageSize;
    private String category;
    private String keyword;
    private Integer quality;
    private Integer status;
}
