package com.bubbles.shared.dedup;

import com.bubbles.modules.core.RawSignal;

import java.util.List;

/**
 * 跨平台话题去重服务
 *
 * 同一热点事件在不同平台同时出现时，避免生成雷同内容。
 * 初期使用关键词 + 向量相似度方案，后续可升级 LLM 驱动聚类。
 */
public interface TopicDedupService {

    /**
     * 判断新信号是否与已有话题重复
     *
     * @param signal        待检查的信号
     * @param existingSignals 已有的活跃信号列表
     * @return 相似度 (0-1), >= 0.75 视为重复
     */
    double computeSimilarity(RawSignal signal, List<RawSignal> existingSignals);

    /**
     * 在信号列表中识别重复组并去重
     *
     * @param signals 待去重的信号列表
     * @return 去重后的信号列表
     */
    List<RawSignal> deduplicate(List<RawSignal> signals);
}
