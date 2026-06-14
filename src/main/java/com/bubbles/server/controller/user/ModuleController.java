package com.bubbles.server.controller.user;

import com.bubbles.common.result.Result;
import com.bubbles.modules.core.AccountStatus;
import com.bubbles.modules.core.PlatformModule;
import com.bubbles.modules.core.RawSignal;
import com.bubbles.modules.core.ScoredTopic;
import com.bubbles.orchestrator.ModuleRegistry;
import com.bubbles.orchestrator.OrchestratorService;
import com.bubbles.orchestrator.PipelineResult;
import com.bubbles.shared.ai.AIWriterEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 模块化架构 API 控制器
 *
 * 提供平台模块注册状态、手动触发流水线、账号状态等接口。
 * 与现有 WriterController 并存，提供更细粒度的模块级控制。
 */
@Slf4j
@RestController
@RequestMapping("/user/modules")
@RequiredArgsConstructor
@Tag(name = "模块管理", description = "平台模块注册、调度与状态接口")
public class ModuleController {

    private final ModuleRegistry registry;
    private final OrchestratorService orchestrator;
    private final AIWriterEngine aiWriterEngine;

    /**
     * 获取所有平台模块的注册状态
     */
    @GetMapping("/status")
    @Operation(summary = "模块注册状态", description = "查看所有平台模块的注册、启停、能力信息")
    public Result<Map<String, Object>> getModuleStatus() {
        List<Map<String, Object>> modules = registry.getAllModules().stream()
                .map(m -> {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("platform", m.platformName());
                    info.put("enabled", m.isEnabled());
                    info.put("capabilities", m.capabilities().stream()
                            .map(Enum::name).collect(Collectors.toList()));
                    try {
                        info.put("account", m.checkAccount());
                    } catch (Exception e) {
                        info.put("account", Map.of("healthy", false, "error", e.getMessage()));
                    }
                    return info;
                }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalModules", modules.size());
        result.put("enabledModules", modules.stream().filter(m -> (boolean) m.get("enabled")).count());
        result.put("aiServiceAvailable", aiWriterEngine.isAvailable());
        result.put("modules", modules);

        return Result.success(result);
    }

    /**
     * 获取所有平台账号状态
     */
    @GetMapping("/accounts")
    @Operation(summary = "账号状态", description = "获取所有平台账号的健康状态")
    public Result<Map<String, AccountStatus>> getAccountStatus() {
        return Result.success(registry.getAllAccountStatus());
    }

    /**
     * 手动触发指定平台的完整流水线
     */
    @PostMapping("/{platform}/pipeline")
    @Operation(summary = "触发流水线", description = "手动触发指定平台的 采集→评分→撰稿→发布 完整流水线")
    public Result<PipelineResult> runPipeline(
            @PathVariable String platform,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "5") int topN) {
        log.info("手动触发流水线: platform={}, limit={}, topN={}", platform, limit, topN);
        PipelineResult result = orchestrator.runPipelineForPlatform(platform, limit, topN);
        return result.isSuccess() ? Result.success(result) : Result.error(result.getMessage());
    }

    /**
     * 触发指定平台的热点采集（不后续处理）
     */
    @PostMapping("/{platform}/crawl")
    @Operation(summary = "采集热点", description = "仅触发指定平台的热点采集，不进行撰稿和发布")
    public Result<List<RawSignal>> crawlHotTopics(
            @PathVariable String platform,
            @RequestParam(defaultValue = "50") int limit) {
        var module = registry.getModule(platform)
                .orElseThrow(() -> new IllegalArgumentException("未知平台: " + platform));
        if (!module.isEnabled()) {
            return Result.error("平台模块未启用: " + platform);
        }
        List<RawSignal> signals = module.crawlHotTopics(limit);
        return Result.success(signals);
    }

    /**
     * 对指定信号列表进行评分（调试用）
     */
    @PostMapping("/{platform}/score")
    @Operation(summary = "评分", description = "对指定平台的信号列表进行评分（调试接口）")
    public Result<List<ScoredTopic>> scoreTopics(
            @PathVariable String platform,
            @RequestBody List<RawSignal> signals) {
        var module = registry.getModule(platform)
                .orElseThrow(() -> new IllegalArgumentException("未知平台: " + platform));
        if (!module.isEnabled()) {
            return Result.error("平台模块未启用: " + platform);
        }
        List<ScoredTopic> topics = module.scoreTopics(signals);
        return Result.success(topics);
    }

    /**
     * 获取所有已注册模块的清单（简化版）
     */
    @GetMapping
    @Operation(summary = "模块清单", description = "获取所有已注册平台模块的清单")
    public Result<List<Map<String, Object>>> listModules() {
        List<Map<String, Object>> list = registry.getAllModules().stream()
                .map(m -> {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("platform", m.platformName());
                    info.put("enabled", m.isEnabled());
                    info.put("capabilities", m.capabilities());
                    return info;
                }).toList();
        return Result.success(list);
    }
}
