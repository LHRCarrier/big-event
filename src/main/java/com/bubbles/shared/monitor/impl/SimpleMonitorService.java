package com.bubbles.shared.monitor.impl;

import com.bubbles.shared.monitor.MonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易监控服务 —— 内存 + 日志实现
 *
 * 初期使用内存存储发布事件，通过日志输出监控信息。
 * 后续可升级为 Prometheus + Grafana 或 ELK 方案。
 */
@Slf4j
@Service
public class SimpleMonitorService implements MonitorService {

    private static final int MAX_EVENTS = 1000;

    private final Map<String, List<PublishEvent>> eventsByPlatform = new ConcurrentHashMap<>();
    private final List<String> accountIssues = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void recordPublish(String platform, String articleId, boolean success, String errorMessage) {
        PublishEvent event = new PublishEvent(platform, articleId, success, errorMessage, LocalDateTime.now());

        eventsByPlatform.computeIfAbsent(platform, k -> new ArrayList<>()).add(event);

        // 限制内存使用
        List<PublishEvent> events = eventsByPlatform.get(platform);
        if (events.size() > MAX_EVENTS) {
            events.subList(0, events.size() - MAX_EVENTS).clear();
        }

        if (!success) {
            log.warn("[监控] 发布失败: platform={}, articleId={}, error={}", platform, articleId, errorMessage);
        } else {
            log.info("[监控] 发布成功: platform={}, articleId={}", platform, articleId);
        }
    }

    @Override
    public void recordAccountIssue(String platform, String message) {
        String entry = String.format("[%s] %s: %s", LocalDateTime.now(), platform, message);
        accountIssues.add(entry);
        log.warn("[监控] 账号异常: {}", entry);

        if (accountIssues.size() > 100) {
            accountIssues.subList(0, accountIssues.size() - 100).clear();
        }
    }

    @Override
    public double getPublishSuccessRate(String platform) {
        List<PublishEvent> events = eventsByPlatform.getOrDefault(platform, List.of());
        if (events.isEmpty()) return 100.0;

        // 只看过去24小时
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        long total = events.stream().filter(e -> e.timestamp.isAfter(since)).count();
        long success = events.stream().filter(e -> e.success && e.timestamp.isAfter(since)).count();

        return total == 0 ? 100.0 : (double) success / total * 100.0;
    }

    @Override
    public boolean isSystemHealthy() {
        // 简单检查：过去24小时无全平台失败
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        long totalRecent = eventsByPlatform.values().stream()
                .flatMap(Collection::stream)
                .filter(e -> e.timestamp.isAfter(since))
                .count();

        return totalRecent > 0 || accountIssues.isEmpty();
    }

    private record PublishEvent(String platform, String articleId, boolean success,
                                String errorMessage, LocalDateTime timestamp) {}
}
