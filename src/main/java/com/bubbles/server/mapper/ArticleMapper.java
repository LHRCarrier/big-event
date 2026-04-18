package com.bubbles.server.mapper;

import com.bubbles.pojo.dto.ArticlePageQueryDTO;
import com.bubbles.pojo.entity.Article;
import com.bubbles.pojo.vo.ArticleVO;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArticleMapper {
    /**
     * 添加文章
     * @param article
     */
    void add(Article article);

    /**
     * 条件分页查询问文章列表
     * @param articlePageQueryDTO
     * @return
     */
    Page<ArticleVO> page(ArticlePageQueryDTO articlePageQueryDTO);

    /**
     * 根据id获取文章详情
     * @param id
     * @return
     */
    @Select("select * from article where id = #{id}")
    Article getById(Long id);

    /**
     * 更新文章信息
     * @param article
     */
    void update(Article article);

    /**
     * 根据id删除文章
     * @param id
     */
    @Delete("delete from article where id =#{id}")
    void delete(Long id);
}
