package com.bubbles.modules.bilibili.strategy;

import com.bubbles.modules.core.ContentStrategy;
import com.bubbles.modules.core.ScoredTopic;
import org.springframework.stereotype.Component;

/**
 * B站内容策略 —— B站专栏的内容生成策略
 *
 * B站用户偏好深度/可视化内容，专栏最佳长度 2000-4000 字。
 */
@Component
public class BilibiliContentStrategy implements ContentStrategy {

    @Override
    public String platformName() {
        return "bilibili";
    }

    @Override
    public int minLength() {
        return 2000;
    }

    @Override
    public int maxLength() {
        return 4000;
    }

    @Override
    public String styleGuide() {
        return """
                ## 风格要求
                - 深度分析型内容，数据驱动，有理有据
                - 可视化思维：善用图表、数据对比、时间线
                - 语气专业但不枯燥，可适当使用网络流行语拉近距离
                - 避免标题党，内容必须兑现标题承诺
                - B站用户偏年轻，可用弹幕文化常用梗增加亲和力
                """;
    }

    @Override
    public String formatGuide() {
        return """
                ## 排版要求
                - 使用 Markdown 格式
                - 开头用引言/摘要吸引读者（3-5句话）
                - 正文分4-6个大段，每段有小标题
                - 关键数据用加粗或引用块突出
                - 结尾有总结和互动引导（点赞/投币/收藏）
                - 支持插入图片位置标记 [图片: 描述]
                """;
    }

    @Override
    public String buildPrompt(ScoredTopic topic) {
        var signal = topic.getSignal();
        var metrics = signal.getRawMetrics();

        return String.format("""
                        你是一位资深的B站专栏作者，擅长撰写深度分析类内容。

                        请根据以下热点信息撰写一篇B站专栏文章：

                        【视频标题】%s
                        【UP主】%s
                        【分区】%s
                        【播放量】%d | 点赞: %d | 投币: %d | 收藏: %d | 分享: %d
                        【弹幕数】%d | 评论数: %d
                        【综合热度分】%.1f

                        %s

                        %s

                        【文章要求】
                        - 文章长度: %d-%d 字
                        - 不要简单复述视频内容，要找到独特视角做深度分析
                        - 引用视频中的关键观点和数据，但要有自己的判断和延伸
                        - 适合在B站专栏区阅读
                        """,
                signal.getTitle(),
                signal.getAuthor(),
                signal.getCategory(),
                getLong(metrics, "viewCount"),
                getLong(metrics, "likeCount"),
                getLong(metrics, "coinCount"),
                getLong(metrics, "favoriteCount"),
                getLong(metrics, "shareCount"),
                getLong(metrics, "danmakuCount"),
                getLong(metrics, "replyCount"),
                topic.getScore(),
                styleGuide(),
                formatGuide(),
                minLength(), maxLength()
        );
    }

    private long getLong(java.util.Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.longValue();
        return 0L;
    }
}
