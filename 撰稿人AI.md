# 撰稿人AI — 自动化文章发布与热点分析平台

## 整体数据流

```
定时调度 (Spring @Scheduled, 每30分钟)
  │
  ▼
B站热榜抓取 (UApiPro: /api/v1/misc/hotboard?type=bilibili)
  │  写入 hot_snapshot 表（含完整 stat 数据）
  ▼
分区映射 (tname → 一级分区: 科技/游戏/生活/娱乐/知识/...)
  │
  ▼
综合热点评分引擎 ← 读取近 60 天历史快照
  │  (播放量×0.3 + 点赞×0.2 + 投币×0.2 + 收藏×0.15 + 分享×0.15)
  │  × 时效衰减因子 × 趋势因子
  ▼
筛选 Top N 话题 → 去重（bvid 查 processed_record 表）
  │
  ▼
AI 撰稿 (DeepSeek via SiliconFlow, 带完整热点上下文 prompt)
  │
  ▼
Article 入库 (state = "draft", 标记来源为 AI 生成)
  │
  ▼
人工审核 → 发布 / 驳回
```

---

## 一、数据库新建表

### 1.1 热点快照表 `hot_snapshot`

每次定时任务抓取 B站热榜后，整榜存入此表。一条记录 = 一次抓取的一个视频条目。

```sql
CREATE TABLE hot_snapshot (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_time DATETIME     NOT NULL COMMENT '抓取时间',
    bvid          VARCHAR(20)  NOT NULL COMMENT 'B站视频BV号',
    title         VARCHAR(200) NOT NULL COMMENT '视频标题',
    url           VARCHAR(500) COMMENT '视频链接',
    cover_url     VARCHAR(500) COMMENT '封面图URL',
    author        VARCHAR(100) COMMENT 'UP主名称',
    tname         VARCHAR(50)  COMMENT 'B站原始分区名',
    partition_tag VARCHAR(20)  COMMENT '映射后的一级分区',
    rank          INT          COMMENT '当时排名',
    view_count    BIGINT       COMMENT '播放量',
    like_count    BIGINT       COMMENT '点赞数',
    coin_count    BIGINT       COMMENT '投币数',
    favorite_count BIGINT      COMMENT '收藏数',
    share_count   BIGINT       COMMENT '分享数',
    danmaku_count BIGINT       COMMENT '弹幕数',
    reply_count   BIGINT       COMMENT '评论数',
    hot_score     DECIMAL(10,2) COMMENT '综合热度评分',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_snapshot_time (snapshot_time),
    INDEX idx_bvid (bvid),
    INDEX idx_partition (partition_tag),
    UNIQUE KEY uk_snapshot_bvid (snapshot_time, bvid)
) COMMENT '热点快照表-每次抓取的完整热榜数据';
```

### 1.2 分区映射表 `partition_mapping`

将 B站 200+ 个细分 `tname` 映射到 10 个以内的一级分区。

```sql
CREATE TABLE partition_mapping (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tname       VARCHAR(50) NOT NULL UNIQUE COMMENT 'B站原始分区名',
    partition   VARCHAR(20) NOT NULL COMMENT '一级分区: 科技/游戏/生活/娱乐/知识/影视/体育/其他'
) COMMENT 'B站分区→平台分区映射表';
```

初始映射数据（摘录关键项，完整约 200+ 条需要从 B站分区 API 获取后批量导入）：

| tname | partition |
|-------|-----------|
| 极客DIY | 科技 |
| 软件应用 | 科技 |
| 科工机械 | 科技 |
| 数码 | 科技 |
| 单机游戏 | 游戏 |
| 手机游戏 | 游戏 |
| 电子竞技 | 游戏 |
| 日常 | 生活 |
| 美食 | 生活 |
| 搞笑 | 娱乐 |
| 明星 | 娱乐 |
| 科学科普 | 知识 |
| 人文历史 | 知识 |
| ... | ... |

### 1.3 AI 处理记录表 `ai_process_record`

追踪哪些 B站视频已经生成过文章，防止重复。

