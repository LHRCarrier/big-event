package com.bubbles.server.service.impl;

import com.bubbles.common.context.BaseContext;
import com.bubbles.common.result.PageResult;
import com.bubbles.pojo.dto.ArticleCategoryDTO;
import com.bubbles.pojo.dto.CategoryPageQueryDTO;
import com.bubbles.pojo.entity.ArticleCategory;
import com.bubbles.pojo.vo.ArticleCategoryVO;
import com.bubbles.pojo.vo.ArticleVO;
import com.bubbles.server.mapper.ArticleCategoryMapper;
import com.bubbles.server.service.ArticleCategoryService;
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
public class ArticleCategoryServiceImpl implements ArticleCategoryService {
    @Autowired
    private ArticleCategoryMapper articleCategoryMapper;
    /**
     * 新增文章分类
     * @param articleCategoryDTO
     */
    public void addCategory(ArticleCategoryDTO articleCategoryDTO) {
        Long currentId = BaseContext.getCurrentId();
        ArticleCategory articleCategory = new ArticleCategory();
        BeanUtils.copyProperties(articleCategoryDTO, articleCategory);
        articleCategory.setCreateUser(currentId);
        articleCategory.setCreateTime(LocalDateTime.now());
        articleCategory.setUpdateTime(LocalDateTime.now());
        articleCategoryMapper.add(articleCategory);
    }

    /**
     * 分页查询文章分类
     * @param categoryPageQueryDTO
     * @return
     */
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        PageHelper.startPage(categoryPageQueryDTO.getPage(),categoryPageQueryDTO.getPageSize());
        Page<ArticleCategoryVO> page = articleCategoryMapper.pageQuery(categoryPageQueryDTO);
        long total = page.getTotal();
        List<ArticleCategoryVO> records = page.getResult();
        return new PageResult(total,records);
    }

    /**
     * 根据文章分类修改属性值
     * @param articleCategoryDTO
     */
    public void update(ArticleCategoryDTO articleCategoryDTO) {
        ArticleCategory articleCategory = new ArticleCategory();
        BeanUtils.copyProperties(articleCategoryDTO, articleCategory);
        articleCategory.setUpdateTime(LocalDateTime.now());
        articleCategoryMapper.update(articleCategory);
    }

    /**
     * 删除文章分类
     * @param id
     */
    public void delete(Long id) {
        articleCategoryMapper.delete(id);
    }
}
