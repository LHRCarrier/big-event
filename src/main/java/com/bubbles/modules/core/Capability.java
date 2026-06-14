package com.bubbles.modules.core;

/**
 * 平台模块支持的能力类型
 */
public enum Capability {
    CRAWL,      // 热点采集
    SCORE,      // 话题评分
    GENERATE,   // 内容生成
    PUBLISH,    // 发布内容
    SCHEDULE,   // 定时发布
    EDIT,       // 编辑已发布内容
    DELETE,     // 删除已发布内容
    ANALYTICS   // 数据分析
}
