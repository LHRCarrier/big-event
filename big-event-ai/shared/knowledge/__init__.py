"""
知识库模块 — 为 AI 撰稿提供风格参考和事实素材

- retriever: 关键词检索，从 MySQL 知识库中召回相关文章
- prompt_builder: 将召回文章注入 prompt，作为风格锚点
"""
from .retriever import KnowledgeRetriever
from .prompt_builder import build_prompt_with_knowledge, build_hot_prompt_with_knowledge

__all__ = [
    "KnowledgeRetriever",
    "build_prompt_with_knowledge",
    "build_hot_prompt_with_knowledge",
]
