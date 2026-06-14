package com.bubbles.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bubbles.ai-writer")
public class AiWriterProperties {

    private AutoPublish autoPublish = new AutoPublish();
    private Snapshot snapshot = new Snapshot();

    private Long systemUserId = 1L;

    @Data
    public static class AutoPublish {
        private boolean enabled = false;
        private boolean afterSync = false; // 同步后是否自动触发发布
        private int topN = 5;
        private int minScore = 60;
        private String partition = "all";
    }

    @Data
    public static class Snapshot {
        private int syncIntervalMin = 30;
        private int retentionDays = 60;
    }
}
