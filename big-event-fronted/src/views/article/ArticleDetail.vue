<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { marked } from 'marked'
import { getArticleDetailService } from '/src/api/article'
import { getArticleCategoryListService } from '/src/api/article-category'

const router = useRouter()
const route = useRoute()

const article = ref(null)
const categoryName = ref('')
const loading = ref(true)

const fetchArticle = async () => {
  const id = route.query.id
  if (!id) {
    router.replace('/article/browse')
    return
  }
  loading.value = true
  try {
    const result = await getArticleDetailService(id)
    article.value = result.data
    if (article.value?.categoryId) {
      try {
        const cats = await getArticleCategoryListService({ page: 1, pageSize: 100 })
        const cat = (cats.data.records || []).find(c => c.id == article.value.categoryId)
        if (cat) categoryName.value = cat.categoryName
      } catch (e) { /* ignore */ }
    }
  } catch (e) {
    console.error('加载文章详情失败', e)
  } finally {
    loading.value = false
  }
}

const renderMarkdown = (content) => {
  if (!content) return ''
  return marked.parse(content)
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

const goBack = () => {
  router.push('/article/browse')
}

onMounted(fetchArticle)
</script>

<template>
  <div class="detail-container">
    <!-- 返回按钮 -->
    <div class="detail-nav">
      <el-button :icon="'ArrowLeft'" text @click="goBack">返回浏览</el-button>
    </div>

    <div v-if="loading" class="detail-loading">
      <el-skeleton :rows="10" animated />
    </div>

    <template v-else-if="article">
      <!-- 封面图 -->
      <div v-if="article.coverImg" class="detail-cover">
        <img :src="article.coverImg" :alt="article.title" referrerpolicy="no-referrer" @error="e => e.target.style.display='none'" />
      </div>

      <!-- 标题 -->
      <h1 class="detail-title">{{ article.title }}</h1>

      <!-- 元信息 -->
      <div class="detail-meta">
        <el-tag v-if="categoryName" size="small" type="info">{{ categoryName }}</el-tag>
        <span class="detail-date">{{ formatDate(article.createTime) }}</span>
      </div>

      <el-divider />

      <!-- Markdown 正文 -->
      <div class="markdown-body" v-html="renderMarkdown(article.content)" />
    </template>

    <!-- 文章不存在 -->
    <div v-else class="detail-empty">
      <el-empty description="文章不存在" />
      <el-button type="primary" @click="goBack">返回浏览</el-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.detail-container {
  max-width: 860px;
  margin: 0 auto;
}

.detail-nav {
  margin-bottom: 16px;
}

.detail-loading {
  padding: 40px 0;
}

.detail-cover {
  width: 100%;
  max-height: 400px;
  overflow: hidden;
  border-radius: 8px;
  margin-bottom: 24px;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.detail-title {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.4;
  margin: 0 0 16px;
  color: #303133;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.detail-date {
  font-size: 14px;
  color: #999;
}

.detail-empty {
  text-align: center;
  padding: 80px 0;
}

/* Markdown 渲染样式 */
.markdown-body {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
  word-break: break-word;

  :deep(h1) {
    font-size: 26px;
    font-weight: 700;
    margin: 24px 0 16px;
    padding-bottom: 8px;
    border-bottom: 1px solid #eee;
  }

  :deep(h2) {
    font-size: 22px;
    font-weight: 700;
    margin: 24px 0 14px;
  }

  :deep(h3) {
    font-size: 19px;
    font-weight: 600;
    margin: 20px 0 12px;
  }

  :deep(h4), :deep(h5), :deep(h6) {
    font-size: 17px;
    font-weight: 600;
    margin: 16px 0 10px;
  }

  :deep(p) {
    margin: 0 0 14px;
  }

  :deep(ul), :deep(ol) {
    padding-left: 24px;
    margin-bottom: 14px;
  }

  :deep(li) {
    margin-bottom: 4px;
  }

  :deep(blockquote) {
    margin: 16px 0;
    padding: 12px 20px;
    border-left: 4px solid #409eff;
    background: #f5f7fa;
    color: #666;

    p {
      margin: 0;
    }
  }

  :deep(code) {
    background: #f5f7fa;
    padding: 2px 6px;
    border-radius: 4px;
    font-family: 'Consolas', 'Monaco', monospace;
    font-size: 14px;
    color: #e96900;
  }

  :deep(pre) {
    background: #282c34;
    border-radius: 8px;
    padding: 16px 20px;
    overflow-x: auto;
    margin: 16px 0;

    code {
      background: none;
      padding: 0;
      color: #abb2bf;
      font-size: 14px;
      line-height: 1.6;
    }
  }

  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: 16px 0;

    th, td {
      border: 1px solid #e0e0e0;
      padding: 10px 14px;
      text-align: left;
    }

    th {
      background: #f5f7fa;
      font-weight: 600;
    }
  }

  :deep(img) {
    max-width: 100%;
    border-radius: 6px;
    margin: 12px 0;
  }

  :deep(hr) {
    border: none;
    border-top: 1px solid #eee;
    margin: 24px 0;
  }

  :deep(a) {
    color: #409eff;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  :deep(strong) {
    font-weight: 700;
  }
}
</style>
