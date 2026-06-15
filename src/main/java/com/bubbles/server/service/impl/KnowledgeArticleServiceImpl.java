package com.bubbles.server.service.impl;

import com.bubbles.common.result.PageResult;
import com.bubbles.pojo.dto.KnowledgeArticleDTO;
import com.bubbles.pojo.dto.KnowledgePageQueryDTO;
import com.bubbles.pojo.entity.KnowledgeArticle;
import com.bubbles.pojo.vo.KnowledgeArticleVO;
import com.bubbles.server.mapper.KnowledgeArticleMapper;
import com.bubbles.server.service.KnowledgeArticleService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class KnowledgeArticleServiceImpl implements KnowledgeArticleService {

    @Autowired
    private KnowledgeArticleMapper knowledgeArticleMapper;

    @Override
    public void add(KnowledgeArticleDTO dto) {
        KnowledgeArticle article = new KnowledgeArticle();
        BeanUtils.copyProperties(dto, article);
        if (article.getStatus() == null) {
            article.setStatus(1);
        }
        if (article.getQuality() == null) {
            article.setQuality(3);
        }
        if (article.getWordCount() == null) {
            article.setWordCount(dto.getContent() != null ? dto.getContent().length() : 0);
        }
        knowledgeArticleMapper.add(article);
        log.info("知识库文章已添加: id={}, title={}", article.getId(), article.getTitle());
    }

    @Override
    public PageResult pageQuery(KnowledgePageQueryDTO query) {
        PageHelper.startPage(query.getPage(), query.getPageSize());
        Page<KnowledgeArticleVO> page = knowledgeArticleMapper.pageQuery(query);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public KnowledgeArticleVO getById(Long id) {
        return knowledgeArticleMapper.getById(id);
    }

    @Override
    public void update(KnowledgeArticleDTO dto) {
        KnowledgeArticle article = new KnowledgeArticle();
        BeanUtils.copyProperties(dto, article);
        if (dto.getContent() != null) {
            article.setWordCount(dto.getContent().length());
        }
        knowledgeArticleMapper.update(article);
        log.info("知识库文章已更新: id={}", dto.getId());
    }

    @Override
    public void delete(Long id) {
        knowledgeArticleMapper.delete(id);
        log.info("知识库文章已删除: id={}", id);
    }

    @Override
    public void toggleStatus(Long id) {
        KnowledgeArticleVO vo = knowledgeArticleMapper.getById(id);
        int newStatus = vo.getStatus() == 1 ? 0 : 1;
        knowledgeArticleMapper.updateStatus(id, newStatus);
        log.info("知识库文章状态已切换: id={}, status={}", id, newStatus);
    }

    @Override
    public List<KnowledgeArticleVO> listAllEnabled() {
        return knowledgeArticleMapper.listAllEnabled();
    }

    @Override
    public KnowledgeArticleVO getEnabledById(Long id) {
        return knowledgeArticleMapper.getEnabledById(id);
    }
}
