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
@Schema(description = "用户查看文章返回的数据格式")
public class ArticleVO implements Serializable {

    @Schema(description = "主键值")
    private Long id ;

    @Schema(description = "标题")
    private String title;

    //以后这里需要改为"概要 description "字段
    @Schema(description = "内容")
    private String content;

    @Schema(description = "封面链接")
    private String coverImg;

    @Schema(description = "发布状态")
    private String state;
}
