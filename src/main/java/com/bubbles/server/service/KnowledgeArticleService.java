package com.bubbles.server.service;

import com.bubbles.common.result.PageResult;
import com.bubbles.pojo.dto.KnowledgeArticleDTO;
import com.bubbles.pojo.dto.KnowledgePageQueryDTO;
import com.bubbles.pojo.vo.KnowledgeArticleVO;

import java.util.List;

public interface KnowledgeArticleService {

    void add(KnowledgeArticleDTO dto);

    PageResult pageQuery(KnowledgePageQueryDTO query);

    KnowledgeArticleVO getById(Long id);

    void update(KnowledgeArticleDTO dto);

    void delete(Long id);

    void toggleStatus(Long id);

    /** 供 Python 检索端使用：获取所有启用的知识库文章 */
    List<KnowledgeArticleVO> listAllEnabled();

    KnowledgeArticleVO getEnabledById(Long id);
}
