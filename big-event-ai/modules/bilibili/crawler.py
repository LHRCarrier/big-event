"""
B站热点爬虫 —— 通过 UApiPro SDK 获取B站热榜数据

从原 hot_topic_service.py 中提取，作为模块化架构的采集层。
"""
import random
from datetime import datetime
from typing import List, Optional

from config import config
from schemas.response import BiliHotResponse, BiliHotItem

# 尝试导入 UApiPro 官方 SDK
try:
    from uapi import UapiClient
    from uapi.errors import UapiError
    HAS_UAPIPRO_SDK = True
except ImportError:
    HAS_UAPIPRO_SDK = False


class BilibiliCrawler:
    """B站热点采集器 —— 负责原始数据获取与格式转换"""

    def __init__(self):
        self.uapipro_client: Optional[UapiClient] = None
        if HAS_UAPIPRO_SDK and config.UAPIPRO_API_KEY:
            try:
                self.uapipro_client = UapiClient(
                    "https://uapis.cn", token=config.UAPIPRO_API_KEY
                )
                print("[B站爬虫] UApiPro 客户端初始化成功")
            except Exception as e:
                print(f"[B站爬虫] UApiPro 客户端初始化失败: {e}")

    async def fetch_hot(self, hot_type: str = "hot", limit: int = 20) -> BiliHotResponse:
        """获取B站热榜（主入口）"""
        if not self.uapipro_client:
            print(f"[B站爬虫] UApiPro 不可用，使用 Mock 数据")
            return self._mock(hot_type, limit)

        try:
            result = self.uapipro_client.misc.get_misc_hotboard(
                type="bilibili", limit=limit
            )
            return self._parse_response(result, hot_type)
        except (UapiError, Exception) as e:
            print(f"[B站爬虫] 获取失败: {e}，降级 Mock")
            return self._mock(hot_type, limit)

    def _parse_response(self, api_data: dict, hot_type: str) -> BiliHotResponse:
        """解析 UApiPro 返回的 B站热榜数据"""
        items = []

        data_list = []
        if isinstance(api_data, list):
            data_list = api_data
        elif "list" in api_data:
            data_list = api_data["list"]
        elif "data" in api_data:
            data_list = api_data["data"]

        for idx, item in enumerate(data_list[:20], 1):
            extra = item.get("extra", {})

            hot_value_str = item.get("hot_value", "0播放")
            hot_value = 0
            try:
                hot_value_str = hot_value_str.split("播放")[0]
                hot_value = int(hot_value_str)
            except (ValueError, IndexError):
                pass

            cover_url = (extra.get("pic", "") or item.get("pic", "")).strip("`")
            if cover_url.startswith("http://"):
                cover_url = "https://" + cover_url[7:]

            bvid = extra.get("bvid", "")
            author = extra.get("owner", {}).get("name", "")
            category = extra.get("tname", "")
            url = item.get("url", "").strip("`")

            stat = extra.get("stat", {})

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

            items.append(BiliHotItem(
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
                like_count=stat.get("like", 0),
                coin_count=stat.get("coin", 0),
                favorite_count=stat.get("favorite", 0),
                share_count=stat.get("share", 0),
                danmaku_count=stat.get("danmaku", 0),
                reply_count=stat.get("reply", 0),
            ))

        return BiliHotResponse(
            fetched_at=datetime.now(),
            hot_type=hot_type,
            items=items,
            source="uapipro",
        )

    def _mock(self, hot_type: str, limit: int) -> BiliHotResponse:
        """Mock 数据（开发测试用）"""
        mock_data = [
            ("2025年最火编程语言排名出炉！Python竟然不是第一？", 2589630, "科技"),
            ("【4K超清】AI绘画新突破！Stable Diffusion 4.0震撼发布", 1895240, "科技"),
            ("程序员的一天：从月薪5k到5w的5年成长历程", 1652380, "生活"),
            ("深度解析：为什么Rust这么火却难学？", 1423690, "科技"),
            ("【教程】30分钟带你入门Web3.0开发", 1258960, "科技"),
            ("【VLOG】UP主用AI做视频年入百万的秘密", 1098560, "生活"),
            ("ChatGPT-5传闻曝光！GPT-4o将成为绝唱？", 985630, "科技"),
            ("【科普】量子计算到底是什么？看完你就懂了", 875420, "科普"),
            ("2025年计算机专业就业现状分析", 765890, "科技"),
            ("【开源推荐】这10个GitHub项目让你事半功倍", 652340, "科技"),
            ("如何用AI做PPT？效率提升10倍！", 598630, "教程"),
            ("【游戏】2025年最值得期待的10款大作", 545280, "游戏"),
            ("程序员必看！2025年技术趋势预测", 489650, "科技"),
            ("【记录】我在大厂工作的第1000天", 432560, "生活"),
            ("Vue3 vs React 2025，谁才是前端王者？", 398560, "科技"),
        ]

        items = []
        for idx, (title, view_count, cat) in enumerate(mock_data[:limit], 1):
            items.append(BiliHotItem(
                rank=idx,
                title=title,
                view_count=view_count,
                cover_url=f"https://picsum.photos/seed/bili{idx}/320/180",
                bvid=f"BV{idx:010d}",
                author=f"UP主{idx}号",
                url=f"https://www.bilibili.com/video/BV{idx:010d}",
                category=cat,
                pubdate=datetime.now(),
                description=f"{title}的Mock简介",
                like_count=view_count // 20 + random.randint(0, 1000),
                coin_count=view_count // 50 + random.randint(0, 500),
                favorite_count=view_count // 30 + random.randint(0, 800),
                share_count=view_count // 200 + random.randint(0, 300),
                danmaku_count=view_count // 500 + random.randint(0, 200),
                reply_count=view_count // 300 + random.randint(0, 400),
            ))

        return BiliHotResponse(
            fetched_at=datetime.now(),
            hot_type=hot_type,
            items=items,
            source="mock",
        )
