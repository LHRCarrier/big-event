"""
响应数据模型
定义API响应的结构化数据
"""
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime

class ArticleSection(BaseModel):
    """
    文章段落模型
    """
    # 段落标题
    title: str = Field(..., description="段落标题")
    
    # 段落内容
    content: str = Field(..., description="段落内容")

class WriteArticleResponse(BaseModel):
    """
    AI撰稿响应模型
    """
    # 文章ID
    article_id: str = Field(..., description="生成的文章唯一标识")
    
    # 文章标题
    title: str = Field(..., description="文章标题")
    
    # 文章内容
    content: str = Field(..., description="完整文章内容")
    
    # 文章摘要
    summary: Optional[str] = Field(None, description="文章摘要")
    
    # 文章段落列表（结构化）
    sections: Optional[List[ArticleSection]] = Field([], description="文章段落结构")
    
    # 生成时间
    generated_at: datetime = Field(..., description="生成时间")
    
    # 使用的模型
    model_used: str = Field(..., description="使用的AI模型")

class HotTopic(BaseModel):
    """
    热点话题模型
    """
    # 话题ID
    topic_id: str = Field(..., description="话题唯一标识")
    
    # 话题名称
    name: str = Field(..., description="话题名称")
    
    # 热度评分（0-100）
    score: float = Field(..., description="热度评分")
    
    # 热度趋势（上升/下降/稳定）
    trend: str = Field(..., description="热度趋势：up(上升)/down(下降)/stable(稳定)")
    
    # 相关关键词
    keywords: List[str] = Field([], description="相关关键词")
    
    # 讨论量
    discussion_count: int = Field(..., description="讨论数量")
    
    # 平台来源
    platform: str = Field(..., description="平台来源")

class AnalyzeHotTopicResponse(BaseModel):
    """
    热点分析响应模型
    """
    # 分析时间
    analyzed_at: datetime = Field(..., description="分析时间")
    
    # 时间范围（天数）
    time_range_days: int = Field(..., description="分析的时间范围")
    
    # 热点话题列表
    hot_topics: List[HotTopic] = Field([], description="热点话题列表")

class InfoItem(BaseModel):
    """
    搜集到的信息项模型
    """
    # 信息ID
    info_id: str = Field(..., description="信息唯一标识")
    
    # 标题
    title: str = Field(..., description="信息标题")
    
    # 来源
    source: str = Field(..., description="信息来源")
    
    # 发布时间
    publish_time: Optional[datetime] = Field(None, description="发布时间")
    
    # 内容摘要
    summary: str = Field(..., description="内容摘要")
    
    # 相关度评分（0-100）
    relevance_score: float = Field(..., description="与话题的相关度评分")
    
    # 原文链接
    url: Optional[str] = Field(None, description="原文链接")

class CollectInfoResponse(BaseModel):
    """
    信息搜集响应模型
    """
    # 话题
    topic: str = Field(..., description="搜集信息的话题")
    
    # 搜集时间
    collected_at: datetime = Field(..., description="搜集时间")
    
    # 信息列表
    info_list: List[InfoItem] = Field([], description="搜集到的信息列表")
    
    # 总数量
    total_count: int = Field(..., description="信息总数量")

class BiliHotItem(BaseModel):
    """
    B站单个热榜条目模型
    """
    # 排名
    rank: int = Field(..., description="排名")

    # 标题
    title: str = Field(..., description="标题")

    # 播放量
    view_count: int = Field(0, description="播放量")

    # 封面图
    cover_url: Optional[str] = Field(None, description="封面图URL")

    # BV号
    bvid: Optional[str] = Field(None, description="视频BV号")

    # UP主
    author: Optional[str] = Field(None, description="UP主名称")

    # 链接
    url: Optional[str] = Field(None, description="链接地址")

    # 分区
    category: Optional[str] = Field(None, description="分区名称")

    # 发布时间（Unix 时间戳转换）
    pubdate: Optional[datetime] = Field(None, description="视频发布时间")

    # 视频简介
    description: Optional[str] = Field(None, description="视频简介")

    # 互动数据（来自B站stat字段）
    like_count: int = Field(0, description="点赞数")
    coin_count: int = Field(0, description="投币数")
    favorite_count: int = Field(0, description="收藏数")
    share_count: int = Field(0, description="分享数")
    danmaku_count: int = Field(0, description="弹幕数")
    reply_count: int = Field(0, description="评论数")

class BiliHotResponse(BaseModel):
    """
    B站热榜响应模型
    """
    # 获取时间
    fetched_at: datetime = Field(..., description="数据获取时间")
    
    # 热榜类型
    hot_type: str = Field(..., description="热榜类型：hot(热门榜)/week(周榜)/real(实时榜)")
    
    # 热榜数据
    items: List[BiliHotItem] = Field([], description="热榜条目列表")
    
    # 数据来源
    source: str = Field("uapipro", description="数据来源")

class ServiceStatus(BaseModel):
    """
    服务状态响应模型
    """
    # 服务名称
    service_name: str = Field(..., description="服务名称")
    
    # 服务状态
    status: str = Field(..., description="服务状态：running(运行中)/error(错误)")
    
    # AI模型连接状态
    ai_model_status: str = Field(..., description="AI模型连接状态")
    
    # 启动时间
    started_at: datetime = Field(..., description="服务启动时间")
    
    # 当前时间
    current_time: datetime = Field(..., description="当前时间")