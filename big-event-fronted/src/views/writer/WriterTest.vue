<template>
  <div class="writer-test-container">
    <!-- 状态栏 -->
    <el-card class="header-card">
      <template #header>
        <div class="card-header">
          <span>AI撰稿测试</span>
          <el-tag :type="serviceStatus === 'running' ? 'success' : 'danger'" size="small">
            {{ serviceStatus === 'running' ? '服务正常' : '服务不可用' }}
          </el-tag>
        </div>
      </template>
      <el-button size="small" @click="checkStatus" :loading="statusLoading">检查服务状态</el-button>
    </el-card>

    <!-- 参数表单 -->
    <el-card class="form-card">
      <template #header>
        <span>撰稿参数</span>
      </template>
      <el-form :model="form" label-width="90px" label-position="top">
        <el-form-item label="用户要求">
          <el-input
            v-model="form.requirement"
            type="textarea"
            :rows="3"
            placeholder="描述你想要的写作内容、方向或要求，例如：写一篇关于AI在医疗领域应用的文章，要突出实际案例和数据"
          />
        </el-form-item>

        <el-row :gutter="16" class="toggle-row">
          <el-col :span="12">
            <el-form-item label="启用知识库">
              <el-switch v-model="form.useKnowledge" :disabled="isWriting" />
              <span class="toggle-hint">检索高质量范文作为风格参考</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="A/B对比模式">
              <el-switch v-model="form.abMode" :disabled="isWriting" />
              <span class="toggle-hint">同时生成有/无知识库两篇文章对比</span>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 高级参数折叠 -->
        <el-collapse v-model="advancedOpen">
          <el-collapse-item name="advanced">
            <template #title>
              高级参数<span class="collapse-hint">（可选，不设置则使用默认值）</span>
            </template>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="文章长度">
                  <span class="slider-hint">{{ form.length === 0 ? '不限制（最长5000字）' : form.length + ' 字' }}</span>
                  <el-slider
                    v-model="form.length"
                    :min="0"
                    :max="5000"
                    :step="100"
                    :disabled="isWriting"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="文章风格">
                  <el-select v-model="form.style" :disabled="isWriting">
                    <el-option label="中性客观" value="neutral" />
                    <el-option label="正式严谨" value="formal" />
                    <el-option label="轻松活泼" value="casual" />
                    <el-option label="文学诗意" value="literary" />
                    <el-option label="新闻纪实" value="journalistic" />
                    <el-option label="犀利锐评" value="sharp" />
                    <el-option label="专业技术" value="technical" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="目标受众">
                  <el-select v-model="form.audience" :disabled="isWriting">
                    <el-option label="普通大众" value="general" />
                    <el-option label="专业人士" value="professional" />
                    <el-option label="学生群体" value="student" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="生成摘要" style="padding-top: 6px;">
                  <el-switch v-model="form.generateSummary" :disabled="isWriting" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-collapse-item>
        </el-collapse>

        <el-form-item class="action-row">
          <el-button
            type="primary"
            size="large"
            @click="handleWrite"
            :loading="isWriting"
            :disabled="!form.requirement.trim()"
          >
            {{ writingLabel }}
          </el-button>
          <el-button size="large" @click="resetAll" :disabled="isWriting">重置</el-button>
          <el-button v-if="isWriting" type="warning" size="large" @click="stopWriting">停止生成</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 知识库匹配预览 -->
    <el-card v-if="knowledgeMatches && (showResult || knowledgeMatches.matched > 0)" class="match-card">
      <template #header>
        <div class="card-header">
          <span>知识库匹配预览</span>
          <el-tag :type="knowledgeMatches.matched > 0 ? 'success' : 'info'" size="small">
            {{ knowledgeMatches.matched > 0 ? `匹配 ${knowledgeMatches.matched} 篇` : '无匹配' }}
          </el-tag>
        </div>
      </template>
      <div v-if="knowledgeMatches.error" class="match-error">
        <el-text type="danger">匹配失败: {{ knowledgeMatches.error }}</el-text>
      </div>
      <div v-else-if="knowledgeMatches.matched === 0" class="match-empty">
        <el-text type="info">未找到匹配的高质量参考文章，将使用默认写作风格</el-text>
      </div>
      <div v-else class="match-list">
        <div v-for="(article, idx) in knowledgeMatches.articles" :key="idx" class="match-item">
          <div class="match-title">
            <el-tag size="small" type="warning" style="margin-right: 8px;">{{ (article.similarity * 100).toFixed(1) }}%</el-tag>
            <span>{{ article.title }}</span>
            <el-text v-if="article.author" type="info" size="small" style="margin-left: 8px;">— {{ article.author }}</el-text>
          </div>
          <div v-if="article.excerpt" class="match-excerpt">
            <el-text type="info" size="small">{{ article.excerpt }}</el-text>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 结果区域 -->
    <div v-if="showResult" class="result-area">
      <!-- A/B 双列 -->
      <el-row v-if="form.abMode" :gutter="16">
        <el-col :span="12">
          <result-column
            title="知识库增强"
            tag-type="success"
            :knowledge="true"
            :content="displayLeft"
            :info="resultLeft"
            :writing="writing.left"
            :word-count="wordCountLeft"
            @copy="copyContent(displayLeft)"
          />
        </el-col>
        <el-col :span="12">
          <result-column
            title="无知识库"
            tag-type="info"
            :knowledge="false"
            :content="displayRight"
            :info="resultRight"
            :writing="writing.right"
            :word-count="wordCountRight"
            @copy="copyContent(displayRight)"
          />
        </el-col>
      </el-row>

      <!-- 单列 -->
      <result-column
        v-else
        :title="form.useKnowledge ? '知识库增强' : '标准生成'"
        :tag-type="form.useKnowledge ? 'success' : ''"
        :knowledge="form.useKnowledge"
        :content="displayLeft"
        :info="resultLeft"
        :writing="writing.left"
        :word-count="wordCountLeft"
        @copy="copyContent(displayLeft)"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { writeArticle, checkWriterStatus, writeArticleStream, matchKnowledge } from '/src/api/writer.js'
