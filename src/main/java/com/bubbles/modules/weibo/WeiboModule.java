package com.bubbles.modules.weibo;

import com.bubbles.modules.core.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 微博平台模块 —— 占位实现
 *
 * 微博特点：
 * - 内容载体：头条文章（长文）
 * - 热点信号：微博热搜排名/搜索量（第三方 API / 爬虫）
 * - 发布方式：无公开API，需 OpenClaw 浏览器自动化
 * - 内容风格：800-1500字，短平快，注重情绪和时效性
 * - 账号风险：高（反自动化强），需模拟人类操作节奏
 * - 发布节奏：可 5-10 条/天（相比B站更频繁）
 *
 * TODO 阶段性实现计划:
 *   1. crawler - 微博热搜第三方API接入
 *   2. scorer - 微博评分策略 (热搜排名/搜索量权重高)
 *   3. strategy - 头条文章格式策略
 *   4. publisher - OpenClaw 浏览器自动化
 */
@Slf4j
@Component("weiboModule")
public class WeiboModule implements PlatformModule {

    @Override
    public String platformName() {
        return "weibo";
    }

    @Override
    public List<RawSignal> crawlHotTopics(int limit) {
        log.warn("[微博模块] 爬虫尚未实现，返回空列表");
        return List.of();
    }

    @Override
    public List<ScoredTopic> scoreTopics(List<RawSignal> signals) {
        log.warn("[微博模块] 评分引擎尚未实现");
        return List.of();
    }

    @Override
    public ContentStrategy getContentStrategy() {
        log.warn("[微博模块] 内容策略尚未实现");
        throw new UnsupportedOperationException("微博模块内容策略尚未实现");
    }

    @Override
    public PublishResult publish(ScoredTopic topic, String title, String content) {
        log.warn("[微博模块] 发布引擎尚未实现 (需 OpenClaw)");
        return PublishResult.builder()
                .success(false)
                .errorMessage("微博模块发布引擎尚未实现，需集成 OpenClaw 浏览器自动化")
                .build();
    }

    @Override
    public AccountStatus checkAccount() {
        return AccountStatus.builder()
                .platform("weibo")
                .healthy(false)
                .loggedIn(false)
                .message("微博模块尚未接入，需配置账号并集成 OpenClaw")
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
