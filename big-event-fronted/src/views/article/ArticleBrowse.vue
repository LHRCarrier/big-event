<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getArticleListService } from '/src/api/article'
import { getArticleCategoryListService } from '/src/api/article-category'

const router = useRouter()

const categories = ref([])
const activeCategoryId = ref(null)
const articles = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(12)
const loading = ref(false)

const fetchCategories = async () => {
  try {
    const result = await getArticleCategoryListService({ page: 1, pageSize: 100 })
    categories.value = result.data.records || []
  } catch (e) {
    console.error('加载分类失败', e)
  }
}

const fetchArticles = async () => {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value, state: '已发布' }
    if (activeCategoryId.value) params.categoryId = activeCategoryId.value
    const result = await getArticleListService(params)
    articles.value = result.data.records || []
    total.value = result.data.total || 0
  } catch (e) {
    console.error('加载文章失败', e)
  } finally {
    loading.value = false
  }
}

const getCategoryName = (categoryId) => {
  const cat = categories.value.find(c => c.id == categoryId)
  return cat ? cat.categoryName : ''
}

const goDetail = (id) => {
  router.push(`/article/read?id=${id}`)
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

const handleTabChange = () => {
  page.value = 1
  fetchArticles()
}

const handlePageChange = (newPage) => {
  page.value = newPage
  fetchArticles()
}

const handleSizeChange = (newSize) => {
  pageSize.value = newSize
  page.value = 1
  fetchArticles()
}

onMounted(() => {
  fetchCategories()
  fetchArticles()
})
</script>

<template>
  <div class="browse-container">
    <div class="browse-header">
      <h2>文章浏览</h2>
    </div>

    <!-- 分类标签 -->
    <div class="category-tabs">
      <el-tabs v-model="activeCategoryId" @tab-change="handleTabChange">
        <el-tab-pane label="全部" :name="null" />
        <el-tab-pane
          v-for="cat in categories"
          :key="cat.id"
          :label="cat.categoryName"
          :name="cat.id"
        />
      </el-tabs>
    </div>

    <!-- 文章卡片网格 -->
    <div v-loading="loading" class="article-grid">
      <div v-if="!loading && articles.length === 0" class="empty-state">
        <el-empty description="暂无文章" />
      </div>
      <div
        v-for="article in articles"
        :key="article.id"
        class="article-card"
        @click="goDetail(article.id)"
      >
        <div class="card-cover">
          <img v-if="article.coverImg" :src="article.coverImg" :alt="article.title" referrerpolicy="no-referrer" @error="e => e.target.style.display='none'" />
          <div v-else class="card-cover-placeholder">
            <span>{{ article.title?.charAt(0) || '文' }}</span>
          </div>
        </div>
        <div class="card-body">
          <h3 class="card-title">{{ article.title }}</h3>
          <div class="card-meta">
            <el-tag size="small" type="info">{{ getCategoryName(article.categoryId) }}</el-tag>
            <span class="card-date">{{ formatDate(article.createTime) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > 0" class="pagination-wrapper">
      <el-pagination
        :current-page="page"
        :page-size="pageSize"
        :page-sizes="[8, 12, 16, 20]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.browse-container {
  max-width: 1200px;
  margin: 0 auto;
}

.browse-header h2 {
  margin: 0 0 8px;
  font-size: 22px;
}

.category-tabs {
  margin-bottom: 20px;
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  min-height: 200px;
}

.article-card {
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #ebeef5;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  }
}

.card-cover {
  width: 100%;
  height: 160px;
  overflow: hidden;
  background: #f5f7fa;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.card-cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 42px;
  font-weight: bold;
}

.card-body {
  padding: 14px 16px;
}

.card-title {
  margin: 0 0 10px;
  font-size: 15px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: #303133;
}

.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-date {
  font-size: 12px;
  color: #999;
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}
</style>