import { useUserTokenStore } from '/src/stores/token.js'
import ResultColumn from './ResultColumn.vue'

const STORAGE_KEY = 'writer-test-state'

const tokenStore = useUserTokenStore()

// ── 服务状态 ──
const serviceStatus = ref('unknown')
const statusLoading = ref(false)

// ── 表单 ──
const defaultForm = () => ({
  requirement: '',
  length: 0,
  style: 'neutral',
  audience: 'general',
  generateSummary: true,
  useKnowledge: true,
  abMode: false
})

const form = reactive(defaultForm())
const advancedOpen = ref([])

// ── 写作状态 ──
const writing = reactive({ left: false, right: false })
const abortControllers = reactive({ left: null, right: null })

const isWriting = computed(() => writing.left || writing.right)

const writingLabel = computed(() => {
  if (!isWriting.value) return '开始撰稿'
  if (form.abMode) {
    const parts = []
    if (writing.left) parts.push('知识库')
    if (writing.right) parts.push('标准')
    return `正在生成...(${parts.join('+')})`
  }
  return 'AI正在撰稿...'
})

// ── 结果 ──
const resultLeft = ref(null)
const resultRight = ref(null)
const displayLeft = ref('')
const displayRight = ref('')
const showResult = ref(false)
const knowledgeMatches = ref(null)

const wordCountLeft = computed(() => displayLeft.value.length)
const wordCountRight = computed(() => displayRight.value.length)

// ── 会话状态持久化 ──
function saveState() {
  const state = {
    form: { ...form },
    advancedOpen: [...advancedOpen.value],
    showResult: showResult.value,
    resultLeft: resultLeft.value,
    resultRight: resultRight.value,
    displayLeft: displayLeft.value,
    displayRight: displayRight.value,
    knowledgeMatches: knowledgeMatches.value
  }
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state))
  } catch (e) { /* ignore quota */ }
}

function restoreState() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) return
    const state = JSON.parse(raw)
    Object.assign(form, state.form || {})
    advancedOpen.value = state.advancedOpen || []
    showResult.value = state.showResult || false
    resultLeft.value = state.resultLeft || null
    resultRight.value = state.resultRight || null
    displayLeft.value = state.displayLeft || ''
    displayRight.value = state.displayRight || ''
    knowledgeMatches.value = state.knowledgeMatches || null
  } catch (e) { /* ignore corrupt data */ }
}

// ── 操作 ──
const checkStatus = async () => {
  statusLoading.value = true
  try {
    const res = await checkWriterStatus()
    serviceStatus.value = res.data === 'AI服务运行正常' ? 'running' : 'error'
    ElMessage.success(res.data)
  } catch {
    serviceStatus.value = 'error'
    ElMessage.error('检查服务状态失败')
  } finally {
    statusLoading.value = false
  }
}

const handleWrite = async () => {
  const req = form.requirement.trim()
  if (!req) {
    ElMessage.warning('请输入用户要求')
    return
  }

  writing.left = true
  if (form.abMode) writing.right = true

  resultLeft.value = null
  resultRight.value = null
  displayLeft.value = ''
  displayRight.value = ''
  showResult.value = true

  // 先预览知识库匹配情况
  knowledgeMatches.value = null
  matchKnowledge(req, 5).then(m => {
    knowledgeMatches.value = m
    saveState()
  })

  const baseParams = {
    topic: req
  }

  // 只有展开高级参数面板时才发送自定义参数
  if (advancedOpen.value.includes('advanced')) {
    if (form.length > 0) baseParams.length = form.length
    baseParams.style = form.style
    baseParams.audience = form.audience
    baseParams.generateSummary = form.generateSummary
  }

  if (form.abMode) {
    // 并行：知识库增强 + 无知识库
    startStream({
      ...baseParams,
      useKnowledge: true
    }, 'left')

    startStream({
      ...baseParams,
      useKnowledge: false
    }, 'right')
  } else {
    startStream({
      ...baseParams,
      useKnowledge: form.useKnowledge
    }, 'left')
  }
}

