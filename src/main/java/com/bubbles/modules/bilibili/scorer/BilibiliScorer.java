package com.bubbles.modules.bilibili.scorer;

import com.bubbles.common.properties.AiWriterProperties;
import com.bubbles.modules.core.RawSignal;
import com.bubbles.modules.core.ScoredTopic;
import com.bubbles.pojo.entity.HotSnapshot;
import com.bubbles.server.mapper.AiProcessRecordMapper;
import com.bubbles.server.mapper.HotSnapshotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * B站评分引擎
 *
 * B站专属的 5 维评分策略（基于架构文档设计）：
 * - 热度 (0.30): 播放/点赞/投币/收藏/分享 加权 + min-max 归一化
 * - 持续性 (0.20): 多批次快照趋势分析，检测上升/下降
 * - 深度潜力 (0.25): 基于话题类型的 LLM 评估（当前简化版用规则）
 * - 差异性 (0.15): 去重检查
 * - 受众匹配 (0.10): 分区与目标受众的匹配度
 *
 * 评分与旧 HotScoreCalculator 的关键区别：
 * 1. 从"评视频热度"改为"评话题撰稿价值"
 * 2. 支持 5 维评分而非单一热度分
 * 3. 接收 RawSignal 而非直接查 DB
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BilibiliScorer {

    private static final double W_HOT = 0.30;
    private static final double W_SUSTAIN = 0.20;
    private static final double W_DEPTH = 0.25;
    private static final double W_DIVERSITY = 0.15;
    private static final double W_AUDIENCE = 0.10;

    // 热度子权重（当前保留与原评分一致）
    private static final double W_VIEW = 0.30;
    private static final double W_LIKE = 0.20;
    private static final double W_COIN = 0.20;
    private static final double W_FAVORITE = 0.15;
    private static final double W_SHARE = 0.15;

    private static final double DECAY_K = 0.03;

    // 分区-受众匹配度映射（B站视角，泛科技方向内容平台）
    private static final Map<String, Double> PARTITION_AUDIENCE_MAP = new HashMap<>();
    static {
        PARTITION_AUDIENCE_MAP.put("科技", 0.95);
        PARTITION_AUDIENCE_MAP.put("游戏", 0.80);
        PARTITION_AUDIENCE_MAP.put("知识", 0.85);
        PARTITION_AUDIENCE_MAP.put("数码", 0.90);
        PARTITION_AUDIENCE_MAP.put("商业", 0.75);
        PARTITION_AUDIENCE_MAP.put("娱乐", 0.55);
        PARTITION_AUDIENCE_MAP.put("影视", 0.50);
        PARTITION_AUDIENCE_MAP.put("生活", 0.60);
        PARTITION_AUDIENCE_MAP.put("体育", 0.30);
        PARTITION_AUDIENCE_MAP.put("音乐", 0.40);
        PARTITION_AUDIENCE_MAP.put("动画", 0.50);
        PARTITION_AUDIENCE_MAP.put("舞蹈", 0.25);
        PARTITION_AUDIENCE_MAP.put("美食", 0.35);
        PARTITION_AUDIENCE_MAP.put("时尚", 0.30);
    }

    private final HotSnapshotMapper hotSnapshotMapper;
    private final AiProcessRecordMapper aiProcessRecordMapper;
    private final AiWriterProperties aiWriterProperties;

    /**
     * 对信号列表进行 B站特化评分
     */
    public List<ScoredTopic> score(List<RawSignal> signals) {
        if (signals.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        int retentionDays = aiWriterProperties.getSnapshot().getRetentionDays();
        LocalDateTime startTime = now.minusDays(retentionDays);

        // 1. 加载历史快照数据用于趋势计算
        List<HotSnapshot> allSnapshots = hotSnapshotMapper.getByTimeRange(startTime, now);
        Map<String, List<HotSnapshot>> historyByBvid = allSnapshots.stream()
                .filter(s -> s.getBvid() != null)
                .collect(Collectors.groupingBy(HotSnapshot::getBvid));

        // 2. 对每个信号进行 5 维评分
        List<ScoredTopic> results = new ArrayList<>();
        for (RawSignal signal : signals) {
            String bvid = signal.getSourceId();
            if (bvid == null) continue;

            boolean processed = aiProcessRecordMapper.countBySourceId("bilibili", bvid) > 0;

            // 维度1: 热度 (0-100)
            double hotScore = computeHotScore(signal);

            // 维度2: 持续性 (0-100)
            double sustainScore = computeSustainScore(bvid, historyByBvid, now);

            // 维度3: 深度潜力 (0-100) — 当前简化版基于分类+标题长度
            double depthScore = computeDepthScore(signal);

            // 维度4: 差异性 (0-100) — 已处理则为 0，未处理为 100
            double diversityScore = processed ? 0.0 : 100.0;

            // 维度5: 受众匹配 (0-100)
            double audienceScore = computeAudienceScore(signal);

            // 综合评分
            double total = hotScore * W_HOT
                    + sustainScore * W_SUSTAIN
                    + depthScore * W_DEPTH
                    + diversityScore * W_DIVERSITY
                    + audienceScore * W_AUDIENCE;

            ScoredTopic topic = ScoredTopic.builder()
                    .signal(signal)
                    .score(Math.round(total * 100.0) / 100.0)
                    .hotScore(Math.round(hotScore * 100.0) / 100.0)
                    .sustainabilityScore(Math.round(sustainScore * 100.0) / 100.0)
                    .depthScore(Math.round(depthScore * 100.0) / 100.0)
                    .diversityScore(Math.round(diversityScore * 100.0) / 100.0)
                    .audienceScore(Math.round(audienceScore * 100.0) / 100.0)
                    .alreadyProcessed(processed)
                    .build();

            results.add(topic);
        }

        // 按综合评分降序排列
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        log.info("[B站评分] 完成 {} 个话题评分, Top3: {}",
                results.size(),
                results.stream().limit(3).map(t -> String.format("%s(%.1f)", t.getSignal().getTitle(), t.getScore())).toList());

        return results;
    }

    /**
     * 维度1: 热度分 —— 5项互动指标的 min-max 归一化加权
     */
    private double computeHotScore(RawSignal signal) {
        Map<String, Object> m = signal.getRawMetrics();
        long view = getLong(m, "viewCount");
        long like = getLong(m, "likeCount");
        long coin = getLong(m, "coinCount");
        long fav = getLong(m, "favoriteCount");
        long share = getLong(m, "shareCount");

        // 对单条信号，使用对数归一化（log10 压缩数量级差异）
        double normView = logNorm(view, 1e3, 1e7);
        double normLike = logNorm(like, 10, 1e6);
        double normCoin = logNorm(coin, 1, 1e5);
        double normFav = logNorm(fav, 10, 1e5);
        double normShare = logNorm(share, 1, 1e4);

        return (normView * W_VIEW + normLike * W_LIKE + normCoin * W_COIN
                + normFav * W_FAVORITE + normShare * W_SHARE) * 100.0;
    }

    /**
     * 维度2: 持续性分 —— 基于历史快照的趋势分析
     */
    private double computeSustainScore(String bvid, Map<String, List<HotSnapshot>> historyByBvid, LocalDateTime now) {
        List<HotSnapshot> history = historyByBvid.getOrDefault(bvid, List.of());
        if (history.size() < 2) {
            return 50.0; // 新信号，默认中等持续性
        }

        // 按时间排序
        List<HotSnapshot> sorted = history.stream()
                .sorted(Comparator.comparing(HotSnapshot::getSnapshotTime))
                .toList();

        // 计算近期平均热度 vs 早期平均热度
        int mid = sorted.size() / 2;
        double recent = avgViewCount(sorted.subList(mid, sorted.size()));
        double earlier = avgViewCount(sorted.subList(0, mid));

        if (earlier == 0) return 50.0;

        double ratio = recent / earlier;
        // 上升趋势 → 高分
        if (ratio > 1.5) return 85.0;
        if (ratio > 1.2) return 70.0;
        if (ratio > 0.8) return 50.0;
        if (ratio > 0.5) return 30.0;
        return 15.0; // 热度快速下降
    }

    /**
     * 维度3: 深度潜力 —— 基于话题分类 + 标题信息量评估
     */
    private double computeDepthScore(RawSignal signal) {
        String title = signal.getTitle();
        String category = signal.getCategory();

        double score = 50.0;

        // 标题长度反映信息量 (20-50 字最佳)
        if (title != null) {
            int len = title.length();
            if (len >= 30 && len <= 80) score += 25;
            else if (len >= 20 && len <= 100) score += 15;
            else score += 5;
        }

        // 知识/科技/数码类话题天然适合深度文章
        if (category != null) {
            String c = category.toLowerCase();
            if (c.contains("知识") || c.contains("科技") || c.contains("数码") || c.contains("商业")) {
                score += 20;
            } else if (c.contains("游戏") || c.contains("生活")) {
                score += 10;
            } else if (c.contains("娱乐") || c.contains("舞蹈")) {
                score -= 10;
            }
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * 维度5: 受众匹配 —— 分区与目标受众的契合度
     */
    private double computeAudienceScore(RawSignal signal) {
        String category = signal.getCategory();
        if (category == null) return 50.0;

        String partition = signal.getCategory(); // 原始分区名
        // 遍历映射表查找最佳匹配
        for (Map.Entry<String, Double> entry : PARTITION_AUDIENCE_MAP.entrySet()) {
            if (partition.contains(entry.getKey())) {
                return entry.getValue() * 100.0;
            }
        }
        return 50.0; // 未知分区默认中等
    }

    private double logNorm(long value, double min, double max) {
        if (value <= 0) return 0;
        double logV = Math.log10(value);
        double logMin = Math.log10(min);
        double logMax = Math.log10(max);
        double normalized = (logV - logMin) / (logMax - logMin);
        return Math.max(0, Math.min(1.0, normalized));
    }

    private long getLong(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.longValue();
        return 0L;
    }

    private double avgViewCount(List<HotSnapshot> snapshots) {
        return snapshots.stream()
                .mapToLong(s -> s.getViewCount() != null ? s.getViewCount() : 0)
                .average().orElse(0);
    }
}
