package com.bubbles.pojo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleCategoryDTO {
    private Long id;
    @NotEmpty
    @Pattern(regexp=".{2,}")
    private String categoryName;
    @NotEmpty
    @Pattern(regexp=".{2,}")
    private String categoryAlias;
}
