package com.bubbles.orchestrator;

import lombok.Builder;
import lombok.Data;

/**
 * 流水线执行结果
 */
@Data
@Builder
public class PipelineResult {
    private String platform;
    private boolean success;
    private int signalsCollected;
    private int topicsScored;
    private int articlesGenerated;
    private int articlesPublished;
    private String message;
}
