package com.bubbles.server.service;

import com.bubbles.common.result.PageResult;
import com.bubbles.pojo.dto.ArticleCategoryDTO;
import com.bubbles.pojo.dto.CategoryPageQueryDTO;

public interface ArticleCategoryService {
    /**
     * 新增文章分类
     * @param articleCategoryDTO
     */
    void addCategory(ArticleCategoryDTO articleCategoryDTO);

    /**
     * 分页查询分类列表
     * @param categoryPageQueryDTO
     * @return
     */
    PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 修改文章分类属性值
     * @param articleCategoryDTO
     */
    void update(ArticleCategoryDTO articleCategoryDTO);

    /**
     * 根据id删除文章分类
     * @param id
     */
    void delete(Long id);
}
