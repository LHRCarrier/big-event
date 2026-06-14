package com.bubbles.server.service.impl;

import com.bubbles.common.properties.AiWriterProperties;
import com.bubbles.pojo.entity.HotSnapshot;
import com.bubbles.pojo.vo.HotTopicVO;
import com.bubbles.server.mapper.AiProcessRecordMapper;
import com.bubbles.server.mapper.HotSnapshotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class HotScoreCalculator {

    private static final double W_VIEW = 0.30;
    private static final double W_LIKE = 0.20;
    private static final double W_COIN = 0.20;
    private static final double W_FAVORITE = 0.15;
    private static final double W_SHARE = 0.15;
    private static final double DECAY_K = 0.03;

    private final HotSnapshotMapper hotSnapshotMapper;
    private final AiProcessRecordMapper aiProcessRecordMapper;
    private final AiWriterProperties aiWriterProperties;

    /**
     * 获取当前 Top N 热点话题（去重已处理）
     */
    public List<HotTopicVO> getTopTopics(int topN, String partition) {
        LocalDateTime now = LocalDateTime.now();
        int retentionDays = aiWriterProperties.getSnapshot().getRetentionDays();
        LocalDateTime startTime = now.minusDays(retentionDays);

        // 1. 拉取时间窗口内所有快照
        List<HotSnapshot> allSnapshots = hotSnapshotMapper.getByTimeRange(startTime, now);
        if (allSnapshots.isEmpty()) {
            log.warn("[评分引擎] 没有快照数据，无法计算评分");
            return List.of();
        }
        log.info("[评分引擎] 拉取 {} 条快照数据, 时间范围: {} ~ {}", allSnapshots.size(), startTime, now);

        // 2. 按 snapshot_time 分组（每组 = 一个可做归一化的批次）
        Map<LocalDateTime, List<HotSnapshot>> batches = allSnapshots.stream()
                .collect(Collectors.groupingBy(HotSnapshot::getSnapshotTime,
                        LinkedHashMap::new, Collectors.toList()));

        // 3. 每批次做 min-max 归一化并计算各项 BASE_SCORE
        //    bvidBatchScores: bvid → 该bvid在各批次中的分数列表
        //    bvidFirstAppear: bvid → 首次出现的 snapshot_time
        Map<String, List<Double>> bvidBatchScores = new HashMap<>();
        Map<String, LocalDateTime> bvidFirstAppear = new HashMap<>();

        for (Map.Entry<LocalDateTime, List<HotSnapshot>> entry : batches.entrySet()) {
            LocalDateTime batchTime = entry.getKey();
            List<HotSnapshot> batch = entry.getValue();
            scoreBatch(batch, bvidBatchScores, bvidFirstAppear, batchTime);
        }

        // 4. 构建 bvid → 最新快照数据的映射（提前，供时效衰减计算使用）
        List<HotSnapshot> latestBatch = batches.values().stream()
                .findFirst().orElse(List.of()); // LinkedHashMap 保证了最新批次在前

        Map<String, HotSnapshot> latestByBvid = latestBatch.stream()
                .collect(Collectors.toMap(HotSnapshot::getBvid, s -> s, (a, b) -> a));

        // 5. 对每个 bvid 计算最终综合评分
        List<ScoredTopic> scored = new ArrayList<>();
        for (Map.Entry<String, List<Double>> entry : bvidBatchScores.entrySet()) {
            String bvid = entry.getKey();
            List<Double> scores = entry.getValue();
            double avgBaseScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);

            // 优先使用 pubdate（视频发布时间），回退到首次快照时间计算时效衰减
            HotSnapshot latest = latestByBvid.get(bvid);
            LocalDateTime publishTime = (latest != null && latest.getPubdate() != null)
                    ? latest.getPubdate() : bvidFirstAppear.get(bvid);
            long daysSinceFirst = ChronoUnit.DAYS.between(publishTime, now);
            double decay = Math.exp(-DECAY_K * Math.max(daysSinceFirst, 0));
            double trend = computeTrendFactor(scores);
            double finalScore = avgBaseScore * decay * trend;

            scored.add(new ScoredTopic(bvid, finalScore));
        }

        // 6. 按评分降序排序并应用分区筛选

        scored.sort((a, b) -> Double.compare(b.score, a.score));

        List<HotTopicVO> result = new ArrayList<>();
        for (ScoredTopic st : scored) {
            if (result.size() >= topN) break;

            HotSnapshot snapshot = latestByBvid.get(st.bvid);
            if (snapshot == null) continue;

            // 分区筛选
            if (partition != null && !"all".equals(partition)
                    && !partition.equals(snapshot.getPartitionTag())) {
                continue;
            }

            boolean processed = aiProcessRecordMapper.countByBvid(st.bvid) > 0;

            result.add(HotTopicVO.builder()
                    .bvid(snapshot.getBvid())
                    .title(snapshot.getTitle())
                    .url(snapshot.getUrl())
                    .coverUrl(snapshot.getCoverUrl())
                    .author(snapshot.getAuthor())
                    .tname(snapshot.getTname())
                    .partitionTag(snapshot.getPartitionTag())
                    .pubDate(snapshot.getPubdate())
                    .description(snapshot.getDescription())
                    .viewCount(snapshot.getViewCount())
                    .likeCount(snapshot.getLikeCount())
                    .coinCount(snapshot.getCoinCount())
                    .favoriteCount(snapshot.getFavoriteCount())
                    .shareCount(snapshot.getShareCount())
                    .danmakuCount(snapshot.getDanmakuCount())
                    .replyCount(snapshot.getReplyCount())
                    .score(Math.round(st.score * 100.0) / 100.0)
                    .alreadyProcessed(processed)
                    .build());
        }

        log.info("[评分引擎] Top {} 话题计算完成, 返回 {} 条", topN, result.size());
        return result;
    }

    /**
     * 对一个快照批次做归一化和评分
     */
    private void scoreBatch(List<HotSnapshot> batch,
                            Map<String, List<Double>> bvidBatchScores,
                            Map<String, LocalDateTime> bvidFirstAppear,
                            LocalDateTime batchTime) {
        // 计算该批次各指标的最大最小值
        long minView = batch.stream().mapToLong(s -> s.getViewCount() != null ? s.getViewCount() : 0).min().orElse(0);
        long maxView = batch.stream().mapToLong(s -> s.getViewCount() != null ? s.getViewCount() : 0).max().orElse(1);
        long minLike = batch.stream().mapToLong(s -> s.getLikeCount() != null ? s.getLikeCount() : 0).min().orElse(0);
        long maxLike = batch.stream().mapToLong(s -> s.getLikeCount() != null ? s.getLikeCount() : 0).max().orElse(1);
        long minCoin = batch.stream().mapToLong(s -> s.getCoinCount() != null ? s.getCoinCount() : 0).min().orElse(0);
        long maxCoin = batch.stream().mapToLong(s -> s.getCoinCount() != null ? s.getCoinCount() : 0).max().orElse(1);
        long minFav = batch.stream().mapToLong(s -> s.getFavoriteCount() != null ? s.getFavoriteCount() : 0).min().orElse(0);
        long maxFav = batch.stream().mapToLong(s -> s.getFavoriteCount() != null ? s.getFavoriteCount() : 0).max().orElse(1);
        long minShare = batch.stream().mapToLong(s -> s.getShareCount() != null ? s.getShareCount() : 0).min().orElse(0);
        long maxShare = batch.stream().mapToLong(s -> s.getShareCount() != null ? s.getShareCount() : 0).max().orElse(1);

        for (HotSnapshot s : batch) {
            String bvid = s.getBvid();
            if (bvid == null) continue;

            double normView = normalize(s.getViewCount(), minView, maxView);
            double normLike = normalize(s.getLikeCount(), minLike, maxLike);
            double normCoin = normalize(s.getCoinCount(), minCoin, maxCoin);
            double normFav = normalize(s.getFavoriteCount(), minFav, maxFav);
            double normShare = normalize(s.getShareCount(), minShare, maxShare);

            double baseScore = (normView * W_VIEW + normLike * W_LIKE + normCoin * W_COIN
                    + normFav * W_FAVORITE + normShare * W_SHARE) * 100.0;

            bvidBatchScores.computeIfAbsent(bvid, k -> new ArrayList<>()).add(baseScore);
            bvidFirstAppear.putIfAbsent(bvid, batchTime);
        }
    }

    /**
     * min-max 归一化到 [0, 1]
     */
    private double normalize(Long value, long min, long max) {
        if (value == null) return 0;
        if (max == min) return 0;
        return (double) (value - min) / (max - min);
    }

    /**
     * 趋势因子: 最近3次平均 / 前3次平均
     */
    private double computeTrendFactor(List<Double> scores) {
        int n = scores.size();
        if (n < 6) return 1.0; // 不足6次快照按持平处理

        double recent3 = scores.subList(0, Math.min(3, n)).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);
        double earlier3 = scores.subList(Math.max(0, n - 3), n).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);

        if (earlier3 == 0) return 1.0;
        return Math.max(0.5, Math.min(2.0, recent3 / earlier3));
    }

    private static class ScoredTopic {
        final String bvid;
        final double score;

        ScoredTopic(String bvid, double score) {
            this.bvid = bvid;
            this.score = score;
        }
    }
}
