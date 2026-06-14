package com.bubbles.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 信号-话题关联实体 —— 对应 signal_topic_rel 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalTopicRel {
    private Long signalId;
    private Long topicId;
    private BigDecimal confidence;
}
