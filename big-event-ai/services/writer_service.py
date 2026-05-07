"""
AI撰稿核心服务
负责调用AI模型生成文章内容

实现说明：
1. 使用 requests 库直接调用AI接口，绕过OpenAI SDK的代理兼容性问题
2. 支持 SiliconFlow API（DeepSeek等模型）
3. 当AI服务不可用时，返回Mock数据
"""
import uuid
import json
from datetime import datetime
from typing import List, Optional
import requests
from config import config
from schemas.request import WriteArticleRequest
from schemas.response import WriteArticleResponse, ArticleSection

class WriterService:
    """
    AI撰稿服务类
    提供文章生成、摘要生成等功能
    """

    def __init__(self):
        """初始化服务"""
        self.session = requests.Session()
        # 禁用代理
        self.session.trust_env = False

    def _generate_article_content(self, request: WriteArticleRequest) -> str:
        """
        调用AI模型生成文章内容

        参数：
        - request: WriteArticleRequest - 撰稿请求

        返回：
        - str: 生成的文章内容

        实现说明：
        1. 构建prompt提示词
        2. 调用SiliconFlow API生成内容
        3. 处理响应并提取文章内容
        """
        try:
            print(f"[WriterService] 开始调用AI模型: {config.OPENAI_MODEL}")

            # 构建风格描述
            style_map = {
                "neutral": "中立客观",
                "formal": "正式严谨",
                "casual": "轻松活泼",
                "technical": "专业技术"
            }

            audience_map = {
                "general": "普通大众",
                "professional": "专业人士",
                "student": "学生群体"
            }

            # 构建提示词
            system_prompt = f"""你是一位专业的撰稿人，擅长撰写各类话题的文章。
请根据以下要求撰写一篇关于「{request.topic}」的文章：

要求：
1. 文章长度约{request.length}字
2. 风格：{style_map.get(request.style, "中立客观")}
3. 目标受众：{audience_map.get(request.audience, "普通大众")}
4. 结构清晰，有引言、正文和结论
5. 内容准确，逻辑严谨

请直接输出文章内容，使用Markdown格式，不需要额外解释。"""

            # 如果有参考信息，添加到prompt中
            if request.references:
                references_text = "\n参考信息：\n" + "\n".join(request.references)
                system_prompt += references_text

            # 构建API请求
            url = f"{config.OPENAI_BASE_URL}/v1/chat/completions"
            headers = {
                "Authorization": f"Bearer {config.OPENAI_API_KEY}",
                "Content-Type": "application/json"
            }
            payload = {
                "model": config.OPENAI_MODEL,
                "messages": [
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": f"请撰写一篇关于「{request.topic}」的文章。"}
                ],
                "max_tokens": request.length * 2,
                "temperature": 0.7
            }

            print(f"[WriterService] 发送请求到: {url}")

            # 发送请求
            response = self.session.post(
                url,
                headers=headers,
                json=payload,
                timeout=60,
                proxies={"http": None, "https": None}
            )

            print(f"[WriterService] 响应状态码: {response.status_code}")

            if response.status_code == 200:
                result = response.json()
                content = result["choices"][0]["message"]["content"]
                print(f"[WriterService] 成功获取AI生成内容，长度: {len(content)}")
                return content.strip()
            else:
                print(f"[WriterService] API调用失败: {response.status_code} - {response.text}")
                return self._generate_mock_article(request)

        except requests.exceptions.RequestException as e:
            print(f"[WriterService] 网络请求失败: {str(e)}")
            return self._generate_mock_article(request)
        except Exception as e:
            print(f"[WriterService] AI调用出错: {str(e)}")
            return self._generate_mock_article(request)

    def _generate_summary(self, content: str, max_length: int = 150) -> str:
        """
        生成文章摘要

        参数：
        - content: str - 文章内容
        - max_length: int - 摘要最大长度

        返回：
        - str: 文章摘要

        实现说明：
        1. 使用AI生成摘要
        2. 如果失败，使用简单截取方式
        """
        try:
            url = f"{config.OPENAI_BASE_URL}/v1/chat/completions"
            headers = {
                "Authorization": f"Bearer {config.OPENAI_API_KEY}",
                "Content-Type": "application/json"
            }
            payload = {
                "model": config.OPENAI_MODEL,
                "messages": [
                    {"role": "system", "content": "请将以下文章内容生成一篇简洁的摘要，不超过150字。"},
                    {"role": "user", "content": content}
                ],
                "max_tokens": 200,
                "temperature": 0.3
            }

            response = self.session.post(
                url,
                headers=headers,
                json=payload,
                timeout=30,
                proxies={"http": None, "https": None}
            )

            if response.status_code == 200:
                result = response.json()
                return result["choices"][0]["message"]["content"].strip()
            else:
                return content[:max_length] + "..." if len(content) > max_length else content

        except Exception as e:
            print(f"[WriterService] 摘要生成失败: {str(e)}")
            return content[:max_length] + "..." if len(content) > max_length else content

    def _generate_mock_article(self, request: WriteArticleRequest) -> str:
        """
        生成Mock文章内容（用于测试或异常时）

        参数：
        - request: WriteArticleRequest - 撰稿请求

        返回：
        - str: Mock文章内容
        """
        print("[WriterService] 使用Mock模式生成文章")
        mock_content = f"""
# {request.topic}

## 引言

随着时代的发展，{request.topic}已经成为人们关注的重要话题。本文将从多个角度深入探讨{request.topic}的相关内容，帮助读者全面了解这一领域的最新动态。

## 发展现状

目前，{request.topic}领域正处于快速发展阶段。相关技术不断进步，应用场景日益广泛。越来越多的企业和机构开始重视{request.topic}的研究和应用，推动了整个行业的发展。

## 主要挑战

尽管{request.topic}取得了显著进展，但仍然面临一些挑战。例如，技术瓶颈、人才短缺、市场竞争激烈等问题都需要得到妥善解决。

## 未来展望

展望未来，{request.topic}有着广阔的发展前景。随着技术的不断突破和市场的逐步成熟，相信{request.topic}将在更多领域发挥重要作用，为社会发展做出更大贡献。

## 结语

总之，{request.topic}是一个充满机遇和挑战的领域。我们需要持续关注其发展动态，积极探索创新应用，以适应不断变化的市场需求。

---

*本文由AI撰稿系统自动生成*
"""
        return mock_content.strip()

    def _parse_sections(self, content: str) -> List[ArticleSection]:
        """
        将文章内容解析为结构化段落

        参数：
        - content: str - 文章内容（Markdown格式）

        返回：
        - List[ArticleSection]: 结构化段落列表

        实现说明：
        1. 按Markdown标题分割内容
        2. 提取每个段落的标题和内容
        3. 返回结构化列表
        """
        sections = []
        lines = content.split('\n')

        current_title = ""
        current_content = ""

        for line in lines:
            if line.startswith('## '):
                # 新的二级标题
                if current_title:
                    sections.append(ArticleSection(
                        title=current_title,
                        content=current_content.strip()
                    ))
                current_title = line[3:].strip()
                current_content = ""
            elif line.startswith('# '):
                # 一级标题作为文章标题，跳过
                continue
            else:
                current_content += line + "\n"

        # 添加最后一个段落
        if current_title:
            sections.append(ArticleSection(
                title=current_title,
                content=current_content.strip()
            ))

        return sections

    def write_article(self, request: WriteArticleRequest) -> WriteArticleResponse:
        """
        撰写文章（主入口方法）

        参数：
        - request: WriteArticleRequest - 撰稿请求

        返回：
        - WriteArticleResponse: 撰稿响应

        实现说明：
        1. 生成文章内容
        2. 提取文章标题
        3. 生成摘要（如果需要）
        4. 解析段落结构
        5. 返回完整响应
        """
        # 生成文章内容
        content = self._generate_article_content(request)

        # 提取标题（从内容中获取第一个标题）
        title = request.topic
        for line in content.split('\n'):
            if line.startswith('# '):
                title = line[2:].strip()
                break

        # 生成摘要
        summary = None
        if request.generate_summary:
            summary = self._generate_summary(content)

        # 解析段落结构
        sections = self._parse_sections(content)

        # 返回响应
        return WriteArticleResponse(
            article_id=str(uuid.uuid4()),
            title=title,
            content=content,
            summary=summary,
            sections=sections,
            generated_at=datetime.now(),
            model_used=config.OPENAI_MODEL
        )

    def is_available(self) -> bool:
        """
        检查AI服务是否可用

        返回：
        - bool: AI服务是否可用
        """
        return bool(config.OPENAI_API_KEY)

    def stream_article_content(self, request: WriteArticleRequest):
        """
        流式生成文章内容（用于实时显示）

        参数：
        - request: WriteArticleRequest - 撰稿请求

        返回：
        - generator: 生成器，逐块返回文章内容

        实现说明：
        1. 构建prompt提示词
        2. 调用SiliconFlow流式API
        3. 逐块返回生成的内容
        """
        import re

        try:
            print(f"[WriterService] 开始流式调用AI模型: {config.OPENAI_MODEL}")

            # 构建风格描述
            style_map = {
                "neutral": "中立客观",
                "formal": "正式严谨",
                "casual": "轻松活泼",
                "technical": "专业技术"
            }

            audience_map = {
                "general": "普通大众",
                "professional": "专业人士",
                "student": "学生群体"
            }

            # 构建提示词
            system_prompt = f"""你是一位专业的撰稿人，擅长撰写各类话题的文章。
请根据以下要求撰写一篇关于「{request.topic}」的文章：

要求：
1. 文章长度约{request.length}字
2. 风格：{style_map.get(request.style, "中立客观")}
3. 目标受众：{audience_map.get(request.audience, "普通大众")}
4. 结构清晰，有引言、正文和结论
5. 内容准确，逻辑严谨

请直接输出文章内容，使用Markdown格式，不需要额外解释。"""

            # 如果有参考信息，添加到prompt中
            if request.references:
                references_text = "\n参考信息：\n" + "\n".join(request.references)
                system_prompt += references_text

            # 构建API请求
            url = f"{config.OPENAI_BASE_URL}/v1/chat/completions"
            headers = {
                "Authorization": f"Bearer {config.OPENAI_API_KEY}",
                "Content-Type": "application/json"
            }
            payload = {
                "model": config.OPENAI_MODEL,
                "messages": [
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": f"请撰写一篇关于「{request.topic}」的文章。"}
                ],
                "max_tokens": request.length * 2,
                "temperature": 0.7,
                "stream": True
            }

            print(f"[WriterService] 发送流式请求到: {url}")

            # 发送流式请求
            response = self.session.post(
                url,
                headers=headers,
                json=payload,
                timeout=120,
                proxies={"http": None, "https": None},
                stream=True
            )

            print(f"[WriterService] 流式响应状态码: {response.status_code}")

            if response.status_code == 200:
                for line in response.iter_lines(chunk_size=1024):
                    if line:
                        line = line.decode('utf-8').strip()
                        # 解析SSE格式
                        if line.startswith('data: '):
                            data = line[5:]
                            if data == '[DONE]':
                                print("[WriterService] 流式传输完成")
                                break
                            try:
                                result = json.loads(data)
                                if 'choices' in result and len(result['choices']) > 0:
                                    delta = result['choices'][0].get('delta', {})
                                    content = delta.get('content', '')
                                    if content:
                                        print(f"[WriterService] 收到chunk: {len(content)} chars")
                                        yield content
                            except json.JSONDecodeError:
                                continue
            else:
                print(f"[WriterService] API调用失败: {response.status_code}")
                # 返回mock内容
                mock_content = self._generate_mock_article(request)
                # 模拟流式返回
                chunks = re.findall(r'.{1,50}', mock_content)
                for chunk in chunks:
                    yield chunk
                    import time
                    time.sleep(0.05)

        except Exception as e:
            print(f"[WriterService] 流式调用失败: {str(e)}")
            # 返回mock内容
            mock_content = self._generate_mock_article(request)
            chunks = re.findall(r'.{1,50}', mock_content)
            for chunk in chunks:
                yield chunk
                import time
                time.sleep(0.05)

# 创建全局服务实例
writer_service = WriterService()