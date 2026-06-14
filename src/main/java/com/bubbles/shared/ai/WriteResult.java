package com.bubbles.shared.ai;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 撰稿结果
 */
@Data
@Builder
public class WriteResult {
    private String articleId;
    private String title;
    private String content;
    private String summary;
    private String modelUsed;
    private LocalDateTime generatedAt;
}
