package com.bubbles.modules.core;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台账号状态
 */
@Data
@Builder
public class AccountStatus {
    /** 平台名称 */
    private String platform;
    /** 账号是否正常 */
    private boolean healthy;
    /** 登录是否有效 */
    private boolean loggedIn;
    /** 是否被限流 */
    private boolean rateLimited;
    /** 当日已发布数 */
    private int publishedToday;
    /** 当日发布上限 */
    private int dailyLimit;
    /** 最后检查时间 */
    private LocalDateTime lastCheckedAt;
    /** 状态备注 */
    private String message;
}
