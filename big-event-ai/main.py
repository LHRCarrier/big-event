"""
AI服务主入口
基于FastAPI实现REST API接口
"""
from datetime import datetime
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from config import config
from schemas.request import WriteArticleRequest, WriteFromHotRequest, AnalyzeHotTopicRequest, CollectInfoRequest
from schemas.response import WriteArticleResponse, AnalyzeHotTopicResponse, CollectInfoResponse, ServiceStatus, BiliHotResponse
from services.writer_service import writer_service
from services.hot_topic_service import hot_topic_service
from services.info_collect_service import info_collect_service
from fastapi.responses import StreamingResponse
import json

# 记录服务启动时间
START_TIME = datetime.now()

# 创建FastAPI应用
app = FastAPI(
    title="撰稿人AI服务",
    description="提供AI撰稿、热点分析、信息搜集等功能的REST API服务",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# 配置CORS中间件
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ==================== 健康检查接口 ====================

@app.get("/health", response_model=ServiceStatus, tags=["健康检查"])
async def health_check():
    """
    健康检查接口
    
    返回服务状态信息，用于监控和诊断
    """
    return ServiceStatus(
        service_name="撰稿人AI服务",
        status="running",
        ai_model_status="available" if writer_service.is_available() else "unavailable (using mock)",
        started_at=START_TIME,
        current_time=datetime.now()
    )

# ==================== 知识库匹配预览接口 ====================

@app.get("/api/knowledge/match", tags=["知识库"])
async def match_knowledge(topic: str, limit: int = 5):
    """知识库匹配预览，返回与话题匹配的参考文章"""
    try:
        from shared.knowledge import get_retriever
        retriever = get_retriever()
        articles = retriever.retrieve(topic=topic, top_k=limit)
        return {
            "matched": len(articles),
            "articles": [
                {
                    "title": a.get("title", ""),
                    "author": a.get("author", ""),
                    "category": a.get("category", ""),
                    "similarity": round(a.get("similarity", 0), 4),
                    "excerpt": (a.get("excerpt", "") or "")[:200]
                }
                for a in articles
            ]
        }
    except Exception as e:
        return {"matched": 0, "articles": [], "error": str(e)}

# ==================== AI撰稿接口 ====================

@app.post("/api/writer/write", response_model=WriteArticleResponse, tags=["AI撰稿"])
async def write_article(request: WriteArticleRequest):
    try:
        print(f"[调用链-2/4] FastAPI收到撰稿: topic={request.topic}, length={request.length}, style={request.style}")
        result = writer_service.write_article(request)
        print(f"[调用链-4/4] FastAPI撰稿完成: articleId={result.article_id}, title={result.title}, contentLen={len(result.content)}")
        return result
    except Exception as e:
        print(f"[调用链-ERROR] 撰稿失败: {e}")
        raise HTTPException(status_code=500, detail=f"文章生成失败: {str(e)}")

@app.post("/api/writer/write/stream", tags=["AI撰稿"])
async def write_article_stream(request: WriteArticleRequest):
    """
    AI撰稿流式接口（实时显示）

    根据话题生成文章内容，支持流式返回，可实时显示在前端

    参数：
    - topic: 要撰写的话题或关键词
    - length: 文章预期长度（默认500字）
    - style: 文章风格（neutral/formal/casual/technical）
    - audience: 目标受众（general/professional/student）
    - references: 参考信息列表（可选）
    - generate_summary: 是否生成摘要（默认true）

    返回：
    - 流式返回文章内容片段
    """
    def generate():
        for chunk in writer_service.stream_article_content(request):
            yield f"data: {json.dumps({'content': chunk})}\n\n"

    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "Access-Control-Allow-Origin": "*"
        }
    )

