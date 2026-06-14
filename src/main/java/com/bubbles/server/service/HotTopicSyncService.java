package com.bubbles.server.service;

import com.bubbles.common.properties.AiWriterProperties;
import com.bubbles.pojo.dto.BiliHotResponseDTO;
import com.bubbles.pojo.entity.HotSnapshot;
import com.bubbles.server.mapper.HotSnapshotMapper;
import com.bubbles.server.mapper.PartitionMappingMapper;
import com.bubbles.server.service.impl.AutoPublishService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class HotTopicSyncService {

    private final WebClient webClient;
    private final HotSnapshotMapper hotSnapshotMapper;
    private final PartitionMappingMapper partitionMappingMapper;
    private final AutoPublishService autoPublishService;
    private final AiWriterProperties aiWriterProperties;

    @Value("${ai.service.base-url:http://localhost:8001}")
    private String aiServiceBaseUrl;

    @Value("${ai.service.timeout:60000}")
    private int timeout;

    public HotTopicSyncService(WebClient.Builder webClientBuilder,
                               HotSnapshotMapper hotSnapshotMapper,
                               PartitionMappingMapper partitionMappingMapper,
                               AutoPublishService autoPublishService,
                               AiWriterProperties aiWriterProperties) {
        this.webClient = webClientBuilder.build();
        this.hotSnapshotMapper = hotSnapshotMapper;
        this.partitionMappingMapper = partitionMappingMapper;
        this.autoPublishService = autoPublishService;
        this.aiWriterProperties = aiWriterProperties;
    }

    /**
     * 应用启动 5 秒后自动执行一次同步，避免页面打开时无数据
     */
    @Async
    @PostConstruct
    public void initialSync() {
        try {
            Thread.sleep(5000); // 等 Python AI 服务先就绪
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("[热榜同步] 应用启动，执行首次同步...");
        syncBilibiliHot();
    }

    /**
     * 手动触发同步（供 Controller 调用）
     */
    public int syncNow() {
        log.info("[热榜同步] 手动触发同步");
        syncBilibiliHot();
        return 1;
    }

    /**
     * 定时抓取B站热榜并落库，可选是否紧随触发自动发布
     */
    @Scheduled(fixedDelayString = "#{${bubbles.ai-writer.snapshot.sync-interval-min:30} * 60 * 1000}")
    @Transactional
    public void syncBilibiliHot() {
        log.info("[热榜同步] 开始定时抓取B站热榜数据...");
        try {
            List<BiliHotResponseDTO.BiliHotItemDTO> hotItems = fetchBilibiliHot();
            if (hotItems == null || hotItems.isEmpty()) {
                log.warn("[热榜同步] 未获取到热榜数据，跳过本次同步");
                return;
            }

            LocalDateTime snapshotTime = LocalDateTime.now();
            List<HotSnapshot> snapshots = new ArrayList<>();

            for (BiliHotResponseDTO.BiliHotItemDTO item : hotItems) {
                String partitionTag = partitionMappingMapper.getPartitionByTname(item.getCategory());
                if (partitionTag == null) {
                    partitionTag = "其他";
                }

                HotSnapshot snapshot = HotSnapshot.builder()
                        .snapshotTime(snapshotTime)
                        .bvid(item.getBvid())
                        .title(item.getTitle())
                        .url(item.getUrl())
                        .coverUrl(item.getCoverUrl())
                        .author(item.getAuthor())
                        .tname(item.getCategory())
                        .partitionTag(partitionTag)
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
            }

            hotSnapshotMapper.batchInsert(snapshots);
            log.info("[热榜同步] 成功同步 {} 条热榜数据, snapshotTime={}", snapshots.size(), snapshotTime);

            // 同步完成后可选自动触发发布
            if (aiWriterProperties.getAutoPublish().isAfterSync()) {
                log.info("[热榜同步] 同步完成，触发自动发布");
                autoPublishService.autoPublish();
            }

        } catch (Exception e) {
            log.error("[热榜同步] 同步失败", e);
        }
    }

    /**
     * 调用 Python AI 服务获取 B站热榜
     */
    private List<BiliHotResponseDTO.BiliHotItemDTO> fetchBilibiliHot() {
        try {
            String url = aiServiceBaseUrl + "/api/bilibili/hot?hot_type=hot&limit=50";

            Mono<BiliHotResponseDTO> responseMono = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(BiliHotResponseDTO.class)
                    .timeout(Duration.ofMillis(timeout));

            BiliHotResponseDTO response = responseMono.block();

            if (response != null && response.getItems() != null) {
                log.info("[热榜同步] 从AI服务获取到 {} 条热榜数据, source={}",
                        response.getItems().size(), response.getSource());
                return response.getItems();
            }

        } catch (Exception e) {
            log.error("[热榜同步] 调用AI服务获取热榜失败: {}", e.getMessage());
        }
        return List.of();
    }
}
