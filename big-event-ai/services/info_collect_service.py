"""
信息搜集服务
负责搜集话题相关的信息并进行处理过滤
"""
import uuid
from datetime import datetime
from typing import List
from schemas.request import CollectInfoRequest
from schemas.response import CollectInfoResponse, InfoItem

class InfoCollectService:
    """
    信息搜集服务类
    提供信息搜索、分类、过滤等功能
    """
    
    def __init__(self):
        """初始化服务"""
        # Mock信息数据（实际应用中从搜索API或新闻源获取）
        self.mock_info_sources = {
            "default": [
                {"title": "话题相关新闻报道", "source": "新闻网站A", "summary": "这是一篇关于该话题的详细新闻报道，包含最新动态和专家分析。", "relevance": 95, "url": "https://example.com/news1"},
                {"title": "行业分析报告", "source": "行业研究机构", "summary": "专业机构发布的行业分析报告，数据详实，观点独到。", "relevance": 88, "url": "https://example.com/report1"},
                {"title": "专家观点解读", "source": "知名媒体", "summary": "业内专家对该话题的深度解读和未来展望。", "relevance": 92, "url": "https://example.com/opinion1"},
                {"title": "用户讨论热点", "source": "社交媒体", "summary": "社交媒体上用户的热议内容和不同观点碰撞。", "relevance": 78, "url": "https://example.com/social1"},
                {"title": "历史数据分析", "source": "数据平台", "summary": "基于历史数据的趋势分析和预测。", "relevance": 85, "url": "https://example.com/data1"},
            ]
        }
    
    def _search_info(self, topic: str, count: int, info_type: str) -> List[dict]:
        """
        搜索话题相关信息
        
        参数：
        - topic: str - 话题关键词
        - count: int - 返回数量
        - info_type: str - 信息类型
        
        返回：
        - List[dict]: 搜索结果列表
        
        实现说明：
        1. 根据话题关键词搜索相关信息
        2. 支持不同类型的信息源
        3. 返回指定数量的结果
        """
        # 获取基础Mock数据
        base_info = self.mock_info_sources.get("default", [])
        
        # 根据信息类型筛选
        if info_type == "news":
            filtered = [item for item in base_info if "新闻" in item["source"] or "报道" in item["title"]]
        elif info_type == "article":
            filtered = [item for item in base_info if "报告" in item["source"] or "分析" in item["title"]]
        elif info_type == "social":
            filtered = [item for item in base_info if "社交" in item["source"]]
        else:
            filtered = base_info
        
        # 添加话题相关信息
        results = []
        for i, item in enumerate(filtered[:count]):
            results.append({
                **item,
                "topic": topic,
                "unique_id": str(uuid.uuid4()),
                "publish_time": datetime.now(),
                # 根据索引调整相关度，模拟真实搜索结果
                "relevance": max(60, item["relevance"] - i * 3)
            })
        
        return results
    
    def _filter_and_rank(self, info_list: List[dict], min_relevance: int = 60) -> List[dict]:
        """
        过滤和排序信息
        
        参数：
        - info_list: List[dict] - 原始信息列表
        - min_relevance: int - 最低相关度阈值
        
        返回：
        - List[dict]: 过滤排序后的列表
        
        实现说明：
        1. 过滤相关度低于阈值的信息
        2. 去除重复内容
        3. 按相关度排序
        """
        # 过滤低相关度
        filtered = [item for item in info_list if item["relevance"] >= min_relevance]
        
        # 去重（基于标题）
        seen_titles = set()
        unique_list = []
        for item in filtered:
            if item["title"] not in seen_titles:
                seen_titles.add(item["title"])
                unique_list.append(item)
        
        # 按相关度排序
        unique_list.sort(key=lambda x: x["relevance"], reverse=True)
        
        return unique_list
    
    def _validate_relevance(self, info_list: List[dict], topic: str) -> List[dict]:
        """
        验证信息相关性
        
        参数：
        - info_list: List[dict] - 信息列表
        - topic: str - 话题关键词
        
        返回：
        - List[dict]: 验证后的信息列表
        
        实现说明：
        1. 使用AI进行语义分析验证
        2. 确保信息与话题真正相关
        3. 更新相关度评分
        """
        # TODO: 实现AI语义分析验证
        # 可以调用AI模型判断信息与话题的相关性
        # 当前使用简单的关键词匹配作为替代
        validated = []
        for item in info_list:
            # 简单的关键词匹配
            topic_lower = topic.lower()
            title_lower = item["title"].lower()
            summary_lower = item["summary"].lower()
            
            # 如果标题或摘要包含话题关键词，认为相关
            if topic_lower in title_lower or topic_lower in summary_lower:
                validated.append(item)
            else:
                # 降低相关度
                item["relevance"] = max(30, item["relevance"] - 30)
                validated.append(item)
        
        return validated
    
    def collect_info(self, request: CollectInfoRequest) -> CollectInfoResponse:
        """
        搜集话题相关信息（主入口方法）
        
        参数：
        - request: CollectInfoRequest - 搜集请求
        
        返回：
        - CollectInfoResponse: 搜集响应
        
        实现说明：
        1. 搜索相关信息
        2. 验证相关性
        3. 过滤和排序
        4. 返回结构化结果
        """
        # 搜索信息
        raw_info = self._search_info(request.topic, request.count, request.info_type)
        
        # 验证相关性
        validated_info = self._validate_relevance(raw_info, request.topic)
        
        # 过滤和排序
        filtered_info = self._filter_and_rank(validated_info)
        
        # 转换为响应模型
        info_items = []
        for info in filtered_info:
            info_items.append(InfoItem(
                info_id=info["unique_id"],
                title=info["title"],
                source=info["source"],
                publish_time=info["publish_time"],
                summary=info["summary"],
                relevance_score=info["relevance"],
                url=info.get("url")
            ))
        
        return CollectInfoResponse(
            topic=request.topic,
            collected_at=datetime.now(),
            info_list=info_items,
            total_count=len(info_items)
        )
    
    def batch_collect(self, topics: List[str], count_per_topic: int = 5) -> List[CollectInfoResponse]:
        """
        批量搜集多个话题的信息
        
        参数：
        - topics: List[str] - 话题列表
        - count_per_topic: int - 每个话题搜集数量
        
        返回：
        - List[CollectInfoResponse]: 多个话题的搜集结果
        
        实现说明：
        批量处理多个话题的信息搜集请求
        """
        results = []
        for topic in topics:
            request = CollectInfoRequest(topic=topic, count=count_per_topic)
            result = self.collect_info(request)
            results.append(result)
        return results
    
    def clean_cache(self):
        """
        清理缓存
        
        实现说明：
        清理过期的搜索缓存，释放资源
        """
        # TODO: 实现缓存清理逻辑
        print("缓存清理功能待实现")

# 创建全局服务实例
info_collect_service = InfoCollectService()