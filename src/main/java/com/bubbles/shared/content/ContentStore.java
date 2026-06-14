package com.bubbles.shared.content;

import com.bubbles.modules.core.ScoredTopic;

/**
 * 内容资产库 —— 统一管理所有平台生成的文章
 *
 * 所有平台模块撰稿完成后，统一通过 ContentStore 入库。
 * article 表附带 platform 字段区分来源。
 */
public interface ContentStore {

    /**
     * 保存文章
     *
     * @param topic    来源话题
     * @param platform 平台标识
     * @param title    文章标题
     * @param content  文章内容 (Markdown)
     * @param coverUrl 封面图URL
     * @param state    初始状态 (草稿/已发布)
     * @return 文章ID
     */
    Long saveArticle(ScoredTopic topic, String platform, String title,
                     String content, String coverUrl, String state);

    /**
     * 检查指定话题是否已被撰稿（避免重复）
     */
    boolean isAlreadyProcessed(String sourceId, String platform);
}
