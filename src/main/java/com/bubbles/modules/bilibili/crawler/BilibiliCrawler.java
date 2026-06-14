package com.bubbles.modules.bilibili.crawler;

import com.bubbles.common.properties.AiWriterProperties;
import com.bubbles.modules.core.RawSignal;
import com.bubbles.pojo.dto.BiliHotResponseDTO;
import com.bubbles.pojo.entity.HotSnapshot;
import com.bubbles.pojo.entity.RawSignalRecord;
import com.bubbles.server.mapper.HotSnapshotMapper;
import com.bubbles.server.mapper.PartitionMappingMapper;
import com.bubbles.server.mapper.RawSignalRecordMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * B站热点爬虫 —— 通过 Python AI 服务 (UApiPro SDK) 获取 B站热榜数据
 *
 * 负责：原始数据获取 → 分区映射 → 落库 hot_snapshot + raw_signal → 输出 RawSignal 列表
 */
@Slf4j
@Component
public class BilibiliCrawler {

    private final WebClient webClient;
    private final HotSnapshotMapper hotSnapshotMapper;
    private final RawSignalRecordMapper rawSignalRecordMapper;
    private final PartitionMappingMapper partitionMappingMapper;
    private final AiWriterProperties aiWriterProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.service.base-url:http://localhost:8001}")
    private String aiServiceBaseUrl;

    @Value("${ai.service.timeout:60000}")
    private int timeout;

    public BilibiliCrawler(WebClient.Builder webClientBuilder,
                           HotSnapshotMapper hotSnapshotMapper,
                           RawSignalRecordMapper rawSignalRecordMapper,
                           PartitionMappingMapper partitionMappingMapper,
                           AiWriterProperties aiWriterProperties) {
        this.webClient = webClientBuilder.build();
        this.hotSnapshotMapper = hotSnapshotMapper;
        this.rawSignalRecordMapper = rawSignalRecordMapper;
        this.partitionMappingMapper = partitionMappingMapper;
        this.aiWriterProperties = aiWriterProperties;
    }

