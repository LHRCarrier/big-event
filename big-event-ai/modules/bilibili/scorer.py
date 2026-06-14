"""
B站评分引擎 —— B站专属的 5 维评分策略

维度:
- 热度 (0.30): 播放/点赞/投币/收藏/分享 加权
- 持续性 (0.20): 多批次趋势分析
- 深度潜力 (0.25): 基于话题分类的规则评估
- 差异性 (0.15): 去重检查
- 受众匹配 (0.10): 分区与目标受众契合度
"""
import math
from typing import List, Dict, Optional
from schemas.response import BiliHotItem

# 5维权重
W_HOT, W_SUSTAIN, W_DEPTH, W_DIVERSITY, W_AUDIENCE = 0.30, 0.20, 0.25, 0.15, 0.10
W_VIEW, W_LIKE, W_COIN, W_FAVORITE, W_SHARE = 0.30, 0.20, 0.20, 0.15, 0.15

# 分区受众匹配度 (B站视角, 泛科技内容方向)
PARTITION_AUDIENCE = {
    "科技": 0.95, "游戏": 0.80, "知识": 0.85, "数码": 0.90,
    "商业": 0.75, "娱乐": 0.55, "影视": 0.50, "生活": 0.60,
    "体育": 0.30, "音乐": 0.40, "动画": 0.50, "舞蹈": 0.25,
    "美食": 0.35, "时尚": 0.30,
}


class BilibiliScorer:
    """B站评分器 —— 将热榜条目转为带得分的撰稿推荐列表"""

    def score(self, items: List[BiliHotItem],
              history: Optional[Dict[str, List[float]]] = None) -> List[dict]:
        """
        对 B站热榜条目进行 5 维评分

        Args:
            items: B站热榜条目列表
            history: bvid → 历史热度分列表 (用于趋势计算), 可选

        Returns:
            评分结果列表，每项包含: item, total, hot, sustain, depth, diversity, audience
        """
        results = []
        for item in items:
            # 维度1: 热度 (对数归一化)
            hot = self._hot_score(item)

            # 维度2: 持续性 (需要历史数据)
            if history and item.bvid and item.bvid in history:
                sustain = self._sustain_score(history[item.bvid])
            else:
                sustain = 50.0  # 新条目默认中等

            # 维度3: 深度潜力
            depth = self._depth_score(item)

            # 维度4: 差异性 (当前简化: 默认全新)
            diversity = 100.0

            # 维度5: 受众匹配
            audience = self._audience_score(item)

            total = (hot * W_HOT + sustain * W_SUSTAIN + depth * W_DEPTH
                     + diversity * W_DIVERSITY + audience * W_AUDIENCE)

            results.append({
                "item": item,
                "score": round(total, 2),
                "hot_score": round(hot, 2),
                "sustain_score": round(sustain, 2),
                "depth_score": round(depth, 2),
                "diversity_score": round(diversity, 2),
                "audience_score": round(audience, 2),
            })

        results.sort(key=lambda x: x["score"], reverse=True)
        return results

    def _hot_score(self, item: BiliHotItem) -> float:
        """热度评分: 5项互动指标的对数归一化加权"""
        return (
            W_VIEW * _log_norm(item.view_count, 1e3, 1e7)
            + W_LIKE * _log_norm(item.like_count, 10, 1e6)
            + W_COIN * _log_norm(item.coin_count, 1, 1e5)
            + W_FAVORITE * _log_norm(item.favorite_count, 10, 1e5)
            + W_SHARE * _log_norm(item.share_count, 1, 1e4)
        ) * 100.0

    def _sustain_score(self, history: List[float]) -> float:
        """持续性评分: 基于历史热度趋势"""
        if len(history) < 2:
            return 50.0
        mid = len(history) // 2
        recent = sum(history[mid:]) / len(history[mid:])
        earlier = sum(history[:mid]) / len(history[:mid])
        if earlier == 0:
            return 50.0
        ratio = recent / earlier
        if ratio > 1.5:
            return 85.0
        if ratio > 1.2:
            return 70.0
        if ratio > 0.8:
            return 50.0
        if ratio > 0.5:
            return 30.0
        return 15.0

    def _depth_score(self, item: BiliHotItem) -> float:
        """深度潜力评分: 基于标题信息量 + 分区特征"""
        score = 50.0
        title = item.title or ""
        cat = (item.category or "").lower()

        # 标题长度 (20-80字较佳)
        length = len(title)
        if 30 <= length <= 80:
            score += 25
        elif 20 <= length <= 100:
            score += 15
        else:
            score += 5

        # 分区特征
        deep_cats = {"知识", "科技", "数码", "商业"}
        shallow_cats = {"娱乐", "舞蹈"}
        if any(c in cat for c in deep_cats):
            score += 20
        elif "游戏" in cat or "生活" in cat:
            score += 10
        elif any(c in cat for c in shallow_cats):
            score -= 10

        return max(0, min(100, score))

    def _audience_score(self, item: BiliHotItem) -> float:
        """受众匹配评分: 分区与目标受众匹配度"""
        cat = item.category or ""
        for keyword, match in PARTITION_AUDIENCE.items():
            if keyword in cat:
                return match * 100.0
        return 50.0


def _log_norm(value: int, min_val: float, max_val: float) -> float:
    """对数归一化到 [0, 1]"""
    if value <= 0:
        return 0.0
    log_v = math.log10(value)
    log_min = math.log10(min_val)
    log_max = math.log10(max_val)
    return max(0.0, min(1.0, (log_v - log_min) / (log_max - log_min)))
