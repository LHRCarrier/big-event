package com.bubbles.server.service.impl;

import com.bubbles.common.context.BaseContext;
import com.bubbles.common.result.PageResult;
import com.bubbles.pojo.dto.ArticleDTO;
import com.bubbles.pojo.dto.ArticlePageQueryDTO;
import com.bubbles.pojo.entity.Article;
import com.bubbles.pojo.vo.ArticleVO;
import com.bubbles.server.mapper.ArticleMapper;
import com.bubbles.server.service.ArticleService;
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
public class ArticleServiceImpl implements ArticleService {
    @Autowired
    private ArticleMapper articleMapper;
    /**
     * 新增文章
     * @param articleDTO
     */
    public void addArticle(ArticleDTO articleDTO) {
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

    /**
     * 文章列表分页查询
     * @param articlePageQueryDTO
     * @return
     */
    public PageResult pageQuery(ArticlePageQueryDTO articlePageQueryDTO) {
        PageHelper.startPage(articlePageQueryDTO.getPage(),articlePageQueryDTO.getPageSize());
        Page<ArticleVO> page = articleMapper.page(articlePageQueryDTO);
        long total = page.getTotal();
        List<ArticleVO> records = page.getResult();
        return new PageResult(total,records);
    }

    /**
     * 根据文章id获取详情
     * @param id
     * @return
     */
    public Article detail(Long id) {
        return articleMapper.getById(id);
    }

    /**
     * 修改文章信息
     * @param articleDTO
     */
    public void update(ArticleDTO articleDTO) {
        Article article = new Article();
        BeanUtils.copyProperties(articleDTO,article);
        article.setUpdateTime(LocalDateTime.now());
        articleMapper.update(article);
    }

    /**
     * 根据id删除文章
     * @param id
     */
    public void delete(Long id) {
        articleMapper.delete(id);
    }
}
