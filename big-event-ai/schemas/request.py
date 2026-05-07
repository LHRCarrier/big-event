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