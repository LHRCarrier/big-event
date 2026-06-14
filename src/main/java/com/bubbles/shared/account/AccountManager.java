package com.bubbles.shared.account;

import com.bubbles.modules.core.AccountStatus;

import java.util.Map;

/**
 * 统一账号管理器 —— 管理各平台账号的凭据、登录态、发布配额
 *
 * 各平台模块通过此接口查询/更新账号状态，由统一层负责 cookie/token 刷新和异常告警。
 * 初期为内存实现，后续可持久化到 DB。
 */
public interface AccountManager {

    /** 获取所有平台账号状态 */
    Map<String, AccountStatus> getAllAccountStatus();

    /** 获取指定平台账号状态 */
    AccountStatus getAccountStatus(String platform);

    /** 更新账号状态 */
    void updateAccountStatus(String platform, AccountStatus status);

    /** 增加当日发布计数 */
    void incrementPublishedCount(String platform);

    /** 检查是否可以发布（未超当日限额且账号健康） */
    boolean canPublish(String platform);
}
