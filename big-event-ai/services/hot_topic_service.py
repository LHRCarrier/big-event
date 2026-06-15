"""
热点分析服务
负责获取平台热点话题并进行分析
"""
import uuid
import requests
from datetime import datetime
from typing import List
from concurrent.futures import ThreadPoolExecutor, as_completed
from config import config
from schemas.request import AnalyzeHotTopicRequest
from schemas.response import AnalyzeHotTopicResponse, HotTopic, BiliHotResponse, BiliHotItem

# 尝试导入 UApiPro 官方 SDK
try:
    from uapi import UapiClient
    from uapi.errors import UapiError
    HAS_UAPIPRO_SDK = True
    print("[配置] UApiPro SDK 导入成功")
except ImportError:
    HAS_UAPIPRO_SDK = False
    print("[配置] UApiPro SDK 未安装，将使用Mock数据")

class HotTopicService:
    """
    热点话题服务类
    提供热点分析、趋势预测等功能
    """
    
    def __init__(self):
        """初始化服务"""
        # Mock热点数据（实际应用中从数据库或外部API获取）
        self.mock_topics = [
            {"name": "人工智能发展趋势", "platform": "tech", "score": 95.5, "trend": "up", "keywords": ["AI", "机器学习", "大模型"], "discussion_count": 125800},
            {"name": "新能源汽车市场分析", "platform": "tech", "score": 88.3, "trend": "up", "keywords": ["新能源", "电动汽车", "特斯拉"], "discussion_count": 89200},
            {"name": "互联网行业裁员潮", "platform": "tech", "score": 82.1, "trend": "stable", "keywords": ["裁员", "互联网", "大厂"], "discussion_count": 67500},
            {"name": "元宇宙概念降温", "platform": "tech", "score": 72.4, "trend": "down", "keywords": ["元宇宙", "VR", "虚拟世界"], "discussion_count": 45300},
            {"name": "ChatGPT应用场景", "platform": "tech", "score": 91.2, "trend": "up", "keywords": ["ChatGPT", "AI助手", "对话AI"], "discussion_count": 156700},
            {"name": "影视行业复苏", "platform": "entertainment", "score": 78.6, "trend": "up", "keywords": ["电影", "票房", "影视"], "discussion_count": 54200},
            {"name": "明星八卦新闻", "platform": "entertainment", "score": 85.8, "trend": "stable", "keywords": ["明星", "八卦", "娱乐圈"], "discussion_count": 98600},
            {"name": "体育赛事热点", "platform": "sports", "score": 89.4, "trend": "up", "keywords": ["体育", "赛事", "奥运会"], "discussion_count": 76800},
            {"name": "国际政治动态", "platform": "politics", "score": 65.2, "trend": "stable", "keywords": ["政治", "国际", "政策"], "discussion_count": 32400},
            {"name": "气候变化议题", "platform": "politics", "score": 71.8, "trend": "up", "keywords": ["气候", "环保", "碳中和"], "discussion_count": 48900},
        ]
        
        # 初始化 UApiPro 客户端
        self.uapipro_client = None
        if HAS_UAPIPRO_SDK and config.UAPIPRO_API_KEY:
            try:
                self.uapipro_client = UapiClient("https://uapis.cn", token=config.UAPIPRO_API_KEY)
                print("[配置] UApiPro 客户端初始化成功")
            except Exception as e:
                print(f"[配置] UApiPro 客户端初始化失败: {str(e)}")
    
    def _get_platform_topics(self, platform: str) -> List[dict]:
        """
        获取指定平台的热点话题
        
        参数：
        - platform: str - 平台名称
        
        返回：
        - List[dict]: 热点话题列表
        
        实现说明：
        1. 根据平台筛选话题
        2. 如果platform为"all"，返回所有话题
        """
        if platform == "all":
            return self.mock_topics
        return [topic for topic in self.mock_topics if topic["platform"] == platform]
    
    def _calculate_score(self, topic: dict, days: int) -> float:
        """
        计算热点话题评分
        
        参数：
        - topic: dict - 话题数据
        - days: int - 时间范围
        
        返回：
        - float: 热度评分
        
        实现说明：
        1. 综合考虑讨论量、趋势、时间因素
        2. 返回0-100的评分
        """
        base_score = topic["score"]
        trend_factor = 1.0
        
        # 根据趋势调整评分
        if topic["trend"] == "up":
            trend_factor = 1.1
        elif topic["trend"] == "down":
            trend_factor = 0.9
        
        # 根据时间范围调整（近期数据权重更高）
        time_factor = min(days / 30, 1.0)
        
        return round(base_score * trend_factor * time_factor, 1)
    
    def analyze_hot_topics(self, request: AnalyzeHotTopicRequest) -> AnalyzeHotTopicResponse:
        """
        分析热点话题（主入口方法）
        
        参数：
        - request: AnalyzeHotTopicRequest - 分析请求
        
        返回：
        - AnalyzeHotTopicResponse: 分析响应
        
        实现说明：
        1. 获取指定平台的话题
        2. 计算热度评分
        3. 按评分排序
        4. 返回topN话题
        """
        # 获取平台话题
        topics = self._get_platform_topics(request.platform)
        
        # 计算评分并排序
        scored_topics = []
        for topic in topics:
            score = self._calculate_score(topic, request.days)
            scored_topics.append({
                **topic,
                "calculated_score": score
            })
        
        # 按评分降序排序
        scored_topics.sort(key=lambda x: x["calculated_score"], reverse=True)
        
        # 取前limit个
        top_topics = scored_topics[:request.limit]
        
        # 转换为响应模型
        hot_topics = []
        for topic in top_topics:
            hot_topics.append(HotTopic(
                topic_id=str(uuid.uuid4()),
                name=topic["name"],
                score=topic["calculated_score"],
                trend=topic["trend"],
                keywords=topic["keywords"],
                discussion_count=topic["discussion_count"],
                platform=topic["platform"]
            ))
        
        return AnalyzeHotTopicResponse(
            analyzed_at=datetime.now(),
            time_range_days=request.days,
            hot_topics=hot_topics
        )
    
    def refresh_topics(self):
        """
        刷新热点话题数据
        
        实现说明：
        1. 从外部数据源获取最新热点
        2. 更新本地缓存
        3. 可以设置定时任务定期调用
        """
        # TODO: 实现从外部API获取热点数据
        # 例如：调用微博热搜、百度热搜等API
        print("热点数据刷新功能待实现")
    
    def get_topic_details(self, topic_id: str) -> HotTopic:
        """
        获取话题详细信息
        
        参数：
        - topic_id: str - 话题ID
        
        返回：
        - HotTopic: 话题详情
        
        实现说明：
        根据话题ID查找详细信息
        """
        # TODO: 实现根据ID获取话题详情
        # 可以从数据库查询完整的话题信息
        return HotTopic(
            topic_id=topic_id,
            name="话题详情",
            score=0.0,
            trend="stable",
            keywords=[],
            discussion_count=0,
            platform="all"
        )
    
    async def get_bilibili_hot(self, hot_type: str = "hot", limit: int = 20) -> BiliHotResponse:
        """
        获取B站热榜数据（通过UApiPro SDK）
        
        参数：
        - hot_type: 热榜类型 (hot/week/real)
        - limit: 返回数量限制
        
        返回：
        - BiliHotResponse: B站热榜响应
        
        实现说明：
        1. 使用UApiPro SDK获取B站热榜
        2. 解析返回数据并转换为标准格式
        """
        print(f"[B站热榜] 开始获取热榜数据, 类型: {hot_type}, 限制: {limit}")
        
        # 检查API Key是否配置
        if not config.UAPIPRO_API_KEY:
            print(f"[B站热榜] UAPIPRO_API_KEY 未配置，使用Mock数据")
            return self._get_mock_bilibili_hot(hot_type)
        
        # 检查SDK是否可用
        if not HAS_UAPIPRO_SDK or not self.uapipro_client:
            print(f"[B站热榜] UApiPro SDK 不可用，使用Mock数据")
            return self._get_mock_bilibili_hot(hot_type)
        
        try:
            print(f"[B站热榜] 使用UApiPro SDK获取数据")
            
            # 使用UApiPro SDK调用热榜接口
            result = self.uapipro_client.misc.get_misc_hotboard(type="bilibili", limit=limit)
            
            print(f"[B站热榜] SDK调用成功")
            print(f"[B站热榜] 返回数据: {result}")
            response = self._parse_uapipro_response(result, hot_type)
            response = self._enrich_descriptions(response)
            return response
            
        except UapiError as e:
            print(f"[B站热榜] API错误: {e}")
            return self._get_mock_bilibili_hot(hot_type)
        except Exception as e:
            print(f"[B站热榜] 获取热榜数据异常: {str(e)}")
            return self._get_mock_bilibili_hot(hot_type)
    
    def _parse_uapipro_response(self, api_data: dict, hot_type: str) -> BiliHotResponse:
        """
        解析UApiPro返回的B站热榜数据
        
        参数：
        - api_data: UApiPro返回的原始数据
        - hot_type: 热榜类型
        
        返回：
        - BiliHotResponse: 格式化后的热榜数据
        """
        items = []
        
        # 打印完整的返回数据用于调试
        print(f"[B站热榜] 完整返回数据: {api_data}")
        
        # 尝试找到数据列表（根据实际返回格式调整）
        data_list = []
        if isinstance(api_data, list):
            data_list = api_data
            print(f"[B站热榜] 返回的是列表格式，直接使用")
        elif "list" in api_data:
            data_list = api_data["list"]
            print(f"[B站热榜] 找到list字段")
        elif "data" in api_data:
            data_list = api_data["data"]
            print(f"[B站热榜] 找到data字段")
        
        print(f"[B站热榜] 解析到 {len(data_list)} 条数据")
        if data_list and len(data_list) > 0:
            print(f"[B站热榜] 第一条数据: {data_list[0]}")
        
        for idx, item in enumerate(data_list[:20], 1):
            # 打印单个item的所有字段
            if idx == 1:
                print(f"[B站热榜] 第{idx}条完整数据: {item}")
            
            # 获取extra字段（包含详细信息）
            extra = item.get("extra", {})
            
            # 解析热度值（从"2917203播放"中提取数字）
            hot_value_str = item.get("hot_value", "0播放")
            hot_value = 0
            try:
                # 从字符串中提取数字部分
                hot_value_str = hot_value_str.split("播放")[0]
                hot_value = int(hot_value_str)
            except (ValueError, IndexError):
                pass
            
            # 从extra中获取详细信息
            cover_url = extra.get("pic", "")
            bvid = extra.get("bvid", "")
            author = extra.get("owner", {}).get("name", "")
            category = extra.get("tname", "")
            
            # 如果extra中没有图片，尝试从其他位置获取
            if not cover_url:
                cover_url = item.get("pic", "")
            
            # 去除 URL 前后的反引号（UApiPro 返回的 URL 被反引号包裹了）
            if cover_url:
                cover_url = cover_url.strip("`")
            
            # 把 HTTP 协议的 URL 转换成 HTTPS（B站现在只支持 HTTPS）
            if cover_url and cover_url.startswith("http://"):
                cover_url = "https://" + cover_url[7:]
            
            # 调试日志
            if idx == 1:
                print(f"[B站热榜] cover_url: {cover_url}")
                print(f"[B站热榜] extra.pic: {extra.get('pic', '')}")
                print(f"[B站热榜] item.pic: {item.get('pic', '')}")
            
            # 获取并清理其他URL字段
            url = item.get("url", "").strip("`")

            # 提取stat互动数据
            stat = extra.get("stat", {})
            like_count = stat.get("like", 0)
            coin_count = stat.get("coin", 0)
            favorite_count = stat.get("favorite", 0)
            share_count = stat.get("share", 0)
            danmaku_count = stat.get("danmaku", 0)
            reply_count = stat.get("reply", 0)

            # 解析视频发布时间（Unix 时间戳）
            pubdate = None
            pubdate_ts = extra.get("pubdate") or item.get("pubdate")
            if pubdate_ts:
                try:
                    pubdate = datetime.fromtimestamp(int(pubdate_ts))
                except (ValueError, TypeError, OSError):
                    pass

            # 视频简介
            description = extra.get("desc", "") or item.get("desc", "")

            bili_item = BiliHotItem(
                rank=idx,
                title=item.get("title", ""),
                view_count=hot_value,
                cover_url=cover_url,
                bvid=bvid,
                author=author,
                url=url,
                category=category,
                pubdate=pubdate,
                description=description,
                like_count=like_count,
                coin_count=coin_count,
                favorite_count=favorite_count,
                share_count=share_count,
                danmaku_count=danmaku_count,
                reply_count=reply_count
            )
            items.append(bili_item)
        
        return BiliHotResponse(
            fetched_at=datetime.now(),
            hot_type=hot_type,
            items=items,
            source="uapipro"
        )
    
    # ── 视频简介补充 ──

    MAX_DESC_FETCH = 10

    def _enrich_descriptions(self, response: BiliHotResponse) -> BiliHotResponse:
        """
        通过 UApiPro videoinfo API 为缺少简介的热榜条目补充 desc。

        API: GET /api/v1/social/bilibili/videoinfo?bvid=xxx
        SDK: client.social.get_social_bilibili_videoinfo(bvid=xxx)
        """
        need_desc = [
            item for item in response.items
            if not item.description and item.bvid
        ][:self.MAX_DESC_FETCH]

        if not need_desc:
            return response

        print(f"[B站热榜] 需为 {len(need_desc)} 条热榜补充简介")

        bvid_map = {item.bvid: item for item in need_desc}
        fetched = 0

        with ThreadPoolExecutor(max_workers=3) as executor:
            futures = {
                executor.submit(self._fetch_video_desc, bvid): bvid
                for bvid in bvid_map
            }
            for future in as_completed(futures):
                bvid = futures[future]
                try:
                    desc = future.result()
                    if desc:
                        bvid_map[bvid].description = desc
                        fetched += 1
                except Exception as e:
                    print(f"[B站热榜] 获取 {bvid} 简介失败: {e}")

        print(f"[B站热榜] 简介补充完成: {fetched}/{len(need_desc)}")
        return response

    def _fetch_video_desc(self, bvid: str) -> str:
        """获取单个视频的简介，优先 SDK，回退 HTTP。"""
        if self.uapipro_client:
            try:
                result = self.uapipro_client.social.get_social_bilibili_videoinfo(bvid=bvid)
                desc = result.get("desc", "") if isinstance(result, dict) else ""
                if desc:
                    print(f"[B站热榜] [SDK] {bvid} 简介: {desc[:60]}...")
                    return desc
            except Exception as e:
                print(f"[B站热榜] [SDK] {bvid} videoinfo 失败: {e}")

        try:
            url = "https://uapis.cn/api/v1/social/bilibili/videoinfo"
            params = {"bvid": bvid, "token": config.UAPIPRO_API_KEY}
            resp = requests.get(url, params=params, timeout=15)
            if resp.status_code == 200:
                data = resp.json()
                desc = data.get("desc", "") if isinstance(data, dict) else ""
                if desc:
                    print(f"[B站热榜] [HTTP] {bvid} 简介: {desc[:60]}...")
                    return desc
            else:
                print(f"[B站热榜] [HTTP] {bvid} videoinfo status={resp.status_code}")
        except Exception as e:
            print(f"[B站热榜] [HTTP] {bvid} videoinfo 请求失败: {e}")

        return ""

    def _get_mock_bilibili_hot(self, hot_type: str = "hot") -> BiliHotResponse:
        """
        获取Mock的B站热榜数据（用于演示和开发测试）
        
        参数：
        - hot_type: 热榜类型
        
        返回：
        - BiliHotResponse: Mock热榜数据
        """
        import random
        mock_data = [
            {"title": "2025年最火编程语言排名出炉！Python竟然不是第一？", "view": 2589630, "category": "科技"},
            {"title": "【4K超清】AI绘画新突破！Stable Diffusion 4.0震撼发布", "view": 1895240, "category": "科技"},
            {"title": "程序员的一天：从月薪5k到5w的5年成长历程", "view": 1652380, "category": "生活"},
            {"title": "深度解析：为什么Rust这么火却难学？", "view": 1423690, "category": "科技"},
            {"title": "【教程】30分钟带你入门Web3.0开发", "view": 1258960, "category": "科技"},
            {"title": "【VLOG】UP主用AI做视频年入百万的秘密", "view": 1098560, "category": "生活"},
            {"title": "ChatGPT-5传闻曝光！GPT-4o将成为绝唱？", "view": 985630, "category": "科技"},
            {"title": "【科普】量子计算到底是什么？看完你就懂了", "view": 875420, "category": "科普"},
            {"title": "2025年计算机专业就业现状分析", "view": 765890, "category": "科技"},
            {"title": "【开源推荐】这10个GitHub项目让你事半功倍", "view": 652340, "category": "科技"},
            {"title": "如何用AI做PPT？效率提升10倍！", "view": 598630, "category": "教程"},
            {"title": "【游戏】2025年最值得期待的10款大作", "view": 545280, "category": "游戏"},
            {"title": "程序员必看！2025年技术趋势预测", "view": 489650, "category": "科技"},
            {"title": "【记录】我在大厂工作的第1000天", "view": 432560, "category": "生活"},
            {"title": "Vue3 vs React 2025，谁才是前端王者？", "view": 398560, "category": "科技"},
        ]

        items = []
        for idx, item in enumerate(mock_data, 1):
            view_count = item["view"]
            items.append(BiliHotItem(
                rank=idx,
                title=item["title"],
                view_count=view_count,
                cover_url=f"https://picsum.photos/seed/bili{idx}/320/180",
                bvid=f"BV{idx:010d}",
                author=f"UP主{idx}号",
                url=f"https://www.bilibili.com/video/BV{idx:010d}",
                category=item["category"],
                pubdate=datetime.now(),
                description=f"{item['title']}的Mock简介",
                like_count=view_count // 20 + random.randint(0, 1000),
                coin_count=view_count // 50 + random.randint(0, 500),
                favorite_count=view_count // 30 + random.randint(0, 800),
                share_count=view_count // 200 + random.randint(0, 300),
                danmaku_count=view_count // 500 + random.randint(0, 200),
                reply_count=view_count // 300 + random.randint(0, 400)
            ))
        
        return BiliHotResponse(
            fetched_at=datetime.now(),
            hot_type=hot_type,
            items=items,
            source="mock"
        )

# 创建全局服务实例
hot_topic_service = HotTopicService()
