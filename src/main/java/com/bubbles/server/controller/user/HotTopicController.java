package com.bubbles.server.controller.user;

import com.bubbles.common.result.Result;
import com.bubbles.pojo.vo.HotTopicVO;
import com.bubbles.server.service.HotTopicSyncService;
import com.bubbles.server.service.impl.HotScoreCalculator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user/hot")
@RequiredArgsConstructor
@Tag(name = "热点话题", description = "热点话题评分与筛选接口")
public class HotTopicController {

    private final HotScoreCalculator scoreCalculator;
    private final HotTopicSyncService syncService;

    /**
     * 获取 Top N 热点话题（带评分）
     */
    @GetMapping("/topics")
    @Operation(summary = "获取热点话题排行", description = "基于历史快照综合评分，返回 Top N 热点话题")
    public Result<List<HotTopicVO>> getTopTopics(
            @RequestParam(defaultValue = "10") int topN,
            @RequestParam(defaultValue = "all") String partition) {
        log.info("获取 Top {} 热点话题, partition={}", topN, partition);
        List<HotTopicVO> topics = scoreCalculator.getTopTopics(topN, partition);
        return Result.success(topics);
    }

    /**
     * 手动触发热榜同步（从B站拉取最新数据落库）
     */
    @PostMapping("/sync")
    @Operation(summary = "手动同步热榜", description = "立即从B站拉取最新热榜数据并落库，同步完成后可重新获取话题")
    public Result<String> syncNow() {
        log.info("手动触发热榜同步");
        syncService.syncNow();
        return Result.success("同步完成");
    }
}
