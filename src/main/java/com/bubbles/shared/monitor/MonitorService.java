package com.bubbles.shared.monitor;

/**
 * 通用监控服务 —— 跨平台统一的发布成功率、账号异常、内容审核驳回监控
 */
public interface MonitorService {

    /** 记录一次发布事件 */
    void recordPublish(String platform, String articleId, boolean success, String errorMessage);

    /** 记录账号异常 */
    void recordAccountIssue(String platform, String message);

    /** 获取24h发布成功率 */
    double getPublishSuccessRate(String platform);

    /** 健康检查 */
    boolean isSystemHealthy();
}
