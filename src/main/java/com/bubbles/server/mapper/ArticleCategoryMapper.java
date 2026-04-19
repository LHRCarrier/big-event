package com.bubbles.server.mapper;

import com.bubbles.pojo.dto.ArticleCategoryDTO;
import com.bubbles.pojo.dto.CategoryPageQueryDTO;
import com.bubbles.pojo.entity.ArticleCategory;
import com.bubbles.pojo.vo.ArticleCategoryVO;
import com.bubbles.pojo.vo.ArticleVO;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArticleCategoryMapper {
    /**
     * 新增文章分类
     * @param articleCategory
     */
    void add(ArticleCategory articleCategory);

    /**
     * 文章分类分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    Page<ArticleCategoryVO> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 修改文章分类属性
     * @param articleCategory
     */
    void update(ArticleCategory articleCategory);

    /**
     * 根据id删除文章分类
     * @param id
     */
    @Delete("delete from category where id = #{id}")
    void delete(Long id);
}
