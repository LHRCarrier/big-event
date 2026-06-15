"""
知识库 Prompt 构建器

核心原则：知识库文章作为"风格锚点"注入 prompt，而非信息来源。
AI 模仿参考文章的语气、节奏、结构，从而减少 AI 味。
"""
from typing import List, Dict


# 每篇参考文章注入 prompt 的最大字符数
MAX_REF_CHARS = 800

# 风格红线 —— 禁止 AI 使用的套话
STYLE_RED_LINES = """## 风格红线（必须遵守）

禁止使用以下套话和写法：
- "在当今社会""随着时代的发展""众所周知""不可否认""综上所述"
- "首先...其次...再次...最后..." 式的机械化段落衔接
- 过于工整的对仗句和排比句（如"既是...也是...更是..."）
- "在一定程度上""某种意义上""大体而言" 等模糊限定词堆砌

鼓励的写法：
- 用具体数字代替"很多""最近""大量"
- 适度口语化和个人观点表达
- 长短句交替，避免一律长句或一律短句
- 开头可以用设问、反常识观点、具体场景切入，不要"随着...的发展"起笔"""


def _format_ref_articles(articles: List[Dict]) -> str:
    """将参考文章格式化为 prompt 可用的风格参考块"""
    if not articles:
        return ""

    blocks = []
    for i, a in enumerate(articles):
        content = a.get("content", "")
        # 截断过长的内容
        display_content = content[:MAX_REF_CHARS]
        if len(content) > MAX_REF_CHARS:
            display_content += "\n...(后续内容省略)"

        blocks.append(f"""### 参考文章 {i + 1}：{a.get('title', '')}
分类：{a.get('category', '未知')} | 标签：{a.get('tags', '无')}

{display_content}""")

    header = f"""## 写作风格参考（共 {len(articles)} 篇）

以下是你需要仔细研究并模仿的写作范例。请重点参考：
- 语言节奏和句式结构
- 段落组织方式（如何引入话题、如何展开、如何收尾）
- 语气和态度
- 数据和细节的呈现方式

---"""
    return header + "\n\n" + "\n\n---\n\n".join(blocks)


def build_prompt_with_knowledge(topic: str, length: int, style: str,
                                 audience: str, knowledge_articles: List[Dict],
                                 extra_context: str = "") -> str:
    """
    构建通用撰稿的 system prompt（含知识库风格注入）

    参数:
        topic: 写作主题
        length: 文章字数
        style: 风格 (neutral/formal/casual/technical)
        audience: 目标受众 (general/professional/student)
        knowledge_articles: 知识库检索返回的相关文章列表
        extra_context: 额外的上下文信息（如 references）

    返回:
        str: 完整的 system prompt
    """
    style_map = {
        "neutral": "中立客观",
        "formal": "正式严谨",
        "casual": "轻松活泼",
        "literary": "文学诗意，多用比喻、意象和优美的语言",
        "journalistic": "新闻纪实，客观真实，注重事实和数据",
        "sharp": "犀利锐评，观点鲜明，语言精炼有力",
        "technical": "专业技术"
    }
    audience_map = {
        "general": "普通大众",
        "professional": "专业人士",
        "student": "学生群体"
    }

    style_text = style_map.get(style, "中立客观")
    audience_text = audience_map.get(audience, "普通大众")
    length_text = f"约 {length} 字" if length else "不限（最长5000字）"

    # 开头：风格参考（如果有知识库文章）
    ref_section = _format_ref_articles(knowledge_articles)

    prompt_parts = [
        "你是一位专业的撰稿人，擅长撰写各类话题的文章。",
        ref_section,
        STYLE_RED_LINES,
        f"""## 写作任务

- 主题：{topic}
- 字数：{length_text}
- 风格：{style_text}
- 目标读者：{audience_text}
- 结构清晰，有引言、正文和结论
- 内容准确，逻辑严谨

请直接输出文章内容，使用 Markdown 格式，不需要额外解释。""",
    ]

    if extra_context:
        prompt_parts.append(f"\n{extra_context}")

    return "\n\n".join(p for p in prompt_parts if p)


def build_hot_prompt_with_knowledge(
    title: str, partition: str, author: str,
    view_count, like_count, favorite_count, share_count,
    hot_score: float, description: str,
    length: int, style: str, audience: str,
    knowledge_articles: List[Dict]
) -> str:
    """
    构建热点撰稿的 system prompt（含知识库风格注入 + 热点上下文）

    参数:
        title/partition/author/...: 热点信息
        description: 视频简介（核心素材）
        length/style/audience: 写作要求
        knowledge_articles: 知识库检索返回的相关文章列表

    返回:
        str: 完整的 system prompt
    """
    style_map = {
        "neutral": "中立客观，新闻体风格",
        "formal": "正式严谨，深度分析风格",
        "casual": "轻松活泼，自媒体风格",
        "literary": "文学诗意，散文风格，多用比喻和意象",
        "journalistic": "新闻纪实，客观真实，事实+数据驱动",
        "sharp": "犀利锐评，观点鲜明，语言精炼，有态度",
        "technical": "专业技术，行业洞察风格"
    }
    audience_map = {
        "general": "普通大众",
        "professional": "专业人士",
        "student": "学生群体"
    }

    style_text = style_map.get(style, "中立客观")
    audience_text = audience_map.get(audience, "普通大众")
    length_text = f"约 {length} 字" if length else "不限（最长5000字）"

    # 素材段
    if description and len(description.strip()) >= 20:
        material_section = f"""## 核心素材（文章必须基于以下内容展开）
{description}

以上是该热点事件的核心信息/简介，是撰写文章的主要素材来源。
请仔细阅读，从中提取关键事实、数据、观点，据此构建文章的主体内容。"""
    else:
        material_section = f"""## 选题信息（请围绕该话题展开文章）
- 热点标题：{title}
- 注意：该话题缺乏详细素材，请基于你的知识库进行客观专业的介绍和分析"""

    ref_section = _format_ref_articles(knowledge_articles)

    prompt = f"""你是一位专业的自媒体撰稿人，擅长基于热点事件撰写深度文章。

{ref_section}

{STYLE_RED_LINES}

## 写作要求
- 文章长度：{length_text}
- 风格：{style_text}
- 目标受众：{audience_text}
- 文章需要有吸引力的标题、引言、正文分论点（2-3个）、结语
- 使用 Markdown 格式，正文适度使用列表和加粗，让文章更易读

## 选题背景
- 选题方向：{title}
- 所属领域：{partition}
- UP主/作者：{author}
- 数据表现：播放 {view_count}，点赞 {like_count}，收藏 {favorite_count}，分享 {share_count}
- 综合热度评分：{hot_score}（满分100）

{material_section}

## 注意事项
- 不要简单复述视频内容，要从事件出发做深度分析或观点输出
- 不要在文中出现"根据B站视频"、"UP主说"、"该视频"、"视频简介显示"之类暴露来源的话术，用"近期"、"据了解"、"有观点认为"、"资料显示"等表达
- 文中可适度加入对同类现象或行业的横向对比，增加文章深度
- 观点要有理有据，避免空洞的情绪化表达
- 不要复读简介内容，要在其基础上补充分析、背景和见解"""

    return prompt
