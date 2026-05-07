<template>
  <div class="bilibili-hot-container">
    <el-card class="header-card">
      <template #header>
        <div class="card-header">
          <span class="title">📺 B站热点分析</span>
          <el-tag :type="dataSource === 'uapipro' ? 'success' : 'warning'">
            数据来源: {{ dataSource === 'uapipro' ? 'UApiPro' : 'Mock数据' }}
          </el-tag>
        </div>
      </template>
      <div class="header-controls">
        <el-button type="primary" :icon="Refresh" @click="fetchHotData" :loading="loading">
          刷新数据
        </el-button>
      </div>
    </el-card>

    <el-card class="stats-card" v-if="hotData">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ hotData.items.length }}</div>
            <div class="stat-label">热榜条目</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ formatHotValue(totalHot) }}</div>
            <div class="stat-label">总热度</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ formatHotValue(avgHot) }}</div>
            <div class="stat-label">平均热度</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ formatFetchedTime }}</div>
            <div class="stat-label">更新时间</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="list-card" v-loading="loading">
      <template #header>
        <div class="list-header">
          <span>
            🔥 B站实时热榜
            <span v-if="hotData" class="update-time">
              ({{ formatFetchedTime }}更新)
            </span>
          </span>
        </div>
      </template>
      
      <div class="hot-list" v-if="hotData && hotData.items.length > 0">
        <div 
          v-for="(item, index) in hotData.items" 
          :key="item.rank"
          class="hot-item"
          :class="getRankClass(index)"
        >
          <div class="rank-badge">{{ item.rank }}</div>
          <div class="item-cover" v-if="item.cover_url"> 
             <img :src="item.cover_url" :alt="item.title" @error="handleImageError" referrerpolicy="no-referrer" /> 
           </div>
          <div class="item-content">
            <div class="item-title" @click="openItem(item)">
              {{ item.title }}
            </div>
            <div class="item-meta">
              <span class="meta-category" v-if="item.category">{{ item.category }}</span>
              <span class="meta-author" v-if="item.author">UP主: {{ item.author }}</span>
              <span class="meta-hot">
                <span class="hot-icon">🔥</span>
                {{ formatHotValue(item.hot_value) }}
              </span>
            </div>
          </div>
          <div class="item-actions">
            <el-link 
              type="primary" 
              :href="item.url" 
              target="_blank"
              v-if="item.url"
            >
              查看详情
            </el-link>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getBilibiliHot, formatHotValue as formatHot } from '/src/api/bilibili'

const hotData = ref(null)
const loading = ref(false)

const dataSource = computed(() => {
  return hotData.value?.source || 'mock'
})

const totalHot = computed(() => {
  if (!hotData.value) return 0
  return hotData.value.items.reduce((sum, item) => sum + item.hot_value, 0)
})

const avgHot = computed(() => {
  if (!hotData.value || hotData.value.items.length === 0) return 0
  return Math.round(totalHot.value / hotData.value.items.length)
})

const formatFetchedTime = computed(() => {
  if (!hotData.value) return ''
  const date = new Date(hotData.value.fetched_at)
  return date.toLocaleString('zh-CN')
})

const getRankClass = (index) => {
  if (index === 0) return 'rank-1'
  if (index === 1) return 'rank-2'
  if (index === 2) return 'rank-3'
  return ''
}

const fetchHotData = async () => {
  loading.value = true
  try {
    const data = await getBilibiliHot({ limit: 20 })
    hotData.value = data
    ElMessage.success('数据获取成功')
  } catch (error) {
    console.error('获取B站热榜失败:', error)
    ElMessage.error('获取数据失败，请检查后端服务是否启动')
  } finally {
    loading.value = false
  }
}

const openItem = (item) => {
  if (item.url) {
    window.open(item.url, '_blank')
  } else {
    ElMessage.info('暂无链接')
  }
}

const handleImageError = (e) => {
  e.target.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="320" height="180" viewBox="0 0 320 180"><rect fill="%23f0f0f0" width="320" height="180"/><text x="50%" y="50%" fill="%23999" text-anchor="middle" dy=".3em">暂无封面</text></svg>'
}

const formatHotValue = (value) => {
  return formatHot(value)
}

onMounted(() => {
  fetchHotData()
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
  gap: 16px;
  align-items: center;
}

.stats-card {
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  padding: 16px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}

.list-card {
  margin-bottom: 20px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.update-time {
  font-size: 14px;
  color: #909399;
  margin-left: 8px;
}

.hot-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hot-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  background: #f5f7fa;
  transition: all 0.3s;
}

.hot-item:hover {
  background: #e6f1ff;
  transform: translateX(4px);
}

.hot-item.rank-1 {
  background: linear-gradient(135deg, #fff5e6 0%, #fff0d6 100%);
  border: 1px solid #ffd700;
}

.hot-item.rank-2 {
  background: linear-gradient(135deg, #f0f4f8 0%, #e9ecef 100%);
  border: 1px solid #c0c4cc;
}

.hot-item.rank-3 {
  background: linear-gradient(135deg, #fff5f0 0%, #ffebe0 100%);
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

.hot-item.rank-1 .rank-badge {
  background: linear-gradient(135deg, #ffd700 0%, #ffb347 100%);
}

.hot-item.rank-2 .rank-badge {
  background: linear-gradient(135deg, #c0c4cc 0%, #909399 100%);
}

.hot-item.rank-3 .rank-badge {
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%);
}

.item-cover {
  width: 120px;
  height: 68px;
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
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
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
  gap: 16px;
  font-size: 13px;
  color: #909399;
}

.meta-category {
  background: #ecf5ff;
  color: #409eff;
  padding: 2px 8px;
  border-radius: 4px;
}

.meta-author {
  color: #606266;
}

.meta-hot {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #f56c6c;
  font-weight: 500;
}

.item-actions {
  margin-left: 16px;
  flex-shrink: 0;
}
</style>