    /**
     * 从 Python AI 服务获取 B站热榜并转换为 RawSignal 列表，同时落库
     */
    public List<RawSignal> fetchHotTopics(int limit) {
        List<BiliHotResponseDTO.BiliHotItemDTO> hotItems = fetchFromAIService(limit);
        if (hotItems == null || hotItems.isEmpty()) {
            log.warn("[B站爬虫] 未获取到热榜数据");
            return List.of();
        }

        LocalDateTime snapshotTime = LocalDateTime.now();
        List<HotSnapshot> snapshots = new ArrayList<>();
        List<RawSignal> signals = new ArrayList<>();

        for (BiliHotResponseDTO.BiliHotItemDTO item : hotItems) {
            String partitionTag = partitionMappingMapper.getPartitionByTname(item.getCategory());
            if (partitionTag == null) {
                partitionTag = "其他";
            }

            // 解析发布时间
            LocalDateTime pubDate = parsePubdate(item.getPubdate());

            // 落库到 hot_snapshot（向后兼容）
            HotSnapshot snapshot = HotSnapshot.builder()
                    .snapshotTime(snapshotTime)
                    .bvid(item.getBvid())
                    .title(item.getTitle())
                    .url(item.getUrl())
                    .coverUrl(item.getCoverUrl())
                    .author(item.getAuthor())
                    .tname(item.getCategory())
                    .partitionTag(partitionTag)
                    .pubdate(pubDate)
                    .description(item.getDescription())
                    .rank(item.getRank())
                    .viewCount(item.getViewCount())
                    .likeCount(item.getLikeCount())
                    .coinCount(item.getCoinCount())
                    .favoriteCount(item.getFavoriteCount())
                    .shareCount(item.getShareCount())
                    .danmakuCount(item.getDanmakuCount())
                    .replyCount(item.getReplyCount())
                    .hotScore(null)
                    .build();
            snapshots.add(snapshot);

            // 落库到 raw_signal（新多平台信号表）
            Map<String, Object> rawMetrics = new HashMap<>();
            rawMetrics.put("viewCount", item.getViewCount());
            rawMetrics.put("likeCount", item.getLikeCount());
            rawMetrics.put("coinCount", item.getCoinCount());
            rawMetrics.put("favoriteCount", item.getFavoriteCount());
            rawMetrics.put("shareCount", item.getShareCount());
            rawMetrics.put("danmakuCount", item.getDanmakuCount());
            rawMetrics.put("replyCount", item.getReplyCount());
            if (pubDate != null) {
                rawMetrics.put("pubDate", pubDate.toString());
            }
            if (item.getDescription() != null) {
                rawMetrics.put("description", item.getDescription());
            }

            String metricsJson;
            try {
                metricsJson = objectMapper.writeValueAsString(rawMetrics);
            } catch (Exception e) {
                metricsJson = "{}";
            }

            RawSignalRecord record = RawSignalRecord.builder()
                    .source("bilibili")
                    .sourceId(item.getBvid())
                    .title(item.getTitle())
                    .url(item.getUrl())
                    .author(item.getAuthor())
                    .coverUrl(item.getCoverUrl())
                    .category(item.getCategory())
                    .partitionTag(partitionTag)
                    .rank(item.getRank())
                    .rawMetrics(metricsJson)
                    .fetchedAt(snapshotTime)
                    .build();
            rawSignalRecordMapper.insert(record);

            // 构建模块 DTO
            RawSignal signal = RawSignal.builder()
                    .source("bilibili")
                    .sourceId(item.getBvid())
                    .title(item.getTitle())
                    .url(item.getUrl())
                    .author(item.getAuthor())
                    .coverUrl(item.getCoverUrl())
                    .category(item.getCategory())
                    .rank(item.getRank())
                    .rawMetrics(rawMetrics)
                    .fetchedAt(snapshotTime)
                    .build();
            signals.add(signal);
        }

        // 批量落库 hot_snapshot（向后兼容）
        try {
            hotSnapshotMapper.batchInsert(snapshots);
            log.info("[B站爬虫] 成功同步 {} 条热榜数据到 hot_snapshot + raw_signal, snapshotTime={}",
                    snapshots.size(), snapshotTime);
        } catch (Exception e) {
            log.error("[B站爬虫] hot_snapshot 落库失败", e);
        }

        return signals;
    }

    /**
     * 解析发布时间字符串为 LocalDateTime
     * 支持 ISO 8601 格式 (e.g. "2024-12-15T10:30:00") 和 Unix 时间戳数字字符串
     */
    private LocalDateTime parsePubdate(String pubdate) {
        if (pubdate == null || pubdate.isBlank()) return null;
        try {
            // 先尝试 ISO 格式 (Python datetime JSON 序列化格式)
            return LocalDateTime.parse(pubdate, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            try {
                // 回退：Unix 时间戳 (秒)
                long epochSeconds = Long.parseLong(pubdate.trim());
                return LocalDateTime.ofEpochSecond(epochSeconds, 0, java.time.ZoneOffset.ofHours(8));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }

    /**
     * 调用 Python AI 服务获取原始热榜数据
     */
    private List<BiliHotResponseDTO.BiliHotItemDTO> fetchFromAIService(int limit) {
        try {
            String url = aiServiceBaseUrl + "/api/bilibili/hot?hot_type=hot&limit=" + limit;

            Mono<BiliHotResponseDTO> responseMono = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(BiliHotResponseDTO.class)
                    .timeout(Duration.ofMillis(timeout));

            BiliHotResponseDTO response = responseMono.block();

            if (response != null && response.getItems() != null) {
                log.info("[B站爬虫] 从AI服务获取到 {} 条热榜数据, source={}",
                        response.getItems().size(), response.getSource());
                return response.getItems();
            }
        } catch (Exception e) {
            log.error("[B站爬虫] 调用AI服务获取热榜失败: {}", e.getMessage());
        }
        return List.of();
    }
}
