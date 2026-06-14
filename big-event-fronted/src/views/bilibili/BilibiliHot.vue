<template>
  <div class="bilibili-hot-container">
    <!-- 顶部操作栏 -->
    <el-card class="header-card">
      <template #header>
        <div class="card-header">
          <span class="title">B站热点分析</span>
          <el-tag v-if="dataSource" :type="dataSource === 'uapipro' ? 'success' : 'warning'">
            {{ dataSource === 'uapipro' ? 'UApiPro' : 'Mock' }}
          </el-tag>
        </div>
      </template>
      <div class="header-controls">
        <el-select v-model="partition" placeholder="分区筛选" style="width: 140px" @change="fetchTopics">
          <el-option label="全部" value="all" />
          <el-option label="科技" value="科技" />
          <el-option label="游戏" value="游戏" />
          <el-option label="生活" value="生活" />
          <el-option label="娱乐" value="娱乐" />
          <el-option label="知识" value="知识" />
          <el-option label="影视" value="影视" />
          <el-option label="动漫" value="动漫" />
          <el-option label="体育" value="体育" />
          <el-option label="其他" value="其他" />
        </el-select>
        <el-button :icon="Refresh" @click="fetchTopics" :loading="loading">
          刷新评分
        </el-button>
        <el-button type="primary" :icon="Refresh" @click="handleSync" :loading="syncing">
          {{ syncing ? '同步中...' : '立即同步' }}
        </el-button>
        <el-button type="success" :icon="MagicStick" @click="handleAutoPublish" :loading="publishing"
                   :disabled="publishing">
          {{ publishing ? '发布中...' : '自动发布 Top' + publishCount }}
        </el-button>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <el-card class="stats-card" v-if="topics.length > 0">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ topics.length }}</div>
            <div class="stat-label">热点话题</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ avgScore }}</div>
            <div class="stat-label">平均评分</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ formatHotValue(totalViews) }}</div>
            <div class="stat-label">总播放量</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ processedCount }} / {{ topics.length }}</div>
            <div class="stat-label">已处理 / 总计</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 热点列表 -->
    <el-card class="list-card" v-loading="loading">
      <template #header>
        <div class="list-header">
          <span>热点话题排行（基于综合评分）</span>
          <span class="update-note">评分综合考虑播放量、互动数据、时效衰减和热度趋势</span>
        </div>
      </template>

      <div class="hot-list" v-if="topics.length > 0">
        <div
          v-for="(item, index) in topics"
          :key="item.bvid"
          class="hot-item"
          :class="[getRankClass(index), { processed: item.alreadyProcessed }]"
        >
          <div class="rank-badge">{{ index + 1 }}</div>

          <div class="item-cover" v-if="item.coverUrl">
            <img :src="item.coverUrl" :alt="item.title" @error="handleImageError" referrerpolicy="no-referrer" />
          </div>

          <div class="item-content">
            <div class="item-title" @click="openBilibili(item)" :title="item.description || item.title">
              {{ item.title }}
            </div>
            <div class="item-meta">
              <el-tag size="small" type="info">{{ item.partitionTag }}</el-tag>
              <span class="meta-author" v-if="item.author">UP: {{ item.author }}</span>
              <span class="meta-pubdate" v-if="item.pubDate">{{ formatPubDate(item.pubDate) }}</span>
              <span class="meta-view">播放 {{ formatHotValue(item.viewCount) }}</span>
              <span class="meta-like">赞 {{ formatHotValue(item.likeCount) }}</span>
              <el-tag v-if="item.alreadyProcessed" size="small" type="success">已生成</el-tag>
            </div>
          </div>

          <div class="item-score">
            <div class="score-value" :class="getScoreClass(item.score)">
              {{ item.score }}
            </div>
            <div class="score-label">综合评分</div>
          </div>

          <div class="item-actions">
            <el-link
              type="primary"
              :href="item.url"
              target="_blank"
              v-if="item.url"
              :underline="false"
              style="margin-right: 12px"
            >
              B站原链
            </el-link>
            <el-button
              type="primary"
              size="small"
              :icon="Edit"
              @click="handleWriteArticle(item)"
              :loading="writingBvid === item.bvid"
              :disabled="item.alreadyProcessed"
            >
              {{ item.alreadyProcessed ? '已生成' : '撰稿' }}
            </el-button>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无热点数据，请先确认热榜同步服务已运行" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, MagicStick, Edit } from '@element-plus/icons-vue'
