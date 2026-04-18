package com.bubbles.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticlePageQueryDTO {

    private int page;

    private int pageSize;
    //文章标题
    private String title ;
    //发布状态
    private String state;

    //分类id
    private Long categoryId;
    //更新时间
    private OffsetDateTime updateTime;
    //创建时间
    private OffsetDateTime createTime;
}
