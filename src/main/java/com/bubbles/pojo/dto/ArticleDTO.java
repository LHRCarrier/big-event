package com.bubbles.pojo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleDTO {
    private Long id;
    @NotEmpty
    @Pattern(regexp=".{2,}")
    private String title;
    @NotEmpty
    @Pattern(regexp=".{2,}")
    private String content;
    private Long categoryId;
    @NotEmpty
    @Pattern(regexp=".{2,}")
    private String state;
    @NotEmpty
    @Pattern(regexp=".{2,}")
    private String coverImg;
}
