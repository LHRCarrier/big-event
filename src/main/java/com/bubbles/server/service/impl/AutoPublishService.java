package com.bubbles.server.service.impl;

import com.bubbles.common.context.BaseContext;
import com.bubbles.common.properties.AiWriterProperties;
import com.bubbles.pojo.dto.ArticleDTO;
import com.bubbles.pojo.dto.WriteFromHotRequestDTO;
import com.bubbles.pojo.dto.WriterResponseDTO;
import com.bubbles.pojo.entity.AiProcessRecord;
import com.bubbles.pojo.entity.Article;
import com.bubbles.pojo.vo.HotTopicVO;
import com.bubbles.server.mapper.AiProcessRecordMapper;
import com.bubbles.server.mapper.ArticleCategoryMapper;
import com.bubbles.server.mapper.ArticleMapper;
import com.bubbles.server.service.ArticleService;
import com.bubbles.server.service.WriterAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AutoPublishService {

    private final HotScoreCalculator scoreCalculator;
    private final WriterAIService writerAIService;
    private final ArticleService articleService;
    private final ArticleCategoryMapper articleCategoryMapper;
    private final AiProcessRecordMapper aiProcessRecordMapper;
    private final AiWriterProperties aiWriterProperties;

    /**
     * 自动发布主流程
     * 评分 → 筛选 → AI撰稿 → 创建文章 → 记录处理状态
     *
     * @return 生成的文章列表（均为草稿状态）
     */
    @Transactional
    public List<Article> autoPublish() {
        var ap = aiWriterProperties.getAutoPublish();
        return autoPublish(ap.getTopN(), ap.getPartition(), ap.getMinScore());
    }

    /**
     * 手动触发自动发布（可指定参数）
     */
    @Transactional
    public List<Article> autoPublish(int topN, String partition, int minScore) {
        if (!aiWriterProperties.getAutoPublish().isEnabled()) {
            log.info("[自动发布] 自动发布已禁用，跳过");
            return List.of();
        }

        log.info("[自动发布] 开始自动发布流程: topN={}, partition={}, minScore={}", topN, partition, minScore);

        // 1. 评分筛选：获取 Top N 热点
        List<HotTopicVO> topics = scoreCalculator.getTopTopics(topN * 2, partition);
        if (topics.isEmpty()) {
            log.info("[自动发布] 没有热点话题，跳过");
            return List.of();
        }

        // 2. 设置系统用户上下文
        Long systemUserId = aiWriterProperties.getSystemUserId();
        BaseContext.setCurrentId(systemUserId);

        List<Article> results = new ArrayList<>();
        try {
            int published = 0;
            for (HotTopicVO topic : topics) {
                if (published >= topN) break;

                // 去重
                if (topic.isAlreadyProcessed()) {
                    log.debug("[自动发布] bvid={} 已处理过，跳过", topic.getBvid());
                    continue;
                }

                // 评分过低
                if (topic.getScore() < minScore) {
                    log.debug("[自动发布] bvid={} 评分{}低于阈值{}, 跳过",
                            topic.getBvid(), topic.getScore(), minScore);
                    continue;
                }

                // 3. 调用 AI 热点撰稿
                log.info("[自动发布] 开始为热点撰稿: {} (评分: {})", topic.getTitle(), topic.getScore());
                WriteFromHotRequestDTO writeReq = buildWriteRequest(topic);
                WriterResponseDTO writeResp = writerAIService.writeFromHot(writeReq);

                if (writeResp == null) {
                    log.warn("[自动发布] AI撰稿返回null, bvid={}", topic.getBvid());
                    continue;
                }

                // 4. 创建 Article（状态为"草稿"）
                Long categoryId = articleCategoryMapper.findIdByName(topic.getPartitionTag());
                ArticleDTO articleDTO = new ArticleDTO();
                articleDTO.setTitle(writeResp.getTitle());
                articleDTO.setContent(writeResp.getContent());
                articleDTO.setCoverImg(topic.getCoverUrl() != null ? topic.getCoverUrl() : "");
                articleDTO.setCategoryId(categoryId);
                articleDTO.setState("草稿");

                articleService.addArticle(articleDTO);

                // 获取刚插入的文章ID（通过查询最新文章来获取，或者修改 addArticle 返回ID）
                // 这里从 articleService 无法直接获取ID，改用最近创建的 Article。
                // 由于 MyBatis 的 add 不会返回自增ID，我们记录处理状态即可。
                // 简化处理: article_id 暂时设为 null，后续可通过标题和时间关联。

                // 5. 记录处理状态
                AiProcessRecord record = AiProcessRecord.builder()
                        .bvid(topic.getBvid())
                        .articleId(null) // MyBatis add 不返回自增ID，暂时留空
                        .processTime(LocalDateTime.now())
                        .status("generated")
                        .aiModel(writeResp.getModelUsed())
                        .hotScore(BigDecimal.valueOf(topic.getScore()))
                        .build();
                aiProcessRecordMapper.insert(record);

                // 构建返回对象
                Article article = Article.builder()
                        .title(writeResp.getTitle())
                        .content(writeResp.getContent())
                        .coverImg(topic.getCoverUrl())
                        .state("草稿")
                        .categoryId(categoryId)
                        .createUser(systemUserId)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .build();
                results.add(article);

                published++;
                log.info("[自动发布] 热点撰稿完成: {} → 文章《{}》", topic.getBvid(), writeResp.getTitle());
            }

            log.info("[自动发布] 本次共生成 {} 篇文章", results.size());

        } finally {
            BaseContext.removeCurrentId();
        }

        return results;
    }

    private WriteFromHotRequestDTO buildWriteRequest(HotTopicVO topic) {
        var ap = aiWriterProperties.getAutoPublish();
        return WriteFromHotRequestDTO.builder()
                .title(topic.getTitle())
                .partition(topic.getPartitionTag())
                .category(topic.getTname())
                .author(topic.getAuthor())
                .viewCount(topic.getViewCount())
                .likeCount(topic.getLikeCount())
                .coinCount(topic.getCoinCount())
                .favoriteCount(topic.getFavoriteCount())
                .shareCount(topic.getShareCount())
                .danmakuCount(topic.getDanmakuCount())
                .replyCount(topic.getReplyCount())
                .hotScore(topic.getScore())
                .rank(0) // rank 从算法层面已经不重要了，评分已考虑
                .length(800)
                .style("neutral")
                .audience("general")
                .generateSummary(true)
                .bvid(topic.getBvid())
                .description(topic.getDescription())
                .build();
    }
}
