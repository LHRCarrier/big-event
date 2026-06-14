package com.bubbles.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 原始信号实体 —— 对应 raw_signal 表
 * 替代 hot_snapshot 的多平台信号统一采集层
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawSignalRecord {
    private Long id;
    private String source;
    private String sourceId;
    private String title;
    private String url;
    private String author;
    private String coverUrl;
    private String category;
    private String partitionTag;
    private Integer rank;
    /** JSON string of raw platform metrics */
    private String rawMetrics;
    private BigDecimal normScore;
    private LocalDateTime fetchedAt;
    private LocalDateTime createdAt;
}
