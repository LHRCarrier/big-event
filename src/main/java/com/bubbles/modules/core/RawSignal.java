package com.bubbles.modules.core;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 平台原始信号 —— 从各平台采集的原始热点条目，尚未做话题提取和归一化
 */
@Data
@Builder
public class RawSignal {
    /** 信号来源平台: bilibili/zhihu/weibo/toutiao */
    private String source;
    /** 平台内唯一ID (bvid / 问题ID / 热搜ID) */
    private String sourceId;
    /** 原始标题 */
    private String title;
    /** 原文URL */
    private String url;
    /** 作者/UP主 */
    private String author;
    /** 封面图URL */
    private String coverUrl;
    /** 分类/分区 */
    private String category;
    /** 排名 */
    private Integer rank;
    /** 原始指标 (平台相关字段, 如播放量/点赞等) */
    private Map<String, Object> rawMetrics;
    /** 归一化后的平台内热度分 (0-100) */
    private Double normScore;
    /** 采集时间 */
    private LocalDateTime fetchedAt;
}
