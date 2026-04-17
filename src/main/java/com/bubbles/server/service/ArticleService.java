package com.bubbles.server.service;

import com.bubbles.pojo.dto.ArticleCategoryDTO;
import com.bubbles.pojo.dto.ArticleDTO;

public interface ArticleService {
    /**
     * 新增文章
     * @param articleDTO
     */
    void addArticle(ArticleDTO articleDTO);

}
