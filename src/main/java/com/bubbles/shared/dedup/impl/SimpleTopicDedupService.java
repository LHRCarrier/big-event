package com.bubbles.shared.dedup.impl;

import com.bubbles.modules.core.RawSignal;
import com.bubbles.shared.dedup.TopicDedupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 简易话题去重服务 —— 基于关键词 Jaccard 相似度
 *
 * 初期使用关键词提取 + Jaccard 相似度进行去重。
 * 后续可升级为向量相似度（text-embedding）或 LLM 驱动的话题聚类。
 */
@Slf4j
@Service
public class SimpleTopicDedupService implements TopicDedupService {

    private static final double SIMILARITY_THRESHOLD = 0.75;

    @Override
    public double computeSimilarity(RawSignal signal, List<RawSignal> existingSignals) {
        Set<String> signalWords = extractKeywords(signal.getTitle());

        double maxSimilarity = 0;
        for (RawSignal existing : existingSignals) {
            Set<String> existingWords = extractKeywords(existing.getTitle());
            double similarity = jaccardSimilarity(signalWords, existingWords);
            maxSimilarity = Math.max(maxSimilarity, similarity);
        }

        return maxSimilarity;
    }

    @Override
    public List<RawSignal> deduplicate(List<RawSignal> signals) {
        if (signals.size() <= 1) return new ArrayList<>(signals);

        List<RawSignal> result = new ArrayList<>();
        List<Set<String>> keywordSets = new ArrayList<>();

        for (RawSignal signal : signals) {
            Set<String> words = extractKeywords(signal.getTitle());

            boolean duplicate = false;
            for (int i = 0; i < result.size(); i++) {
                double similarity = jaccardSimilarity(words, keywordSets.get(i));
                if (similarity >= SIMILARITY_THRESHOLD) {
                    duplicate = true;
                    log.debug("[去重] 信号 '{}' 与 '{}' 重复 (相似度={:.2f})",
                            signal.getTitle(), result.get(i).getTitle(), similarity);
                    break;
                }
            }

            if (!duplicate) {
                result.add(signal);
                keywordSets.add(words);
            }
        }

        log.info("[去重] 原始信号 {} 条 → 去重后 {} 条", signals.size(), result.size());
        return result;
    }

    /**
     * 从标题提取关键词（简易分词：按常见分隔符切分，过滤停用词和短词）
     */
    private Set<String> extractKeywords(String title) {
        if (title == null || title.isEmpty()) return Set.of();

        String[] words = title.split("[\\s，,。！!？?、：:；;（）()\\[\\]【】\"'\"'·/\\\\|]+");

        Set<String> keywords = new HashSet<>();
        for (String w : words) {
            w = w.trim().toLowerCase();
            if (w.length() >= 2 && !isStopWord(w)) {
                keywords.add(w);
            }
        }
        return keywords;
    }

    /**
     * Jaccard 相似度: |A ∩ B| / |A ∪ B|
     */
    private double jaccardSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    private boolean isStopWord(String word) {
        return STOP_WORDS.contains(word);
    }

    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "在", "是", "我", "有", "和", "就",
            "不", "人", "都", "一", "一个", "上", "也", "很",
            "到", "说", "要", "去", "你", "会", "着", "没有",
            "看", "好", "自己", "这", "他", "她", "它", "们",
            "那", "什么", "怎么", "为什么", "因为", "所以",
            "可以", "这个", "那个", "哪", "这些", "那些",
            "吗", "吧", "呢", "啊", "哦", "嗯", "哈"
    );
}
