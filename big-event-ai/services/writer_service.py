"""
AI撰稿核心服务
负责调用AI模型生成文章内容

实现说明：
1. 使用 requests 库直接调用AI接口，绕过OpenAI SDK的代理兼容性问题
2. 支持 SiliconFlow API（DeepSeek等模型）
3. 当AI服务不可用时，返回Mock数据
4. Phase 2: 集成知识库检索，为文章注入风格参考，减少AI味
"""
import uuid
import json
from datetime import datetime
from typing import List, Optional
import requests
from config import config
from schemas.request import WriteArticleRequest, WriteFromHotRequest
from schemas.response import WriteArticleResponse, ArticleSection

# 知识库模块
try:
    from shared.knowledge.retriever import get_retriever
    from shared.knowledge.prompt_builder import (
        build_prompt_with_knowledge,
        build_hot_prompt_with_knowledge,
    )
    _knowledge_available = True
except ImportError as e:
    print(f"[WriterService] 知识库模块加载失败: {e}")
    _knowledge_available = False

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
        1. 构建prompt提示词（优先使用知识库注入）
        2. 调用SiliconFlow API生成内容
        3. 处理响应并提取文章内容
        """
        try:
            print(f"[调用链-3/4] WriterService 构建prompt + 调用AI: model={config.OPENAI_MODEL}")

            # 构建风格描述
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

            # 构建提示词（优先使用知识库注入）
            extra_context = ""
            if request.references:
                extra_context = "参考信息：\n" + "\n".join(request.references)

            knowledge_articles = []
            if _knowledge_available and getattr(request, 'use_knowledge', True):
                try:
                    retriever = get_retriever()
                    knowledge_articles = retriever.retrieve(
                        topic=request.topic,
                        top_k=config.KNOWLEDGE_TOP_K
                    )
                except Exception as e:
                    print(f"[WriterService] 知识库检索跳过: {e}")

            if knowledge_articles:
                system_prompt = build_prompt_with_knowledge(
                    topic=request.topic,
                    length=request.length,
                    style=request.style or "neutral",
                    audience=request.audience or "general",
                    knowledge_articles=knowledge_articles,
                    extra_context=extra_context
                )
                print(f"[调用链-3/4] 知识库注入: {len(knowledge_articles)}篇参考, prompt总长度={len(system_prompt)}")
            else:
                print(f"[调用链-3/4] 知识库无匹配, 使用默认prompt")
                length_req = f"文章长度约{request.length}字" if request.length else "文章长度不限（最长5000字）"
                system_prompt = f"""你是一位专业的撰稿人，擅长撰写各类话题的文章。
请根据以下要求撰写一篇关于「{request.topic}」的文章：

要求：
1. {length_req}
2. 风格：{style_map.get(request.style, "中立客观")}
3. 目标受众：{audience_map.get(request.audience, "普通大众")}
4. 结构清晰，有引言、正文和结论
5. 内容准确，逻辑严谨

