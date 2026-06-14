package com.bubbles.modules.core;

/**
 * 平台内容策略 —— 定义该平台文章的 prompt 模板、长度、风格、格式
 */
public interface ContentStrategy {

    /** 平台名称 */
    String platformName();

    /** 获取该平台最佳文章长度范围 (字) */
    int minLength();

    int maxLength();

    /** 获取该平台内容风格提示 */
    String styleGuide();

    /** 获取该平台排版格式要求 */
    String formatGuide();

    /** 构建该平台的撰稿 prompt */
    String buildPrompt(ScoredTopic topic);
}
