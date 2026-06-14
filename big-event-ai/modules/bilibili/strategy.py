"""
B站内容策略 —— B站专栏的内容生成配置

B站用户偏好深度/可视化内容，专栏最佳长度 2000-4000 字。
"""
from typing import Dict


class BilibiliContentStrategy:
    """B站内容生成策略"""

    PLATFORM = "bilibili"
    MIN_LENGTH = 2000
    MAX_LENGTH = 4000

    STYLE_GUIDE = """
## 风格要求
- 深度分析型内容，数据驱动，有理有据
- 可视化思维：善用图表、数据对比、时间线
- 语气专业但不枯燥，可适当使用网络流行语拉近距离
- 避免标题党，内容必须兑现标题承诺
- B站用户偏年轻，可用弹幕文化常用梗增加亲和力
"""

    FORMAT_GUIDE = """
## 排版要求
- 使用 Markdown 格式
- 开头用引言/摘要吸引读者（3-5句话）
- 正文分4-6个大段，每段有小标题
- 关键数据用加粗或引用块突出
- 结尾有总结和互动引导（点赞/投币/收藏）
- 支持插入图片位置标记 [图片: 描述]
"""

    def build_prompt(self, scored: dict) -> str:
        """构建 B站专栏撰稿 prompt"""
        item = scored["item"]
        return f"""你是一位资深的B站专栏作者，擅长撰写深度分析类内容。

请根据以下热点信息撰写一篇B站专栏文章：

【视频标题】{item.title}
【UP主】{item.author}
【分区】{item.category}
【播放量】{item.view_count} | 点赞: {item.like_count} | 投币: {item.coin_count} | 收藏: {item.favorite_count} | 分享: {item.share_count}
【弹幕数】{item.danmaku_count} | 评论数: {item.reply_count}
【综合热度分】{scored['score']}

{self.STYLE_GUIDE}

{self.FORMAT_GUIDE}

【文章要求】
- 文章长度: {self.MIN_LENGTH}-{self.MAX_LENGTH} 字
- 不要简单复述视频内容，要找到独特视角做深度分析
- 引用视频中的关键观点和数据，但要有自己的判断和延伸
- 适合在B站专栏区阅读
"""