请直接输出文章内容，使用Markdown格式，不需要额外解释。"""
                if extra_context:
                    system_prompt += "\n" + extra_context

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
                "max_tokens": (request.length or 5000) * 2,
                "temperature": 0.7
            }

            print(f"[WriterService] 发送请求到: {url}")

            # 发送请求
            response = self.session.post(
                url,
                headers=headers,
                json=payload,
                timeout=300,
                proxies={"http": None, "https": None}
            )

            print(f"[WriterService] 响应状态码: {response.status_code}")

            if response.status_code == 200:
                result = response.json()
                content = result["choices"][0]["message"]["content"]
                usage = result.get("usage", {})
                print(f"[调用链-3/4] AI返回成功: contentLen={len(content)}, promptTokens={usage.get('prompt_tokens','?')}, completionTokens={usage.get('completion_tokens','?')}")
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
                timeout=180,
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

    def _build_hot_article_prompt(self, request: WriteFromHotRequest) -> str:
        """
        构建包含热点上下文信息的系统提示词

        核心改进：将视频简介(desc)作为主要素材来源，让 AI 基于真实内容展开，
        而非仅凭标题臆测。简介提供事实和细节，标题和互动数据提供选题热度佐证。

        Phase 2: 集成知识库风格注入。
        """
        # 先尝试知识库注入
        if _knowledge_available and getattr(request, 'use_knowledge', True):
            try:
                retriever = get_retriever()
                knowledge_articles = retriever.retrieve(
                    topic=request.title,
                    category=request.partition,
                    top_k=config.KNOWLEDGE_TOP_K
                )
                if knowledge_articles:
                    prompt = build_hot_prompt_with_knowledge(
                        title=request.title,
                        partition=request.partition,
                        author=request.author,
                        view_count=request.view_count,
                        like_count=request.like_count,
                        favorite_count=request.favorite_count,
                        share_count=request.share_count,
                        hot_score=request.hot_score,
                        description=request.description or "",
                        length=request.length,
                        style=request.style or "neutral",
                        audience=request.audience or "general",
                        knowledge_articles=knowledge_articles
                    )
                    print(f"[调用链-3/4] 知识库注入: {len(knowledge_articles)}篇参考, prompt总长度={len(prompt)}")
                    return prompt
            except Exception as e:
                print(f"[WriterService] 热点知识库检索跳过: {e}")

        # 回退到原始 prompt
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

        if request.description and len(request.description.strip()) >= 20:
            material_section = f"""## 核心素材（文章必须基于以下内容展开）
{request.description}

以上是该热点事件的核心信息/简介，是撰写文章的主要素材来源。
请仔细阅读，从中提取关键事实、数据、观点，据此构建文章的主体内容。"""
        else:
            material_section = f"""## 选题信息（请围绕该话题展开文章）
- 热点标题：{request.title}
- 注意：该话题缺乏详细素材，请基于你的知识库进行客观专业的介绍和分析"""

        length_req = f"约 {request.length} 字" if request.length else "不限（最长5000字）"
        prompt = f"""你是一位专业的自媒体撰稿人，擅长基于热点事件撰写深度文章。

## 写作要求
- 文章长度：{length_req}
- 风格：{style_map.get(request.style, "中立客观")}
- 目标受众：{audience_map.get(request.audience, "普通大众")}
- 文章需要有吸引力的标题、引言、正文分论点（2-3个）、结语
- 使用 Markdown 格式，正文适度使用列表和加粗，让文章更易读

## 选题背景
- 选题方向：{request.title}
- 所属领域：{request.partition}
- UP主/作者：{request.author}
- 数据表现：播放 {request.view_count}，点赞 {request.like_count}，收藏 {request.favorite_count}，分享 {request.share_count}
- 综合热度评分：{request.hot_score}（满分100）

{material_section}

