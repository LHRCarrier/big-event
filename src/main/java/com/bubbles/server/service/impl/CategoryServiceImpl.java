package com.bubbles.server.service.impl;

import com.bubbles.common.context.BaseContext;
import com.bubbles.common.result.PageResult;
import com.bubbles.pojo.dto.ArticleCategoryDTO;
import com.bubbles.pojo.dto.CategoryPageQueryDTO;
import com.bubbles.pojo.entity.Category;
import com.bubbles.server.mapper.CategoryMapper;
import com.bubbles.server.service.CategoryService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryMapper categoryMapper;
    /**
     * 新增文章分类
     * @param articleCategoryDTO
     */
    public void addCategory(ArticleCategoryDTO articleCategoryDTO) {
        Long currentId = BaseContext.getCurrentId();
        Category category = new Category();
        BeanUtils.copyProperties(articleCategoryDTO,category);
        category.setCreateUser(currentId);
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.category(category);
    }

    /**
     * 分页查询文章分类
     * @param categoryPageQueryDTO
     * @return
     */
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        PageHelper.startPage(categoryPageQueryDTO.getPage(),categoryPageQueryDTO.getPageSize());
        Page<Category> page = categoryMapper.pageQuery(categoryPageQueryDTO);
        long total = page.getTotal();
        List<Category> records = page.getResult();
        return new PageResult(total,records);
    }

}
