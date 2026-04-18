package com.bubbles.server.service;

import com.bubbles.common.result.PageResult;
import com.bubbles.pojo.dto.ArticleDTO;
import com.bubbles.pojo.dto.ArticlePageQueryDTO;
import com.bubbles.pojo.entity.Article;
import org.apache.ibatis.annotations.Delete;

public interface ArticleService {
    /**
     * 新增文章
     * @param articleDTO
     */
    void addArticle(ArticleDTO articleDTO);

    /**
     * 文章列表分页查询
     * @param articlePageQueryDTO
     * @return
     */
    PageResult pageQuery(ArticlePageQueryDTO articlePageQueryDTO);

    /**
     * 根据id获取文章详情
     * @param id
     * @return
     */
    Article detail(Long id);

    /**
     * 修改文章信息
     * @param articleDTO
     */
    void update(ArticleDTO articleDTO);

    /**
     * 根据id删除文章
     * @param id
     */

    void delete(Long id);
}
