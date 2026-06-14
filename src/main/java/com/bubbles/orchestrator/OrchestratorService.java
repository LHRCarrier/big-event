package com.bubbles.orchestrator;

import com.bubbles.common.properties.AiWriterProperties;
import com.bubbles.modules.core.*;
import com.bubbles.shared.ai.AIWriterEngine;
import com.bubbles.shared.ai.WriteResult;
import com.bubbles.shared.content.ContentStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 统一调度器 —— 跨平台的任务调度与编排
 *
 * 负责:
 * 1. 定时触发各平台模块的热点采集
 * 2. 走 "爬取→评分→生成→发布" 完整流水线
 * 3. 全局开关控制（AI配置中的 enabled 标志）
 * 4. 故障隔离：单平台失败不影响其他平台
 *
 * 当前仅 B站模块有完整实现，其他模块在 isEnabled()=false 时自动跳过。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestratorService {

    private final ModuleRegistry registry;
    private final AIWriterEngine aiWriterEngine;
    private final ContentStore contentStore;
    private final AiWriterProperties aiWriterProperties;

    /**
     * 定时调度主入口
     *
     * 按配置的间隔（默认30分钟），遍历所有已启用模块：
     * 采集热点 → 评分筛选 → AI撰稿 → 保存到内容库 → 发布到平台
     */
    @Scheduled(fixedDelayString = "#{${bubbles.ai-writer.snapshot.sync-interval-min:30} * 60 * 1000}")
    public void scheduledRun() {
        if (!aiWriterProperties.getAutoPublish().isEnabled()) {
            log.debug("[调度器] 自动发布已禁用，跳过定时调度");
            return;
        }

        log.info("[调度器] ===== 开始定时调度 =====");
        List<PlatformModule> modules = registry.getEnabledModules();
        log.info("[调度器] 已启用模块: {}", modules.stream().map(PlatformModule::platformName).toList());

        for (PlatformModule module : modules) {
            try {
                runPipeline(module);
            } catch (Exception e) {
                log.error("[调度器] 模块 {} 执行失败（已隔离）: {}", module.platformName(), e.getMessage(), e);
            }
        }

        log.info("[调度器] ===== 定时调度完成 =====");
    }

    /**
     * 手动触发指定平台的完整流水线
     */
    public PipelineResult runPipelineForPlatform(String platformName, int limit, int topN) {
        var module = registry.getModule(platformName)
                .orElseThrow(() -> new IllegalArgumentException("未知平台: " + platformName));

        if (!module.isEnabled()) {
            return PipelineResult.builder()
                    .platform(platformName)
                    .success(false)
                    .message("平台模块未启用")
                    .build();
        }

        return runPipeline(module, limit, topN);
    }

    /**
     * 执行指定模块的完整流水线
     */
    private PipelineResult runPipeline(PlatformModule module) {
        var ap = aiWriterProperties.getAutoPublish();
        return runPipeline(module, 50, ap.getTopN());
    }

    private PipelineResult runPipeline(PlatformModule module, int limit, int topN) {
        String platform = module.platformName();
        int generated = 0;
        int published = 0;

        try {
            // 阶段1: 爬取热点
            log.info("[调度器|{}] 阶段1: 爬取热点, limit={}", platform, limit);
            List<RawSignal> signals = module.crawlHotTopics(limit);
            log.info("[调度器|{}] 爬取到 {} 条信号", platform, signals.size());

            if (signals.isEmpty()) {
                return PipelineResult.builder().platform(platform).success(true).message("无新信号").build();
            }

            // 阶段2: 评分筛选
            log.info("[调度器|{}] 阶段2: 评分筛选", platform);
            List<ScoredTopic> topics = module.scoreTopics(signals);
            int minScore = aiWriterProperties.getAutoPublish().getMinScore();
            List<ScoredTopic> qualified = topics.stream()
                    .filter(t -> t.getScore() >= minScore && !t.isAlreadyProcessed())
                    .limit(topN)
                    .toList();
            log.info("[调度器|{}] 评分后 {} 条合格, 选取 Top {}", platform, qualified.size(), qualified.size());

            // 阶段3: AI 撰稿
            ContentStrategy strategy = module.getContentStrategy();
            for (ScoredTopic topic : qualified) {
                try {
                    log.info("[调度器|{}] 阶段3: AI撰稿, title={}", platform, topic.getSignal().getTitle());
                    WriteResult result = aiWriterEngine.write(topic, strategy);

                    // 阶段4: 保存到内容库
                    log.info("[调度器|{}] 阶段4: 保存文章", platform);
                    Long articleId = contentStore.saveArticle(topic, platform,
                            result.getTitle(), result.getContent(),
                            topic.getSignal().getCoverUrl(), "草稿");
                    generated++;

                    // 阶段5: 发布到平台（如果有 PUBLISH 能力）
                    if (module.capabilities().contains(Capability.PUBLISH)) {
                        log.info("[调度器|{}] 阶段5: 发布到平台", platform);
                        PublishResult pubResult = module.publish(topic, result.getTitle(), result.getContent());
                        if (pubResult.isSuccess()) {
                            published++;
                        }
                    }

                } catch (Exception e) {
                    log.error("[调度器|{}] 撰稿/发布失败: {}", platform, e.getMessage());
                }
            }

            log.info("[调度器|{}] 流水线完成: 生成 {} 篇, 发布 {} 篇", platform, generated, published);

            return PipelineResult.builder()
                    .platform(platform)
                    .success(true)
                    .signalsCollected(signals.size())
                    .topicsScored(topics.size())
                    .articlesGenerated(generated)
                    .articlesPublished(published)
                    .message(String.format("生成 %d 篇, 发布 %d 篇", generated, published))
                    .build();

        } catch (Exception e) {
            log.error("[调度器|{}] 流水线执行失败: {}", platform, e.getMessage(), e);
            return PipelineResult.builder()
                    .platform(platform)
                    .success(false)
                    .message(e.getMessage())
                    .build();
        }
    }
}