```sql
CREATE TABLE ai_process_record (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    bvid          VARCHAR(20)  NOT NULL COMMENT 'B站视频BV号',
    article_id    BIGINT       COMMENT '生成的文章ID，关联article表',
    process_time  DATETIME     NOT NULL COMMENT '处理时间',
    status        VARCHAR(20)  NOT NULL DEFAULT 'generated' COMMENT 'generated/published/rejected',
    ai_model      VARCHAR(50)  COMMENT '使用的AI模型',
    hot_score     DECIMAL(10,2) COMMENT '处理时的热度评分',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_bvid (bvid),
    INDEX idx_article_id (article_id)
) COMMENT 'AI处理记录-追踪每个热点是否已生成文章';
```

---

## 二、实现步骤

### Phase 1：数据基础设施（建表 + 定时抓取）

**目标**：让热点数据持续落库，不再一次性丢弃。

#### Step 1.1 建表

在 MySQL `big_event` 库中执行上述三条建表语句。

#### Step 1.2 初始化分区映射数据

从 [B站视频分区代码](https://github.com/SocialSisterYi/bilibili-API-collect/blob/master/docs/video/video_zone.md) 获取完整分区列表，生成 `partition_mapping` 的 INSERT 语句导入。未覆盖的 tname 统一归入 "其他"。

#### Step 1.3 创建 Java 实体与 Mapper

新建文件：

- `com.bubbles.pojo.entity.HotSnapshot`
- `com.bubbles.pojo.entity.PartitionMapping`
- `com.bubbles.pojo.entity.AiProcessRecord`
- `com.bubbles.server.mapper.HotSnapshotMapper` + XML
- `com.bubbles.server.mapper.PartitionMappingMapper` + XML
- `com.bubbles.server.mapper.AiProcessRecordMapper` + XML

#### Step 1.4 实现定时抓取服务

在 Spring Boot 中新建 `HotTopicSyncService`：

```java
@Service
public class HotTopicSyncService {

    @Scheduled(fixedDelay = 30 * 60 * 1000) // 每30分钟
    public void syncBilibiliHot() {
        // 1. 通过 WebClient 调 Python AI 服务 GET /api/bilibili/hot
        // 2. 对每条 hot item，查 partition_mapping 获取一级分区
        // 3. 计算单次热度评分
        // 4. 批量 INSERT 到 hot_snapshot 表
        // 5. 全部包装在 @Transactional 中
    }
}
```

在 `BigEventApplication` 上加 `@EnableScheduling`。

---

### Phase 2：评分引擎

**目标**：基于历史数据计算每个话题的综合热度分，选出真正值得写的 Top 话题。

#### 评分公式（V1）

```
综合评分 = BASE_SCORE × 时效衰减因子 × 趋势因子

BASE_SCORE = 播放量×0.30 + 点赞×0.20 + 投币×0.20 + 收藏×0.15 + 分享×0.15
             (各项先做 min-max 归一化到 [0,1])

时效衰减因子 = e^(-k × days_since_first_appear)
              k = 0.03 (约14天后降为原来的65%)

趋势因子 = 最近3次快照的平均分 / 前3次快照的平均分
         = 1.1 表示热度上升，= 0.9 表示热度下降
         新话题(不足6次快照) 按 1.0 处理
```

#### 实现：`HotScoreCalculator`

```java
@Component
public class HotScoreCalculator {

    /**
     * 计算单个话题的综合评分
     * @param bvid      视频BV号
     * @param lookbackDays 回溯天数 (默认60)
     */
    public double calculate(String bvid, int lookbackDays) {
        // 1. 从 hot_snapshot 表查该 bvid 近 N 天的所有快照
        // 2. 对每次快照的各项指标做归一化 → 计算 BASE_SCORE
        // 3. 计算时效衰减因子
        // 4. 计算趋势因子（对比近期 vs 早期快照的BASE_SCORE均值）
        // 5. 返回综合评分
    }

    /**
     * 获取当前 Top N 热点话题（去重、去已处理）
     */
    public List<HotTopicVO> getTopTopics(int topN, String partition) {
        // 1. 查最近一次快照中的所有 bvid
        // 2. 对每个 bvid 调 calculate()
        // 3. 排除 ai_process_record 中已存在的 bvid
        // 4. 按评分降序排列返回 topN
    }
}
```

#### min-max 归一化

对于单次快照中的所有条目：
```
norm_value = (原始值 - min) / (max - min)
```
各项指标独立归一化，避免播放量绝对优势碾压其他维度。

---

### Phase 3：AI Prompt 升级

**目标**：让 AI 拿到的不只是一个话题名，而是结构化的热点上下文，写出高质量、有针对性的文章。

#### 新的 System Prompt 模板

```text
你是一位专业的自媒体撰稿人，擅长基于热点事件撰写深度文章。

## 写作要求
- 文章长度：约 {length} 字
- 风格：{style_text}  
- 目标受众：{audience_text}
- 文章需要有吸引力的标题、引言、正文分论点（2-3个）、结语
- 使用 Markdown 格式，正文适度使用列表和加粗

## 热点背景（务必紧密结合）
- 热点标题：{hot_title}
- 分区：{partition}
- UP主：{author}
- 当前播放量：{view_count}，点赞：{like_count}，投币：{coin_count}
- 综合热度评分：{hot_score}（满分100）
- 排名：第 {rank} 名

## 注意事项
- 不要简单复述视频内容，要从事件出发做深度分析或观点输出
- 不要提"根据B站视频"、"UP主说"之类的话术，用"近期"、"据了解"等新闻体
- 文中可适度加入对同类现象的横向对比
```

#### 改动点

在 `writer_service.py` 中新增 `write_article_from_hot()` 方法：

```python
def write_article_from_hot(self, hot_item: dict, style: str, length: int) -> WriteArticleResponse:
    """基于热点数据撰写文章，prompt 包含完整热点上下文"""
    system_prompt = build_hot_article_prompt(hot_item, style, length)
    # 其余逻辑复用现有 _generate_article_content 的 API 调用部分
```

同时新增对应的 FastAPI 端点：
```python
@app.post("/api/writer/write-from-hot", tags=["AI撰稿"])
async def write_article_from_hot(request: WriteFromHotRequest):
    """基于热点数据撰写文章"""
```

---

### Phase 4：自动发布流水线

**目标**：串联"评分→筛选→撰稿→入库"全流程。

#### 新建 `AutoPublishService`

```java
@Service
public class AutoPublishService {

    private final HotScoreCalculator scoreCalculator;
    private final WriterAIService writerAIService;
    private final ArticleService articleService;
    private final AiProcessRecordMapper recordMapper;

    /**
     * 自动发布主流程
     * 可被定时任务或手动触发
     */
    @Transactional
    public List<Article> autoPublish(int topN, String partition) {
        // 1. 评分筛选：获取 Top N 话题
        List<HotTopicVO> topics = scoreCalculator.getTopTopics(topN, partition);

        List<Article> results = new ArrayList<>();
        for (HotTopicVO topic : topics) {
            // 2. 去重：ai_process_record 中已存在则跳过
            if (recordMapper.existsByBvid(topic.getBvid())) continue;

            // 3. 调 Python AI 服务的 write-from-hot 接口撰稿
            WriterResponseDTO response = writerAIService.writeFromHot(topic);

            // 4. 创建 Article（state = "draft"）
            Article article = Article.builder()
                .title(response.getTitle())
                .content(response.getContent())
                .coverImg(topic.getCoverUrl())
                .categoryId(mapPartitionToCategoryId(topic.getPartition()))
                .state("draft")
                .createUser(getSystemUserId())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
            articleService.addArticle(article);

            // 5. 记录处理状态
            AiProcessRecord record = AiProcessRecord.builder()
                .bvid(topic.getBvid())
                .articleId(article.getId())
                .processTime(LocalDateTime.now())
                .status("generated")
                .aiModel(response.getModelUsed())
                .hotScore(topic.getScore())
                .build();
            recordMapper.insert(record);

            results.add(article);
        }
        return results;
    }
}
```

#### 新增定时任务（与 Phase 1 的同步任务配合）

```java
// 在 HotTopicSyncService 中，同步完成后自动触发：
@Scheduled(fixedDelay = 30 * 60 * 1000)
public void syncAndPublish() {
    syncBilibiliHot();                  // Phase 1: 抓取热榜
    autoPublishService.autoPublish(5);  // Phase 4: 选 Top5 自动撰稿
}
```

#### 触发方式

| 方式 | 说明 |
|------|------|
| 定时自动 | 每 30 分钟自动执行 sync → publish |
| 手动触发 | 前端 `/user/writer/auto-publish` API 接收参数后手动执行 |
| 单篇选择 | 用户在 B站热榜页面勾选某条热点，点击"撰稿"按钮 |

---

### Phase 5：前端完善

**目标**：让用户可以在平台上浏览热点、选择热点、触发撰稿、查看结果。

#### Step 5.1 热点浏览页增强

修改 `BilibiliHot.vue`：

- 显示表格列增加：综合评分、分区标签、已处理状态标记
- 每行增加"撰稿"按钮（调用 write-from-hot 接口）
- 顶部增加筛选器（按分区过滤）
- 顶部增加"自动发布 Top5"按钮（调用 auto-publish 接口，带确认弹窗）

#### Step 5.2 新增文章管理过滤

修改 `ArticleManage.vue`：

- 列表增加"来源"列（AI生成 / 手动创建）
- 增加"AI 生成"筛选标签
- 文章状态有"草稿"/"已发布"，草稿增加"审核发布"按钮

#### Step 5.3 API 模块

在 `big-event-fronted/src/api/` 新建或修改：

- `bilibili.js` — 增加获取热榜列表（带评分）、触发自动发布
- `writer.js` — 增加 `POST /user/writer/write-from-hot`

---

## 三、文件清单

### 新建文件

| 文件 | 位置 |
|------|------|
| `HotSnapshot.java` | `com.bubbles.pojo.entity` |
| `PartitionMapping.java` | `com.bubbles.pojo.entity` |
| `AiProcessRecord.java` | `com.bubbles.pojo.entity` |
| `HotTopicVO.java` | `com.bubbles.pojo.vo` |
| `WriteFromHotRequest.java` | `com.bubbles.pojo.dto` |
| `HotSnapshotMapper.java` + XML | `server/mapper` + `resources/mapper` |
| `PartitionMappingMapper.java` + XML | `server/mapper` + `resources/mapper` |
| `AiProcessRecordMapper.java` + XML | `server/mapper` + `resources/mapper` |
| `HotTopicSyncService.java` | `com.bubbles.server.service.impl` |
| `HotScoreCalculator.java` | `com.bubbles.server.service.impl` |
| `AutoPublishService.java` | `com.bubbles.server.service.impl` |
| 建表 SQL | `resources/sql/init_ai_tables.sql` |
| 分区映射初始数据 SQL | `resources/sql/partition_mapping_data.sql` |

### 修改文件

| 文件 | 改动点 |
|------|--------|
| `BigEventApplication.java` | 加 `@EnableScheduling` |
| `WriterAIService.java` + `Impl` | 新增 `writeFromHot()` 方法 |
| `writer_service.py` | 新增 `write_article_from_hot()` 方法，抽取 prompt 构建逻辑 |
| `main.py` | 新增 `/api/writer/write-from-hot` 端点 |
| `ArticleController.java` | 新增 `/user/article/auto-publish` 端点 |
| `BilibiliHot.vue` | 增加评分列、撰稿按钮、分区筛选 |
| `ArticleManage.vue` | 增加来源列、草稿审核功能 |
| `bilibili.js` (前端API) | 增加自动发布接口调用 |

---

## 四、配置项

在 `application.yml` 中新增：

```yaml
bubbles:
  ai-writer:
    auto-publish:
      enabled: false           # 全局开关，初期建议关闭
      top-n: 5                 # 每次自动发布选取的热点数量
      min-score: 60            # 最低评分阈值，低于此分不写
      partition: all           # 默认分区(或"科技","游戏")，all=全分区
    snapshot:
      sync-interval-min: 30    # 热榜同步间隔(分钟)
      retention-days: 60       # 历史快照保留天数
```

这些值通过 `@ConfigurationProperties` 绑定到 `AiWriterProperties` 类，供 `HotScoreCalculator` 和 `AutoPublishService` 使用。

---

## 五、实现顺序

| 阶段 | 内容 | 预估工作量 |
|------|------|-----------|
| **Phase 1** | 建表 + 实体/Mapper + 定时抓取落库 + 分区映射初始化 | 后端核心 |
| **Phase 2** | 评分引擎（归一化、衰减、趋势） | 算法逻辑 |
| **Phase 3** | AI prompt 升级（热点上下文注入） | Python 端改动 |
| **Phase 4** | 自动发布流水线（串联全流程） | 后端集成 |
| **Phase 5** | 前端页面完善 | 前端交互 |

建议严格按顺序推进，每个 Phase 完成后验证再进入下一个。
