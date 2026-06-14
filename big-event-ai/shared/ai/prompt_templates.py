"""
Prompt 模板管理器 —— 跨平台共享的 prompt 模板注册与检索

各平台模块的 ContentStrategy.build_prompt() 可通过此管理器
获取基础模板后做平台特化调整。
"""
from typing import Dict


class PromptTemplateManager:
    """跨平台 Prompt 模板管理"""

    def __init__(self):
        self._templates: Dict[str, str] = {}

    def get(self, platform: str) -> str:
        """获取平台基础模板"""
        return self._templates.get(
            platform,
            "你是一位资深的内容创作者，请根据以下信息撰写一篇高质量文章。",
        )

    def register(self, platform: str, template: str) -> None:
        """注册/更新平台模板"""
        self._templates[platform] = template


# 全局实例
prompt_manager = PromptTemplateManager()
