"""
B站内容策略 —— B站专栏的内容生成配置

B站用户偏好深度/可视化内容，专栏最佳长度 2000-4000 字。

Phase 2: 集成知识库风格注入。
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

    STYLE_RED_LINES = """
## 风格红线（必须遵守）
- 禁止使用"在当今社会""随着时代的发展""众所周知""不可否认""综上所述"等套话
- 禁止"首先...其次...再次...最后..."式机械化段落衔接
- 禁止过于工整的对仗和排比
- 鼓励用具体数字代替模糊描述，鼓励口语化表达和个人观点
"""

    def build_prompt(self, scored: dict) -> str:
        """构建 B站专栏撰稿 prompt（含知识库风格注入）"""
        item = scored["item"]

        # 知识库检索
        knowledge_text = ""
        try:
            from shared.knowledge.retriever import get_retriever
            retriever = get_retriever()
            articles = retriever.retrieve(
                topic=item.title,
                category=item.category,
                top_k=2
            )
            if articles:
                ref_blocks = []
                for i, a in enumerate(articles):
                    content = a.get("content", "")[:800]
                    ref_blocks.append(
                        f"### 参考文章 {i + 1}：{a.get('title', '')}\n{content}"
                    )
                knowledge_text = (
                    "## 写作风格参考\n\n"
                    "以下是你需要仔细研究并模仿的写作范例。请重点参考其：\n"
                    "- 语言节奏和句式结构\n"
                    "- 段落组织方式\n"
                    "- 语气和态度\n"
                    "- 数据呈现方式\n\n"
                    + "\n\n---\n\n".join(ref_blocks)
                )
                print(f"[BilibiliStrategy] 已注入 {len(articles)} 篇知识库参考文章")
        except Exception as e:
            print(f"[BilibiliStrategy] 知识库检索跳过: {e}")

        prompt = f"""你是一位资深的B站专栏作者，擅长撰写深度分析类内容。

请根据以下热点信息撰写一篇B站专栏文章：

【视频标题】{item.title}
【UP主】{item.author}
【分区】{item.category}
【播放量】{item.view_count} | 点赞: {item.like_count} | 投币: {item.coin_count} | 收藏: {item.favorite_count} | 分享: {item.share_count}
【弹幕数】{item.danmaku_count} | 评论数: {item.reply_count}
【综合热度分】{scored['score']}

{self.STYLE_GUIDE}

{self.FORMAT_GUIDE}

{self.STYLE_RED_LINES}
"""
        if knowledge_text:
            prompt += f"\n{knowledge_text}\n"

        prompt += f"""【文章要求】
- 文章长度: {self.MIN_LENGTH}-{self.MAX_LENGTH} 字
- 不要简单复述视频内容，要找到独特视角做深度分析
- 引用视频中的关键观点和数据，但要有自己的判断和延伸
- 适合在B站专栏区阅读
"""
        return prompt