@app.post("/api/writer/write-from-hot", response_model=WriteArticleResponse, tags=["AI撰稿"])
async def write_article_from_hot(request: WriteFromHotRequest):
    try:
        desc_len = len(request.description) if request.description else 0
        print(f"[调用链-2/4] FastAPI收到热点撰稿: title={request.title}, score={request.hot_score}, partition={request.partition}, descLen={desc_len}")
        result = writer_service.write_article_from_hot(request)
        print(f"[调用链-4/4] FastAPI热点撰稿完成: articleId={result.article_id}, title={result.title}, contentLen={len(result.content)}")
        return result
    except Exception as e:
        print(f"[调用链-ERROR] 热点撰稿失败: {e}")
        raise HTTPException(status_code=500, detail=f"热点文章生成失败: {str(e)}")


# ==================== 热点分析接口 ====================

@app.post("/api/hot-topic/analyze", response_model=AnalyzeHotTopicResponse, tags=["热点分析"])
async def analyze_hot_topics(request: AnalyzeHotTopicRequest):
    """
    热点分析接口
    
    分析平台热点话题，返回TopN热点列表
    
    参数：
    - platform: 平台分区（all/tech/entertainment/sports/politics）
    - days: 分析时间范围（默认60天）
    - limit: 返回数量（默认10）
    
    返回：
    - analyzed_at: 分析时间
    - time_range_days: 时间范围
    - hot_topics: 热点话题列表
    """
    try:
        result = hot_topic_service.analyze_hot_topics(request)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"热点分析失败: {str(e)}")

@app.get("/api/hot-topic/{topic_id}", tags=["热点分析"])
async def get_topic_details(topic_id: str):
    """
    获取话题详细信息
    
    参数：
    - topic_id: 话题ID
    
    返回：
    - 话题详情信息
    """
    try:
        result = hot_topic_service.get_topic_details(topic_id)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取话题详情失败: {str(e)}")

@app.post("/api/hot-topic/refresh", tags=["热点分析"])
async def refresh_topics():
    """
    刷新热点话题数据
    
    从外部数据源获取最新热点数据
    """
    try:
        hot_topic_service.refresh_topics()
        return {"message": "热点数据刷新任务已触发"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"刷新热点数据失败: {str(e)}")

# ==================== B站热点分析接口 ====================

@app.get("/api/bilibili/hot", response_model=BiliHotResponse, tags=["B站热点分析"])
async def get_bilibili_hot(hot_type: str = "hot", limit: int = 20):
    """
    获取B站热榜数据
    
    参数：
    - hot_type: 热榜类型：hot(热门榜)/week(周榜)/real(实时榜)
    - limit: 返回数量限制，最大50
    
    返回：
    - B站热榜数据
    """
    try:
        # 限制最大返回数量
        limit = min(limit, 50)
        result = await hot_topic_service.get_bilibili_hot(hot_type, limit)
        return result
    except Exception as e:
        print(f"[B站热榜] 获取热榜失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"获取B站热榜失败: {str(e)}")

# ==================== 信息搜集接口 ====================

@app.post("/api/info/collect", response_model=CollectInfoResponse, tags=["信息搜集"])
async def collect_info(request: CollectInfoRequest):
    """
    信息搜集接口
    
    搜集话题相关的信息并进行处理过滤
    
    参数：
    - topic: 要搜集信息的话题
    - count: 搜集数量（默认10）
    - info_type: 信息类型（all/news/article/social）
    
    返回：
    - topic: 搜集信息的话题
    - collected_at: 搜集时间
    - info_list: 搜集到的信息列表
    - total_count: 信息总数量
    """
    try:
        result = info_collect_service.collect_info(request)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"信息搜集失败: {str(e)}")

@app.post("/api/info/clean-cache", tags=["信息搜集"])
async def clean_cache():
    """
    清理缓存
    
    清理过期的搜索缓存，释放资源
    """
    try:
        info_collect_service.clean_cache()
        return {"message": "缓存清理任务已触发"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"清理缓存失败: {str(e)}")

# ==================== 服务启动 ====================

if __name__ == "__main__":
    import uvicorn
    print(f"启动撰稿人AI服务...")
    print(f"服务地址: http://{config.HOST}:{config.PORT}")
    print(f"API文档: http://{config.HOST}:{config.PORT}/docs")
    
    uvicorn.run(
        app,
        host=config.HOST,
        port=config.PORT,
        timeout_keep_alive=60
    )