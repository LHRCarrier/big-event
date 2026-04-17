package com.bubbles.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPageQueryDTO {

    private int page;

    private int pageSize;
    //分类名称

    private String categoryName;
    //分类别名
    private String categoryAlias;
    //更新时间
    private OffsetDateTime updateTime;
    //创建时间
    private OffsetDateTime createTime;
}
