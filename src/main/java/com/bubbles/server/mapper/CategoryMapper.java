package com.bubbles.server.mapper;

import com.bubbles.pojo.dto.CategoryPageQueryDTO;
import com.bubbles.pojo.entity.Category;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper {
    /**
     * 新增文章分类
     * @param category
     */
    void category(Category category);

    /**
     * 文章分类分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    Page<Category> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);
}
