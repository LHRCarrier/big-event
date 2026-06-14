package com.bubbles.modules.core;

import lombok.Builder;
import lombok.Data;

/**
 * 评分后的话题 —— 经过平台特定评分策略处理后的结果
 */
@Data
@Builder
public class ScoredTopic {
    /** 原始信号 */
    private RawSignal signal;
    /** 综合评分 (0-100) */
    private double score;
    /** 热度分 */
    private double hotScore;
    /** 持续性分 */
    private double sustainabilityScore;
    /** 深度潜力分 */
    private double depthScore;
    /** 差异度分 */
    private double diversityScore;
    /** 受众匹配分 */
    private double audienceScore;
    /** 是否已被处理 */
    private boolean alreadyProcessed;
}
