package com.bubbles.modules.zhihu;

import com.bubbles.modules.core.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 知乎平台模块 —— 占位实现
 *
 * 知乎特点：
 * - 内容载体：回答/文章，强调观点和深度分析
 * - 热点信号：热榜关注数/浏览量（非官方API，需爬虫获取）
 * - 发布方式：无公开API，需 OpenClaw 浏览器自动化
 * - 内容风格：1500-3000字观点型分析，需有明确立场
 * - 账号风险：高（封号风险），需控制发布频率
 *
 * TODO 阶段性实现计划:
 *   1. crawler - 知乎热榜 HTTP 爬取 (有非官方 API 可参考)
 *   2. scorer - 知乎评分策略 (关注数/浏览量权重高)
 *   3. strategy - 回答格式策略 (观点+论据结构)
 *   4. publisher - OpenClaw 浏览器自动化发布
 */
@Slf4j
@Component("zhihuModule")
public class ZhihuModule implements PlatformModule {

    @Override
    public String platformName() {
        return "zhihu";
    }

    @Override
    public List<RawSignal> crawlHotTopics(int limit) {
        log.warn("[知乎模块] 爬虫尚未实现，返回空列表");
        return List.of();
    }

    @Override
    public List<ScoredTopic> scoreTopics(List<RawSignal> signals) {
        log.warn("[知乎模块] 评分引擎尚未实现");
        return List.of();
    }

    @Override
    public ContentStrategy getContentStrategy() {
        log.warn("[知乎模块] 内容策略尚未实现");
        throw new UnsupportedOperationException("知乎模块内容策略尚未实现");
    }

    @Override
    public PublishResult publish(ScoredTopic topic, String title, String content) {
        log.warn("[知乎模块] 发布引擎尚未实现 (需 OpenClaw)");
        return PublishResult.builder()
                .success(false)
                .errorMessage("知乎模块发布引擎尚未实现，需集成 OpenClaw 浏览器自动化")
                .build();
    }

    @Override
    public AccountStatus checkAccount() {
        return AccountStatus.builder()
                .platform("zhihu")
                .healthy(false)
                .loggedIn(false)
                .message("知乎模块尚未接入，需配置账号并集成 OpenClaw")
                .build();
    }

    @Override
    public Set<Capability> capabilities() {
        return EnumSet.noneOf(Capability.class); // 尚未实现任何能力
    }

    @Override
    public boolean isEnabled() {
        return false; // 默认关闭
    }
}
