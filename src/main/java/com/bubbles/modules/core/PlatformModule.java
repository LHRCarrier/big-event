package com.bubbles.modules.core;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 平台模块标准接口
 *
 * 每个平台模块实现相同接口，但内部实现完全独立。
 * 初期在单体应用中作为独立 package，后续可按需拆分为微服务。
 */
public interface PlatformModule {

    /** 模块标识 */
    String platformName();

    /** 热点话题采集 */
    List<RawSignal> crawlHotTopics(int limit);

    /** 平台特定的评分计算 */
    List<ScoredTopic> scoreTopics(List<RawSignal> signals);

    /** 内容生成策略 */
    ContentStrategy getContentStrategy();

    /** 发布内容到平台 */
    PublishResult publish(ScoredTopic topic, String title, String content);

    /** 检查账号状态 */
    default AccountStatus checkAccount() {
        return AccountStatus.builder()
                .platform(platformName())
                .healthy(true)
                .loggedIn(false)
                .message("账号状态检查尚未实现")
                .build();
    }

    /** 平台支持的操作类型 */
    default Set<Capability> capabilities() {
        return Collections.singleton(Capability.CRAWL);
    }

    /** 模块是否启用 */
    default boolean isEnabled() {
        return true;
    }
}
