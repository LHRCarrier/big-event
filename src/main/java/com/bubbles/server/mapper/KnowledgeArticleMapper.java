package com.bubbles.server.mapper;

import com.bubbles.pojo.dto.KnowledgePageQueryDTO;
import com.bubbles.pojo.entity.KnowledgeArticle;
import com.bubbles.pojo.vo.KnowledgeArticleVO;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface KnowledgeArticleMapper {

    void add(KnowledgeArticle article);

    Page<KnowledgeArticleVO> pageQuery(KnowledgePageQueryDTO query);

    @Select("SELECT * FROM knowledge_article WHERE id = #{id}")
    KnowledgeArticleVO getById(Long id);

    void update(KnowledgeArticle article);

    @Delete("DELETE FROM knowledge_article WHERE id = #{id}")
    void delete(Long id);

    @Update("UPDATE knowledge_article SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    void updateStatus(Long id, Integer status);

    /** 供 Python 检索端使用：获取所有启用的知识库文章（仅返回基础字段，不含正文全文） */
    @Select("SELECT id, title, excerpt, category, tags, quality, word_count FROM knowledge_article WHERE status = 1")
    List<KnowledgeArticleVO> listAllEnabled();

    @Select("SELECT * FROM knowledge_article WHERE status = 1 AND id = #{id}")
    KnowledgeArticleVO getEnabledById(Long id);
}