import { getHotTopics, syncHotTopics, formatHotValue } from '/src/api/bilibili'
import { writeFromHot, autoPublish } from '/src/api/writer'

const topics = ref([])
const loading = ref(false)
const publishing = ref(false)
const writingBvid = ref(null)
const syncing = ref(false)
const partition = ref('all')
const publishCount = ref(5)

const dataSource = computed(() => {
  return '' // 评分接口不返回source字段
})

const avgScore = computed(() => {
  if (topics.value.length === 0) return 0
  const sum = topics.value.reduce((s, t) => s + (t.score || 0), 0)
  return Math.round(sum / topics.value.length * 10) / 10
})

const totalViews = computed(() => {
  return topics.value.reduce((s, t) => s + (t.viewCount || 0), 0)
})

const processedCount = computed(() => {
  return topics.value.filter(t => t.alreadyProcessed).length
})

const getRankClass = (index) => {
  if (index === 0) return 'rank-1'
  if (index === 1) return 'rank-2'
  if (index === 2) return 'rank-3'
  return ''
}

const getScoreClass = (score) => {
  if (score >= 80) return 'score-high'
  if (score >= 60) return 'score-mid'
  return 'score-low'
}

const fetchTopics = async () => {
  loading.value = true
  try {
    const result = await getHotTopics({ topN: 20, partition: partition.value })
    topics.value = result.data || []
    if (topics.value.length === 0) {
      ElMessage.info('暂无热点数据，请点击"立即同步"从B站拉取热榜数据')
    }
  } catch (error) {
    console.error('获取热点话题失败:', error)
    ElMessage.error('获取热点数据失败，请检查后端服务')
  } finally {
    loading.value = false
  }
}

const handleSync = async () => {
  syncing.value = true
  try {
    await syncHotTopics()
    ElMessage.success('热榜同步完成')
    await fetchTopics()
  } catch (error) {
    console.error('同步热榜失败:', error)
    ElMessage.error('同步失败，请检查 Python AI 服务是否已启动')
  } finally {
    syncing.value = false
  }
}

const handleWriteArticle = async (item) => {
  writingBvid.value = item.bvid
  try {
    const result = await writeFromHot({
      bvid: item.bvid,
      title: item.title,
      partition: item.partitionTag,
      category: item.tname,
      author: item.author,
      cover_url: item.coverUrl || '',
      view_count: item.viewCount,
      like_count: item.likeCount,
      coin_count: item.coinCount,
      favorite_count: item.favoriteCount,
      share_count: item.shareCount,
      danmaku_count: item.danmakuCount,
      reply_count: item.replyCount,
      hot_score: item.score,
      rank: 0,
      description: item.description || '',
      length: 800,
      style: 'neutral',
      audience: 'general',
      generate_summary: true
    })
    if (result.code === 0) {
      ElMessage.success(`文章已生成并同步到文章管理：${result.data.title}`)
      // 标记为已处理
      item.alreadyProcessed = true
    }
  } catch (error) {
    console.error('撰稿失败:', error)
    ElMessage.error('AI撰稿失败，请检查AI服务')
  } finally {
    writingBvid.value = null
  }
}

