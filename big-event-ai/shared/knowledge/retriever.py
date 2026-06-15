"""
知识库检索器

方案A（当前实现）：jieba 分词 + Jaccard 关键词相似度匹配
方案B（后续升级）：SiliconFlow embedding + ChromaDB 向量检索
"""
import re
from typing import List, Dict, Optional
import jieba
import pymysql
from config import config


class KnowledgeRetriever:
    """从 MySQL 知识库中检索与 topic 相关的文章"""

    def __init__(self):
        self._conn = None

    @property
    def conn(self):
        """懒连接 MySQL"""
        if self._conn is None:
            try:
                self._conn = pymysql.connect(
                    host=config.MYSQL_HOST,
                    port=config.MYSQL_PORT,
                    user=config.MYSQL_USER,
                    password=config.MYSQL_PASSWORD,
                    database=config.MYSQL_DATABASE,
                    charset="utf8mb4",
                    connect_timeout=5,
                )
                print(f"[KnowledgeRetriever] MySQL 连接成功: {config.MYSQL_HOST}:{config.MYSQL_PORT}")
            except Exception as e:
                print(f"[KnowledgeRetriever] MySQL 连接失败: {e}")
                self._conn = None
                raise
        return self._conn

    def _tokenize(self, text: str) -> set:
        """对文本进行 jieba 分词，返回词集合（过滤单字和纯数字）"""
        words = jieba.lcut(text)
        return {w.strip().lower() for w in words
                if len(w.strip()) >= 2 and not w.strip().isdigit()}

    def _jaccard_similarity(self, set1: set, set2: set) -> float:
        """计算两个集合的 Jaccard 相似度"""
        if not set1 or not set2:
            return 0.0
        intersection = len(set1 & set2)
        union = len(set1 | set2)
        return intersection / union if union > 0 else 0.0

    def _load_articles(self, category: Optional[str] = None) -> List[Dict]:
        """从 MySQL 加载所有启用的知识库文章"""
        try:
            if category:
                sql = "SELECT id, title, excerpt, category, tags, content, quality FROM knowledge_article WHERE status = 1 AND category = %s"
                with self.conn.cursor(pymysql.cursors.DictCursor) as cursor:
                    cursor.execute(sql, (category,))
                    return cursor.fetchall()
            else:
                sql = "SELECT id, title, excerpt, category, tags, content, quality FROM knowledge_article WHERE status = 1"
                with self.conn.cursor(pymysql.cursors.DictCursor) as cursor:
                    cursor.execute(sql)
                    return cursor.fetchall()
        except Exception as e:
            print(f"[KnowledgeRetriever] 加载文章失败: {e}")
            return []

    def retrieve(self, topic: str, category: Optional[str] = None,
                 top_k: int = None) -> List[Dict]:
        """
        检索与 topic 最相关的知识库文章

        参数:
            topic: 写作主题 / 标题
            category: 可选，限定分类
            top_k: 返回篇数，默认取 config 配置

        返回:
            List[Dict]: 相关文章列表，每篇含 id/title/excerpt/content/quality/score
        """
        if top_k is None:
            top_k = config.KNOWLEDGE_TOP_K
        min_sim = config.KNOWLEDGE_MIN_SIMILARITY

        # 对 topic 分词
        topic_tokens = self._tokenize(topic)
        if not topic_tokens:
            print(f"[KnowledgeRetriever] topic 分词结果为空: {topic}")
            return []

        # 加载文章
        articles = self._load_articles(category)

        # 计算每篇文章与 topic 的相似度
        scored = []
        for article in articles:
            # 把 title + tags + excerpt 拼起来做匹配
            index_text = f"{article.get('title', '')} {article.get('tags', '')} {article.get('excerpt', '')}"
            article_tokens = self._tokenize(index_text)
            sim = self._jaccard_similarity(topic_tokens, article_tokens)

            if sim >= min_sim:
                article["score"] = round(sim, 4)
                scored.append(article)

        # 按相似度降序 + 质量评分加权
        scored.sort(key=lambda x: (x["score"] * 0.7 + x.get("quality", 3) / 5 * 0.3), reverse=True)

        result = scored[:top_k]
        print(f"[KnowledgeRetriever] 检索完成: topic='{topic}', category={category}, "
              f"候选{len(articles)}篇, 匹配{len(scored)}篇, 返回{len(result)}篇")
        for a in result:
            print(f"  - [{a['category']}] {a['title'][:40]}... score={a['score']}")
        return result


# 全局单例
_retriever_instance = None


def get_retriever() -> KnowledgeRetriever:
    """获取全局 KnowledgeRetriever 实例（懒初始化）"""
    global _retriever_instance
    if _retriever_instance is None:
        _retriever_instance = KnowledgeRetriever()
    return _retriever_instance
