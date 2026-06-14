package com.bubbles.shared.account.impl;

import com.bubbles.modules.core.AccountStatus;
import com.bubbles.shared.account.AccountManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易账号管理器 —— 内存实现
 *
 * 初期使用内存存储，后续可升级为 DB 持久化 + Redis 缓存。
 */
@Slf4j
@Service
public class SimpleAccountManager implements AccountManager {

    private final Map<String, AccountStatus> statusMap = new ConcurrentHashMap<>();

    @Override
    public Map<String, AccountStatus> getAllAccountStatus() {
        return Map.copyOf(statusMap);
    }

    @Override
    public AccountStatus getAccountStatus(String platform) {
        return statusMap.getOrDefault(platform, AccountStatus.builder()
                .platform(platform)
                .healthy(false)
                .loggedIn(false)
                .message("未注册")
                .build());
    }

    @Override
    public void updateAccountStatus(String platform, AccountStatus status) {
        statusMap.put(platform, status);
    }

    @Override
    public void incrementPublishedCount(String platform) {
        statusMap.computeIfPresent(platform, (k, v) -> {
            AccountStatus updated = AccountStatus.builder()
                    .platform(v.getPlatform())
                    .healthy(v.isHealthy())
                    .loggedIn(v.isLoggedIn())
                    .rateLimited(v.isRateLimited())
                    .publishedToday(v.getPublishedToday() + 1)
                    .dailyLimit(v.getDailyLimit())
                    .lastCheckedAt(LocalDateTime.now())
                    .message(v.getMessage())
                    .build();
            return updated;
        });
    }

    @Override
    public boolean canPublish(String platform) {
        AccountStatus status = getAccountStatus(platform);
        return status.isHealthy() && status.isLoggedIn()
                && !status.isRateLimited()
                && status.getPublishedToday() < status.getDailyLimit();
    }
}