const handleAutoPublish = () => {
  ElMessageBox.confirm(
    `将自动从热榜中选取 Top ${publishCount.value} 条热点，调用 AI 生成文章并保存为草稿。确认继续？`,
    '自动发布',
    { confirmButtonText: '确认', cancelButtonText: '取消', type: 'info' }
  ).then(async () => {
    publishing.value = true
    try {
      const result = await autoPublish(publishCount.value, partition.value, 60)
      if (result.code === 0) {
        const count = result.data ? result.data.length : 0
        ElMessage.success(`自动发布完成！共生成 ${count} 篇文章`)
        await fetchTopics()
      } else {
        ElMessage.error(result.message || '自动发布失败')
      }
    } catch (error) {
      console.error('自动发布失败:', error)
      ElMessage.error('自动发布失败，请检查服务')
    } finally {
      publishing.value = false
    }
  }).catch(() => {})
}

const openBilibili = (item) => {
  if (item.url) {
    window.open(item.url, '_blank')
  }
}

const formatPubDate = (dateStr) => {
  if (!dateStr) return ''
  if (typeof dateStr === 'string') {
    // Java Jackson format: "yyyy-MM-dd HH:mm" or ISO "2024-12-15T10:30:00"
    return dateStr.substring(0, 10)
  }
  if (Array.isArray(dateStr)) {
    // Jackson array format fallback: [2024, 12, 15, 10, 30]
    const [y, m, d] = dateStr
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
  }
  return ''
}

const handleImageError = (e) => {
  e.target.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="320" height="180" viewBox="0 0 320 180"><rect fill="%23f0f0f0" width="320" height="180"/><text x="50%" y="50%" fill="%23999" text-anchor="middle" dy=".3em">暂无封面</text></svg>'
}

onMounted(() => {
  fetchTopics()
})
</script>

<style scoped>
.bilibili-hot-container {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: bold;
}

.header-controls {
  display: flex;
  gap: 12px;
  align-items: center;
}

.stats-card {
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  padding: 12px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.list-card {
  margin-bottom: 20px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.update-note {
  font-size: 12px;
  color: #909399;
}

.hot-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hot-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  background: #f5f7fa;
  transition: all 0.2s;
}

.hot-item:hover {
  background: #e6f1ff;
  transform: translateX(4px);
}

.hot-item.processed {
  opacity: 0.7;
}

.hot-item.rank-1 {
  background: linear-gradient(135deg, #fff5e6, #fff0d6);
  border: 1px solid #ffd700;
}

.hot-item.rank-2 {
  background: linear-gradient(135deg, #f0f4f8, #e9ecef);
  border: 1px solid #c0c4cc;
}

.hot-item.rank-3 {
  background: linear-gradient(135deg, #fff5f0, #ffebe0);
  border: 1px solid #e67e22;
}

.rank-badge {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-weight: bold;
  font-size: 16px;
  background: #409eff;
  color: white;
  margin-right: 16px;
  flex-shrink: 0;
}

.rank-1 .rank-badge {
  background: linear-gradient(135deg, #ffd700, #ffb347);
}

.rank-2 .rank-badge {
  background: linear-gradient(135deg, #c0c4cc, #909399);
}

.rank-3 .rank-badge {
  background: linear-gradient(135deg, #e67e22, #d35400);
}

.item-cover {
  width: 100px;
  height: 56px;
  border-radius: 6px;
  overflow: hidden;
  margin-right: 16px;
  flex-shrink: 0;
}

.item-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 6px;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-title:hover {
  color: #409eff;
}

.item-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
  align-items: center;
}

.meta-author {
  color: #606266;
}

.meta-pubdate {
  color: #909399;
}

.meta-view, .meta-like {
  color: #909399;
}

.item-score {
  text-align: center;
  margin: 0 20px;
  flex-shrink: 0;
}

.score-value {
  font-size: 22px;
  font-weight: bold;
}

.score-value.score-high {
  color: #f56c6c;
}

.score-value.score-mid {
  color: #e6a23c;
}

.score-value.score-low {
  color: #909399;
}

.score-label {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.item-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
</style>
