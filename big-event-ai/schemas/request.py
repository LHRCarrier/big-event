"""
请求数据模型
定义API请求的结构化数据
"""
from pydantic import BaseModel, Field
from typing import List, Optional

class WriteArticleRequest(BaseModel):
    """
    AI撰稿请求模型
    """
    # 话题/关键词
    topic: str = Field(..., description="要撰写的话题或关键词")
    
    # 文章长度（字数）
    length: Optional[int] = Field(500, description="文章预期长度（字数）", ge=100, le=3000)
    
    # 文章风格
    style: Optional[str] = Field("neutral", description="文章风格：neutral(中性)/formal(正式)/casual(轻松)/technical(技术)")
    
    # 目标受众
    audience: Optional[str] = Field("general", description="目标受众：general(大众)/professional(专业人士)/student(学生)")
    
    # 参考信息（可选，用于辅助撰稿）
    references: Optional[List[str]] = Field([], description="参考信息列表，帮助AI了解更多背景")
    
    # 是否生成摘要
    generate_summary: Optional[bool] = Field(True, description="是否生成文章摘要")

class AnalyzeHotTopicRequest(BaseModel):
    """
    热点分析请求模型
    """
    # 平台分区
    platform: Optional[str] = Field("all", description="平台分区：all(全部)/tech(科技)/entertainment(娱乐)/sports(体育)/politics(政治)")
    
    # 时间范围（天数）
    days: Optional[int] = Field(60, description="分析的时间范围（天数）", ge=7, le=180)
    
    # 返回数量
    limit: Optional[int] = Field(10, description="返回热点话题数量", ge=1, le=50)

class CollectInfoRequest(BaseModel):
    """
    信息搜集请求模型
    """
    # 话题
    topic: str = Field(..., description="要搜集信息的话题")

    # 搜索数量
    count: Optional[int] = Field(10, description="搜集信息数量", ge=1, le=50)

    # 信息类型
    info_type: Optional[str] = Field("all", description="信息类型：all(全部)/news(新闻)/article(文章)/social(社交媒体)")


class WriteFromHotRequest(BaseModel):
    """
    基于热点数据撰稿请求模型
    """
    # 热点标题
    title: str = Field(..., description="热点视频标题")

    # 热点分区
    partition: str = Field("其他", description="映射后的一级分区")

    # B站原始分区
    category: str = Field("", description="B站原始分区名")

    # UP主
    author: str = Field("", description="UP主名称")

    # 播放量
    view_count: int = Field(0, description="播放量")

    # 点赞数
    like_count: int = Field(0, description="点赞数")

    # 投币数
    coin_count: int = Field(0, description="投币数")

    # 收藏数
    favorite_count: int = Field(0, description="收藏数")

    # 分享数
    share_count: int = Field(0, description="分享数")

    # 弹幕数
    danmaku_count: int = Field(0, description="弹幕数")

    # 评论数
    reply_count: int = Field(0, description="评论数")

    # 综合热度评分
    hot_score: float = Field(0, description="综合热度评分(0-100)")

    # 排名
    rank: int = Field(0, description="热榜排名")

    # 文章长度
    length: Optional[int] = Field(800, description="文章预期长度（字数）", ge=100, le=3000)

    # 文章风格
    style: Optional[str] = Field("neutral", description="文章风格")

    # 目标受众
    audience: Optional[str] = Field("general", description="目标受众")

    # 是否生成摘要
    generate_summary: Optional[bool] = Field(True, description="是否生成文章摘要")

    # BV号
    bvid: Optional[str] = Field(None, description="B站视频BV号")

    # 视频简介（核心素材）
    description: Optional[str] = Field(None, description="视频简介，AI撰稿的核心素材来源")