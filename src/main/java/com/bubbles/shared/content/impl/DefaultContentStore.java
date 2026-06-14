package com.bubbles.shared.content.impl;

import com.bubbles.common.context.BaseContext;
import com.bubbles.modules.core.ScoredTopic;
import com.bubbles.pojo.dto.ArticleDTO;
import com.bubbles.pojo.entity.AiProcessRecord;
import com.bubbles.pojo.entity.ArticleCategory;
import com.bubbles.server.mapper.AiProcessRecordMapper;
import com.bubbles.server.mapper.ArticleCategoryMapper;
import com.bubbles.server.service.ArticleService;
import com.bubbles.shared.content.ContentStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 内容资产库默认实现 —— 统一管理所有平台生成的稿件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultContentStore implements ContentStore {

    private final ArticleService articleService;
    private final ArticleCategoryMapper articleCategoryMapper;
    private final AiProcessRecordMapper aiProcessRecordMapper;

    @Override
    @Transactional
    public Long saveArticle(ScoredTopic topic, String platform, String title,
                            String content, String coverUrl, String state) {
        var signal = topic.getSignal();

        // 查找或创建分类
        String categoryName = signal.getCategory();
        Long categoryId = articleCategoryMapper.findIdByName(categoryName);
        if (categoryId == null && categoryName != null && !categoryName.isEmpty()) {
            Long createUser = BaseContext.getCurrentId();
            if (createUser == null) {
                createUser = 1L;
            }
            ArticleCategory category = ArticleCategory.builder()
                    .categoryName(categoryName)
                    .categoryAlias(categoryName)
                    .createUser(createUser)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            articleCategoryMapper.add(category);
            categoryId = articleCategoryMapper.findIdByName(categoryName);
        }

        // 截断标题
        if (title != null && title.length() > 28) {
            title = title.substring(0, 28) + "...";
        }

        ArticleDTO dto = new ArticleDTO();
        dto.setTitle(title);
        dto.setContent(content);
        dto.setCoverImg(coverUrl != null ? coverUrl : "");
        dto.setCategoryId(categoryId);
        dto.setState(state != null ? state : "草稿");
        dto.setPlatform(platform);

        articleService.addArticle(dto);

        // 记录 AI 处理状态（使用 source + sourceId 替代 bvid）
        if (signal.getSourceId() != null) {
            AiProcessRecord record = AiProcessRecord.builder()
                    .bvid(signal.getSourceId()) // 向后兼容
                    .source(signal.getSource())
                    .sourceId(signal.getSourceId())
                    .articleId(null)
                    .processTime(LocalDateTime.now())
                    .status("generated")
                    .aiModel("ai-service")
                    .hotScore(BigDecimal.valueOf(topic.getScore()))
                    .build();
            aiProcessRecordMapper.insert(record);
        }

        log.info("[内容库] 文章已保存: title={}, platform={}, categoryId={}", title, platform, categoryId);
        return null; // MyBatis add 不返回自增ID
    }

    @Override
    public boolean isAlreadyProcessed(String sourceId, String platform) {
        return aiProcessRecordMapper.countBySourceId(platform, sourceId) > 0;
    }
}
