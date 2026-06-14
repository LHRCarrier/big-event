package com.bubbles.shared.ai;

import com.bubbles.modules.core.ContentStrategy;
import com.bubbles.modules.core.ScoredTopic;

/**
 * AI 撰稿引擎 —— 跨平台共享的 LLM 调用能力
 *
 * 负责多步骤撰稿流水线：搜集资料 → 确定视角 → 生成大纲 → 撰写正文 → 润色
 * 各平台模块通过 ContentStrategy 注入平台特定的 prompt 模板和风格要求
 */
public interface AIWriterEngine {

    /**
     * 基于话题和平台策略生成文章
     *
     * @param topic   评分后的话题
     * @param strategy 平台内容策略
     * @return 生成的文章内容 (Markdown)
     */
    WriteResult write(ScoredTopic topic, ContentStrategy strategy);

    /**
     * 生成文章摘要
     */
    String generateSummary(String content, int maxLength);

    /**
     * 检查 AI 服务是否可用
     */
    boolean isAvailable();
}
