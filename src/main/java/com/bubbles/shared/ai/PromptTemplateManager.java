package com.bubbles.shared.ai;

/**
 * Prompt 模板管理器 —— 管理各平台的 prompt 模板
 *
 * 支持按平台、话题类型、风格检索模板；平台模块的 ContentStrategy.buildPrompt()
 * 内部可委托本管理器获取基础模板后做平台特化调整。
 */
public interface PromptTemplateManager {

    /**
     * 按平台名称获取基础 prompt 模板
     */
    String getBaseTemplate(String platform);

    /**
     * 注册/更新平台 prompt 模板
     */
    void registerTemplate(String platform, String template);
}
