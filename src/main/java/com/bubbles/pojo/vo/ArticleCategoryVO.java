package com.bubbles.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户查看种类返回的数据格式")
public class ArticleCategoryVO implements Serializable {

    @Schema(description = "主键值")
    private Long id;

    @Schema(description = "种类名称")
    private String categoryName;

    @Schema(description = "种类别名")
    private String categoryAlias;

}