function startStream(params, side) {
  // 使用 AbortController 支持中断
  const controller = new AbortController()
  abortControllers[side] = controller

  const onChunk = (chunk) => {
    if (side === 'left') {
      displayLeft.value += chunk
    } else {
      displayRight.value += chunk
    }
    saveState()
  }

  const onComplete = () => {
    writing[side] = false
    abortControllers[side] = null
    const content = side === 'left' ? displayLeft.value : displayRight.value
    const result = {
      title: params.topic,
      content,
      modelUsed: 'streaming',
      generatedAt: new Date().toISOString(),
      useKnowledge: params.useKnowledge
    }
    if (side === 'left') {
      resultLeft.value = result
    } else {
      resultRight.value = result
    }
    saveState()
    if (!isWriting.value) {
      ElMessage.success('文章生成完成')
    }
  }

  const onError = (error) => {
    writing[side] = false
    abortControllers[side] = null
    console.error(`[${side}] 流式请求失败:`, error)
    if (!isWriting.value) {
      ElMessage.error('文章生成失败: ' + error.message)
    }
    saveState()
  }

  // 通过 Spring Boot 代理（Vite proxy /api → localhost:8080）
  const token = tokenStore.token

  fetch('/api/user/writer/write/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    },
    body: JSON.stringify(params),
    signal: controller.signal
  })
  .then(response => {
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    const readChunk = () => {
      reader.read().then(({ done, value }) => {
        if (done) {
          onComplete()
          return
        }
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        for (const line of lines) {
          if (line.startsWith('data:')) {
            const chunk = line.slice(5)
            if (chunk) {
              onChunk(chunk)
            }
          }
        }
        readChunk()
      }).catch(error => {
        if (error.name === 'AbortError') {
          onComplete()
        } else {
          onError(error)
        }
      })
    }
    readChunk()
  })
  .catch(error => {
    if (error.name === 'AbortError') {
      onComplete()
    } else {
      onError(error)
    }
  })
}

const stopWriting = () => {
  if (abortControllers.left) {
    abortControllers.left.abort()
    writing.left = false
    abortControllers.left = null
  }
  if (abortControllers.right) {
    abortControllers.right.abort()
    writing.right = false
    abortControllers.right = null
  }
  ElMessage.info('已停止生成')
  saveState()
}

const resetAll = () => {
  Object.assign(form, defaultForm())
  advancedOpen.value = []
  resultLeft.value = null
  resultRight.value = null
  displayLeft.value = ''
  displayRight.value = ''
  showResult.value = false
  knowledgeMatches.value = null
  sessionStorage.removeItem(STORAGE_KEY)
}

const copyContent = (text) => {
  if (text) {
    navigator.clipboard.writeText(text)
    ElMessage.success('内容已复制到剪贴板')
  }
}

// ── 自动保存 ──
watch([() => form, advancedOpen, showResult, displayLeft, displayRight],
  () => { if (showResult.value || form.requirement) saveState() },
  { deep: true }
)

// ── 生命周期 ──
onMounted(() => {
  restoreState()
  checkStatus()
})

onBeforeUnmount(() => {
  stopWriting()
})
</script>

<style scoped>
.writer-test-container {
  padding: 20px;
  max-width: 1600px;
  margin: 0 auto;
}

.header-card { margin-bottom: 16px; }
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-card { margin-bottom: 16px; }
.match-card { margin-bottom: 16px; }

.toggle-row {
  margin-bottom: 8px;
}
.toggle-hint {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.match-error, .match-empty { padding: 8px 0; }
.match-list { display: flex; flex-direction: column; gap: 8px; }
.match-item { padding: 6px 0; border-bottom: 1px solid #f0f0f0; }
.match-item:last-child { border-bottom: none; }
.match-title { display: flex; align-items: center; margin-bottom: 4px; }
.match-excerpt { padding-left: 54px; }

.result-area { margin-bottom: 16px; }

.action-row {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

/* Element Plus collapse 头部调整 */
:deep(.el-collapse-item__header) {
  font-size: 13px;
  color: #909399;
  padding: 8px 0;
}
.collapse-hint {
  color: #c0c4cc;
  font-size: 12px;
  font-weight: normal;
  margin-left: 6px;
}
.slider-hint {
  display: block;
  color: #909399;
  font-size: 12px;
  margin-bottom: 4px;
}
:deep(.el-collapse-item__content) {
  padding-bottom: 0;
}
</style>
