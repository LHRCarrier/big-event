package com.bubbles.modules.bilibili.publisher;

import com.bubbles.modules.core.AccountStatus;
import com.bubbles.modules.core.PublishResult;
import com.bubbles.modules.core.ScoredTopic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * B站发布器 —— 发布文章到 B站专栏
 *
 * 当前状态：占位实现。B站专栏发布 API 状态待确认（B站创作中心是否有公开API）。
 * 如果B站专栏有创作者 API，则直接调用 API；
 * 如果没有，后续引入 OpenClaw 浏览器自动化作为 fallback。
 */
@Slf4j
@Component
public class BilibiliPublisher {

    /**
     * 发布文章到 B站专栏
     *
     * 当前为占位实现，日志记录发布意图。
     * TODO: 接入B站创作中心API 或 OpenClaw 浏览器自动化
     */
    public PublishResult publish(ScoredTopic topic, String title, String content) {
        log.info("[B站发布] 准备发布专栏: title={}, sourceId={}, 内容长度={}",
                title, topic.getSignal().getSourceId(), content.length());

        // TODO: 实际的 B站专栏发布逻辑
        // 方案A: B站创作中心 API (需确认可用性)
        // 方案B: OpenClaw 浏览器自动化

        log.warn("[B站发布] B站专栏发布API尚未接入，当前为占位模式");

        return PublishResult.builder()
                .success(false)
                .errorMessage("B站专栏发布功能尚未接入，等待 B站创作中心 API 或 OpenClaw 集成")
                .publishedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 检查 B站账号状态
     */
    public AccountStatus checkAccount() {
        // TODO: 实际检查 B站账号登录态、发布配额
        return AccountStatus.builder()
                .platform("bilibili")
                .healthy(true)
                .loggedIn(false)
                .rateLimited(false)
                .publishedToday(0)
                .dailyLimit(2)
                .lastCheckedAt(LocalDateTime.now())
                .message("B站发布器为占位模式，尚未连接真实账号")
                .build();
    }
}
