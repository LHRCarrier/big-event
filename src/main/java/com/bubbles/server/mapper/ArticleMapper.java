package com.bubbles.server.mapper;

import com.bubbles.pojo.dto.ArticleCategoryDTO;
import com.bubbles.pojo.dto.ArticleDTO;
import com.bubbles.pojo.entity.Article;
import com.bubbles.pojo.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArticleMapper {
    /**
     * 添加文章
     * @param article
     */
    void add(Article article);

    /**
     * 新增文章分类
     * @param category
     */
    void category(Category category);
}
