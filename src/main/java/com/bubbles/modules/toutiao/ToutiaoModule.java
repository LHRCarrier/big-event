package com.bubbles.modules.toutiao;

import com.bubbles.modules.core.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 今日头条平台模块 —— 占位实现
 *
 * 头条特点：
 * - 内容载体：头条号文章
 * - 热点信号：头条号后台推荐量/阅读数据
 * - 发布方式：头条号 API（有官方内容管理 API）
 * - 内容风格：1000-2000字，标题党风格，快消型内容
 * - 账号风险：低（有官方API），适合自动化
 * - 反自动化：弱（官方支持API发布）
 *
 * TODO 阶段性实现计划:
 *   1. crawler - 头条号后台数据 API
 *   2. scorer - 头条评分策略 (推荐量/阅读量权重高)
 *   3. strategy - 头条号文章格式策略
 *   4. publisher - 头条号内容管理 API
 */
@Slf4j
@Component("toutiaoModule")
public class ToutiaoModule implements PlatformModule {

    @Override
    public String platformName() {
        return "toutiao";
    }

    @Override
    public List<RawSignal> crawlHotTopics(int limit) {
        log.warn("[头条模块] 爬虫尚未实现，返回空列表");
        return List.of();
    }

    @Override
    public List<ScoredTopic> scoreTopics(List<RawSignal> signals) {
        log.warn("[头条模块] 评分引擎尚未实现");
        return List.of();
    }

    @Override
    public ContentStrategy getContentStrategy() {
        log.warn("[头条模块] 内容策略尚未实现");
        throw new UnsupportedOperationException("头条模块内容策略尚未实现");
    }

    @Override
    public PublishResult publish(ScoredTopic topic, String title, String content) {
        log.warn("[头条模块] 发布引擎尚未实现 (需接入头条号API)");
        return PublishResult.builder()
                .success(false)
                .errorMessage("头条模块发布引擎尚未实现，需接入头条号内容管理 API")
                .build();
    }

    @Override
    public AccountStatus checkAccount() {
        return AccountStatus.builder()
                .platform("toutiao")
                .healthy(false)
                .loggedIn(false)
                .message("头条模块尚未接入，需配置头条号开发者账号")
                .build();
    }

    @Override
    public Set<Capability> capabilities() {
        return EnumSet.noneOf(Capability.class);
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