## 注意事项
- 不要简单复述视频内容，要从事件出发做深度分析或观点输出
- 不要在文中出现"根据B站视频"、"UP主说"、"该视频"、"视频简介显示"之类暴露来源的话术，用"近期"、"据了解"、"有观点认为"、"资料显示"等表达
- 文中可适度加入对同类现象或行业的横向对比，增加文章深度
- 观点要有理有据，避免空洞的情绪化表达
- 不要复读简介内容，要在其基础上补充分析、背景和见解"""

        return prompt

    def write_article_from_hot(self, request: WriteFromHotRequest) -> WriteArticleResponse:
        """
        基于热点数据撰写文章

        与 write_article 的区别：
        - prompt 包含完整的热点上下文（播放量、互动数、排名等），文章质量更高
        - 使用热点标题而非简单 topic 作为写作主题
        """
        try:
            print(f"[WriterService] 开始基于热点撰写文章: {request.title}")

            url = f"{config.OPENAI_BASE_URL}/v1/chat/completions"
            headers = {
                "Authorization": f"Bearer {config.OPENAI_API_KEY}",
                "Content-Type": "application/json"
            }

            system_prompt = self._build_hot_article_prompt(request)

            payload = {
                "model": config.OPENAI_MODEL,
                "messages": [
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": f"请基于以下热点话题撰写一篇文章：{request.title}"}
                ],
                "max_tokens": request.length * 2,
                "temperature": 0.7
            }

            print(f"[WriterService] 发送热点撰稿请求, model={config.OPENAI_MODEL}")

            response = self.session.post(
                url,
                headers=headers,
                json=payload,
                timeout=300,
                proxies={"http": None, "https": None}
            )

            if response.status_code == 200:
                result = response.json()
                content = result["choices"][0]["message"]["content"]
                usage = result.get("usage", {})
                print(f"[调用链-3/4] AI热点返回成功: contentLen={len(content)}, promptTokens={usage.get('prompt_tokens','?')}, completionTokens={usage.get('completion_tokens','?')}")

                # 提取标题
                title = request.title
                for line in content.split('\n'):
                    if line.startswith('# '):
                        title = line[2:].strip()
                        break

                # 生成摘要
                summary = None
                if request.generate_summary:
                    summary = self._generate_summary(content)

                sections = self._parse_sections(content)

                return WriteArticleResponse(
                    article_id=str(uuid.uuid4()),
                    title=title,
                    content=content,
                    summary=summary,
                    sections=sections,
                    generated_at=datetime.now(),
                    model_used=config.OPENAI_MODEL
                )
            else:
                print(f"[WriterService] 热点撰稿API失败: {response.status_code}")
                # fallback: 退回到普通 mock，使用热点标题
                fallback_req = WriteArticleRequest(
                    topic=request.title,
                    length=request.length,
                    style=request.style,
                    audience=request.audience,
                    generate_summary=request.generate_summary
                )
                return self.write_article(fallback_req)

        except Exception as e:
            print(f"[WriterService] 热点撰稿异常: {str(e)}")
            fallback_req = WriteArticleRequest(
                topic=request.title,
                length=request.length,
                style=request.style,
                audience=request.audience,
                generate_summary=request.generate_summary
            )
            return self.write_article(fallback_req)

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

            # 构建提示词（优先使用知识库注入）
            extra_context = ""
            if request.references:
                extra_context = "参考信息：\n" + "\n".join(request.references)

            knowledge_articles = []
            if _knowledge_available and getattr(request, 'use_knowledge', True):
                try:
                    retriever = get_retriever()
                    knowledge_articles = retriever.retrieve(
                        topic=request.topic,
                        top_k=config.KNOWLEDGE_TOP_K
                    )
                except Exception as e:
                    print(f"[WriterService] 知识库检索跳过: {e}")

            if knowledge_articles:
                system_prompt = build_prompt_with_knowledge(
                    topic=request.topic,
                    length=request.length,
                    style=request.style or "neutral",
                    audience=request.audience or "general",
                    knowledge_articles=knowledge_articles,
                    extra_context=extra_context
                )
                print(f"[WriterService] 流式撰稿已注入 {len(knowledge_articles)} 篇知识库参考文章")
            else:
                length_req = f"文章长度约{request.length}字" if request.length else "文章长度不限（最长5000字）"
                system_prompt = f"""你是一位专业的撰稿人，擅长撰写各类话题的文章。
请根据以下要求撰写一篇关于「{request.topic}」的文章：

要求：
1. {length_req}
2. 风格：{style_map.get(request.style, "中立客观")}
3. 目标受众：{audience_map.get(request.audience, "普通大众")}
4. 结构清晰，有引言、正文和结论
5. 内容准确，逻辑严谨

请直接输出文章内容，使用Markdown格式，不需要额外解释。"""
                if extra_context:
                    system_prompt += "\n" + extra_context

            # 构建API请求
            max_tokens = (request.length or 5000) * 2
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
                "max_tokens": max_tokens,
                "temperature": 0.7,
                "stream": True
            }

            print(f"[WriterService] 发送流式请求到: {url}")

            # 发送流式请求
            response = self.session.post(
                url,
                headers=headers,
                json=payload,
                timeout=600,
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