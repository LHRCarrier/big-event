package com.bubbles.shared.ai.impl;

import com.bubbles.modules.core.ContentStrategy;
import com.bubbles.modules.core.ScoredTopic;
import com.bubbles.pojo.dto.WriteFromHotRequestDTO;
import com.bubbles.pojo.dto.WriterResponseDTO;
import com.bubbles.server.service.WriterAIService;
import com.bubbles.shared.ai.AIWriterEngine;
import com.bubbles.shared.ai.WriteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * AI 撰稿引擎默认实现 —— 通过 REST API 调用 Python AI 服务
 *
 * 包装现有的 WriterAIService，将模块化接口（ContentStrategy + ScoredTopic）
 * 适配到现有的 HTTP 调用层。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAIWriterEngine implements AIWriterEngine {

    private final WriterAIService writerAIService;

    @Override
    public WriteResult write(ScoredTopic topic, ContentStrategy strategy) {
        var signal = topic.getSignal();
        var metrics = signal.getRawMetrics();

        // 将 ScoredTopic + ContentStrategy 转换为 WriteFromHotRequestDTO
        WriteFromHotRequestDTO request = WriteFromHotRequestDTO.builder()
                .title(signal.getTitle())
                .partition(signal.getCategory())
                .category(signal.getCategory())
                .author(signal.getAuthor())
                .bvid(signal.getSourceId())
                .coverUrl(signal.getCoverUrl())
                .viewCount(getLong(metrics, "viewCount"))
                .likeCount(getLong(metrics, "likeCount"))
                .coinCount(getLong(metrics, "coinCount"))
                .favoriteCount(getLong(metrics, "favoriteCount"))
                .shareCount(getLong(metrics, "shareCount"))
                .danmakuCount(getLong(metrics, "danmakuCount"))
                .replyCount(getLong(metrics, "replyCount"))
                .hotScore(topic.getScore())
                .rank(signal.getRank())
                .length(strategy.maxLength())
                .style("neutral")
                .audience("general")
                .generateSummary(true)
                .build();

        log.info("[AI引擎] 开始撰稿, topic={}, strategy={}, length={}",
                signal.getTitle(), strategy.platformName(), strategy.maxLength());

        WriterResponseDTO response = writerAIService.writeFromHot(request);

        if (response == null) {
            return WriteResult.builder()
                    .articleId("error-" + System.currentTimeMillis())
                    .title(signal.getTitle())
                    .content("AI 撰稿失败，请稍后重试")
                    .summary("")
                    .modelUsed("unknown")
                    .generatedAt(LocalDateTime.now())
                    .build();
        }

        log.info("[AI引擎] 撰稿完成, articleId={}, title={}", response.getArticleId(), response.getTitle());

        return WriteResult.builder()
                .articleId(response.getArticleId())
                .title(response.getTitle())
                .content(response.getContent())
                .summary(response.getSummary())
                .modelUsed(response.getModelUsed())
                .generatedAt(response.getGeneratedAt())
                .build();
    }

    @Override
    public String generateSummary(String content, int maxLength) {
        if (content == null || content.isEmpty()) return "";
        String summary = content.length() > maxLength ? content.substring(0, maxLength) + "..." : content;
        return summary.replaceAll("[#*>`\\r\\n]+", " ").trim();
    }

    @Override
    public boolean isAvailable() {
        return writerAIService.isServiceAvailable();
    }

    private long getLong(java.util.Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.longValue();
        return 0L;
    }
}
