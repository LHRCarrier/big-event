package com.bubbles.server.service.impl;

import com.bubbles.common.context.BaseContext;
import com.bubbles.pojo.dto.ArticleCategoryDTO;
import com.bubbles.pojo.dto.ArticleDTO;
import com.bubbles.pojo.entity.Article;
import com.bubbles.pojo.entity.Category;
import com.bubbles.server.mapper.ArticleMapper;
import com.bubbles.server.service.ArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class ArticleServiceImpl implements ArticleService {
    @Autowired
    private ArticleMapper articleMapper;
    /**
     * 新增文章
     * @param articleDTO
     */
    public void addArticle(ArticleDTO articleDTO) {
        log.info("新增文章，articleDTO: {}", articleDTO);
        Long currentId = BaseContext.getCurrentId();
        log.info("当前用户 ID: {}", currentId);
        Article article = new Article();
        BeanUtils.copyProperties(articleDTO,article);
        article.setCreateUser(currentId);
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        log.info("准备插入的文章对象: {}", article);
        articleMapper.add(article);
        log.info("文章插入成功");
    }

}
