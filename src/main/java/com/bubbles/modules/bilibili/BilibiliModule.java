package com.bubbles.modules.bilibili;

import com.bubbles.modules.bilibili.crawler.BilibiliCrawler;
import com.bubbles.modules.bilibili.publisher.BilibiliPublisher;
import com.bubbles.modules.bilibili.scorer.BilibiliScorer;
import com.bubbles.modules.bilibili.strategy.BilibiliContentStrategy;
import com.bubbles.modules.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * B站平台模块 —— 完整的 "爬取→评分→生成→发布" 闭环
 *
 * 这是平台化架构的第一个完整实现模块，用于验证模块化架构可行性。
 */
@Slf4j
@Component("bilibiliModule")
@RequiredArgsConstructor
public class BilibiliModule implements PlatformModule {

    private final BilibiliCrawler crawler;
    private final BilibiliScorer scorer;
    private final BilibiliContentStrategy contentStrategy;
    private final BilibiliPublisher publisher;

    @Override
    public String platformName() {
        return "bilibili";
    }

    @Override
    public List<RawSignal> crawlHotTopics(int limit) {
        log.info("[B站模块] 开始采集热点, limit={}", limit);
        return crawler.fetchHotTopics(limit);
    }

    @Override
    public List<ScoredTopic> scoreTopics(List<RawSignal> signals) {
        log.info("[B站模块] 开始评分, 信号数={}", signals.size());
        return scorer.score(signals);
    }

    @Override
    public ContentStrategy getContentStrategy() {
        return contentStrategy;
    }

    @Override
    public PublishResult publish(ScoredTopic topic, String title, String content) {
        log.info("[B站模块] 发布文章: title={}", title);
        return publisher.publish(topic, title, content);
    }

    @Override
    public AccountStatus checkAccount() {
        return publisher.checkAccount();
    }

    @Override
    public Set<Capability> capabilities() {
        return EnumSet.of(
                Capability.CRAWL,
                Capability.SCORE,
                Capability.GENERATE,
                Capability.PUBLISH
        );
    }
}
