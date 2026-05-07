<template>
  <div class="writer-test-container">
    <el-card class="header-card">
      <template #header>
        <div class="card-header">
          <span>🤖 AI撰稿测试</span>
          <el-tag :type="serviceStatus === 'running' ? 'success' : 'danger'">
            {{ serviceStatus === 'running' ? '服务正常' : '服务不可用' }}
          </el-tag>
        </div>
      </template>
      <el-button @click="checkStatus" :loading="statusLoading">检查服务状态</el-button>
    </el-card>

    <el-card class="form-card">
      <template #header>
        <span>📝 撰稿参数</span>
      </template>
      <el-form :model="form" label-width="100px">
        <el-form-item label="话题">
          <el-input
            v-model="form.topic"
            placeholder="请输入文章话题，如：人工智能的发展趋势"
            clearable
          />
        </el-form-item>

        <el-form-item label="文章长度">
          <el-slider
            v-model="form.length"
            :min="100"
            :max="3000"
            :step="100"
            show-input
          />
        </el-form-item>

        <el-form-item label="文章风格">
          <el-select v-model="form.style" placeholder="请选择风格">
            <el-option label="中性客观" value="neutral" />
            <el-option label="正式严谨" value="formal" />
            <el-option label="轻松活泼" value="casual" />
            <el-option label="专业技术" value="technical" />
          </el-select>
        </el-form-item>

        <el-form-item label="目标受众">
          <el-select v-model="form.audience" placeholder="请选择受众">
            <el-option label="普通大众" value="general" />
            <el-option label="专业人士" value="professional" />
            <el-option label="学生群体" value="student" />
          </el-select>
        </el-form-item>

        <el-form-item label="生成摘要">
          <el-switch v-model="form.generateSummary" />
        </el-form-item>

        <el-form-item label="实时显示">
          <el-switch v-model="form.streamMode" />
          <span style="margin-left: 10px; color: #999; font-size: 12px;">开启后文章内容会实时显示</span>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            @click="handleWrite"
            :loading="writing"
            :disabled="!form.topic"
          >
            {{ writing ? (form.streamMode ? 'AI正在撰稿...' : 'AI撰稿中...') : '开始撰稿' }}
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="showResult" class="result-card">
      <template #header>
        <div class="card-header">
          <span>📄 生成结果</span>
          <el-tag type="info">模型: {{ result?.modelUsed || 'mock' }}</el-tag>
        </div>
      </template>

      <div class="result-content">
        <h2 class="article-title">{{ result?.title || form.topic }}</h2>

        <el-tag v-if="result?.summary" type="warning" class="summary-tag">
          摘要: {{ result.summary }}
        </el-tag>

        <el-divider content-position="left">文章内容</el-divider>

        <div class="article-content-wrapper">
          <div class="article-content" v-html="formatContent(displayContent)"></div>
          <span v-if="writing && form.streamMode" class="typing-indicator">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
          </span>
        </div>

        <div class="result-footer">
          <div class="result-info">
            <el-text type="info">生成时间: {{ formatDate(result?.generatedAt) }}</el-text>
            <el-text type="info" class="word-count">字数统计: {{ wordCount }} 字</el-text>
          </div>
          <el-button type="primary" size="small" @click="copyContent" :disabled="!displayContent">
            复制内容
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { writeArticle, checkWriterStatus, writeArticleStream } from '/src/api/writer.js'

const serviceStatus = ref('unknown')
const statusLoading = ref(false)
const writing = ref(false)
const result = ref(null)
const displayContent = ref('')
const showResult = ref(false)

const form = reactive({
  topic: '',
  length: 500,
  style: 'neutral',
  audience: 'general',
  generateSummary: true,
  streamMode: true
})

const wordCount = computed(() => {
  const content = displayContent.value || ''
  return content.length
})

const checkStatus = async () => {
  statusLoading.value = true
  try {
    const res = await checkWriterStatus()
    serviceStatus.value = res.data === 'AI服务运行正常' ? 'running' : 'error'
    ElMessage.success(res.data)
  } catch (error) {
    serviceStatus.value = 'error'
    ElMessage.error('检查服务状态失败')
  } finally {
    statusLoading.value = false
  }
}

const handleWrite = async () => {
  if (!form.topic) {
    ElMessage.warning('请输入话题')
    return
  }

  writing.value = true
  result.value = null
  displayContent.value = ''
  showResult.value = true

  if (form.streamMode) {
    writeArticleStream(
      {
        topic: form.topic,
        length: form.length,
        style: form.style,
        audience: form.audience,
        generateSummary: form.generateSummary
      },
      (chunk) => {
        displayContent.value += chunk
      },
      () => {
        writing.value = false
        result.value = {
          title: form.topic,
          content: displayContent.value,
          modelUsed: 'streaming',
          generatedAt: new Date().toISOString()
        }
        ElMessage.success('文章生成成功')
      },
      (error) => {
        writing.value = false
        console.error('流式请求失败:', error)
        ElMessage.error('文章生成失败')
      }
    )
  } else {
    try {
      const res = await writeArticle(form)
      result.value = res.data
      displayContent.value = res.data.content
      ElMessage.success('文章生成成功')
    } catch (error) {
      ElMessage.error('文章生成失败')
    } finally {
      writing.value = false
    }
  }
}

const resetForm = () => {
  form.topic = ''
  form.length = 500
  form.style = 'neutral'
  form.audience = 'general'
  form.generateSummary = true
  form.streamMode = true
  result.value = null
  displayContent.value = ''
  showResult.value = false
}

const formatContent = (content) => {
  if (!content) return ''
  return content
    .replace(/\n/g, '<br>')
    .replace(/^# (.*$)/gim, '<h1 style="font-size:24px;font-weight:bold;margin:16px 0;">$1</h1>')
    .replace(/^## (.*$)/gim, '<h2 style="font-size:20px;font-weight:bold;margin:14px 0;">$1</h2>')
    .replace(/^### (.*$)/gim, '<h3 style="font-size:18px;font-weight:bold;margin:12px 0;">$1</h3>')
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const copyContent = () => {
  if (displayContent.value) {
    navigator.clipboard.writeText(displayContent.value)
    ElMessage.success('内容已复制到剪贴板')
  }
}

checkStatus()
</script>

<style scoped>
.writer-test-container {
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

.form-card {
  margin-bottom: 20px;
}

.result-card {
  margin-bottom: 20px;
}

.result-content {
  padding: 10px;
}

.article-title {
  text-align: center;
  color: #303133;
  margin-bottom: 20px;
}

.summary-tag {
  display: block;
  margin-bottom: 20px;
  padding: 10px;
}

.article-content-wrapper {
  line-height: 1.8;
  color: #606266;
  min-height: 200px;
  position: relative;
}

.article-content {
  white-space: pre-wrap;
}

.result-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.result-info {
  display: flex;
  gap: 20px;
}

.word-count {
  margin-left: 20px;
}

.typing-indicator {
  display: inline-block;
  padding-left: 8px;
  vertical-align: middle;
}

.typing-indicator .dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #409EFF;
  margin: 0 2px;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator .dot:nth-child(1) { animation-delay: 0s; }
.typing-indicator .dot:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator .dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}
</style>