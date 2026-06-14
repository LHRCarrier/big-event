package com.bubbles.orchestrator;

import com.bubbles.modules.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.util.*;

/**
 * 模块注册中心 —— 管理所有平台模块的注册与发现
 *
 * Spring 自动注入所有 PlatformModule 实现类，
 * 通过 platformName() 区分各平台模块。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleRegistry {

    /** Spring 自动注入所有 PlatformModule bean */
    private final List<PlatformModule> allModules;

    @PostConstruct
    public void init() {
        logRegisteredModules();
    }

    /**
     * 获取所有已注册的模块（含未启用的）
     */
    public List<PlatformModule> getAllModules() {
        return Collections.unmodifiableList(allModules);
    }

    /**
     * 获取所有已启用的模块
     */
    public List<PlatformModule> getEnabledModules() {
        return allModules.stream()
                .filter(PlatformModule::isEnabled)
                .toList();
    }

    /**
     * 按平台名查找模块
     */
    public Optional<PlatformModule> getModule(String platformName) {
        return allModules.stream()
                .filter(m -> m.platformName().equalsIgnoreCase(platformName))
                .findFirst();
    }

    /**
     * 获取具备指定能力的模块列表
     */
    public List<PlatformModule> getModulesWithCapability(Capability capability) {
        return allModules.stream()
                .filter(m -> m.isEnabled() && m.capabilities().contains(capability))
                .toList();
    }

    /**
     * 获取所有平台账号状态汇总
     */
    public Map<String, AccountStatus> getAllAccountStatus() {
        Map<String, AccountStatus> statuses = new LinkedHashMap<>();
        for (PlatformModule m : allModules) {
            try {
                statuses.put(m.platformName(), m.checkAccount());
            } catch (Exception e) {
                log.warn("[模块注册] 获取 {} 账号状态失败", m.platformName(), e);
                statuses.put(m.platformName(), AccountStatus.builder()
                        .platform(m.platformName())
                        .healthy(false)
                        .message("状态检查失败: " + e.getMessage())
                        .build());
            }
        }
        return statuses;
    }

    /**
     * 打印已注册模块信息（启动时调用）
     */
    public void logRegisteredModules() {
        log.info("[模块注册] ========== 平台模块注册状态 ==========");
        for (PlatformModule m : allModules) {
            log.info("[模块注册]   {} | enabled={} | capabilities={}",
                    m.platformName(), m.isEnabled(), m.capabilities());
        }

        long enabledCount = allModules.stream().filter(PlatformModule::isEnabled).count();
        log.info("[模块注册] 总计: {} 模块已注册, {} 已启用", allModules.size(), enabledCount);
    }
}
