"""
跨平台话题去重服务

同一热点事件在不同平台同时出现时，避免生成雷同内容。
初期使用关键词 Jaccard 相似度方案，后续可升级为向量相似度或 LLM 聚类。
"""
from typing import List, Set

# 中文停用词
STOP_WORDS: Set[str] = {
    "的", "了", "在", "是", "我", "有", "和", "就",
    "不", "人", "都", "一", "一个", "上", "也", "很",
    "到", "说", "要", "去", "你", "会", "着", "没有",
    "看", "好", "自己", "这", "他", "她", "它", "们",
    "那", "什么", "怎么", "为什么", "因为", "所以",
    "可以", "这个", "那个", "哪", "这些", "那些",
    "吗", "吧", "呢", "啊", "哦", "嗯", "哈",
}


def extract_keywords(title: str) -> Set[str]:
    """从标题提取关键词（简易分词）"""
    if not title:
        return set()
    # 按标点和空格切分
    import re
    words = re.split(r"[\s，,。！!？?、：:；;（）()\[\]【】\"'\"'·/\\|]+", title)
    return {
        w.strip().lower()
        for w in words
        if len(w.strip()) >= 2 and w.strip().lower() not in STOP_WORDS
    }


def jaccard_similarity(a: Set[str], b: Set[str]) -> float:
    """Jaccard 相似度: |A ∩ B| / |A ∪ B|"""
    if not a and not b:
        return 1.0
    intersection = a & b
    union = a | b
    return len(intersection) / len(union) if union else 0.0


class TopicDedupService:
    """跨平台话题去重服务"""

    SIMILARITY_THRESHOLD = 0.75

    def is_duplicate(self, title: str, existing_titles: List[str]) -> bool:
        """检查标题是否与已有话题重复"""
        keywords = extract_keywords(title)
        for existing in existing_titles:
            existing_kw = extract_keywords(existing)
            if jaccard_similarity(keywords, existing_kw) >= self.SIMILARITY_THRESHOLD:
                return True
        return False

    def deduplicate(self, titles: List[str]) -> List[str]:
        """去重标题列表"""
        result = []
        for title in titles:
            if not self.is_duplicate(title, result):
                result.append(title)
        return result
