package com.bubbles.modules.core;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发布结果
 */
@Data
@Builder
public class PublishResult {
    /** 是否发布成功 */
    private boolean success;
    /** 平台返回的文章ID */
    private String platformArticleId;
    /** 平台返回的文章URL */
    private String platformArticleUrl;
    /** 错误信息 */
    private String errorMessage;
    /** 发布时间 */
    private LocalDateTime publishedAt;
}
