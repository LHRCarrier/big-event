package com.bubbles.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiProcessRecord {
    private Long id;
    /** @deprecated kept for backward compat, use source + sourceId instead */
    @Deprecated
    private String bvid;
    private String source;
    private String sourceId;
    private Long articleId;
    private LocalDateTime processTime;
    private String status;
    private String aiModel;
    private BigDecimal hotScore;
    private LocalDateTime createdAt;
}
