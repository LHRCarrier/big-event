<template>
  <el-card class="result-column-card">
    <template #header>
      <div class="card-header">
        <span>{{ title }}</span>
        <span class="header-tags">
          <el-tag v-if="tagType" :type="tagType" size="small">{{ knowledge ? '知识库' : '标准' }}</el-tag>
          <el-tag v-if="info?.modelUsed" type="info" size="small">{{ info.modelUsed }}</el-tag>
        </span>
      </div>
    </template>
    <div class="result-content">
      <div class="article-content markdown-body" v-html="renderedHtml"></div>
      <span v-if="writing" class="typing-indicator">
        <span class="dot"></span>
        <span class="dot"></span>
        <span class="dot"></span>
      </span>
      <div class="result-footer">
        <div class="result-info">
          <el-text type="info" size="small">字数: {{ wordCount }} 字</el-text>
          <el-text v-if="info?.generatedAt" type="info" size="small">{{ formatDate(info.generatedAt) }}</el-text>
        </div>
        <el-button type="primary" size="small" :disabled="!content" @click="$emit('copy')">复制内容</el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { marked } from 'marked'

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true
})

const props = defineProps({
  title: { type: String, default: '' },
  tagType: { type: String, default: '' },
  knowledge: { type: Boolean, default: false },
  content: { type: String, default: '' },
  info: { type: Object, default: null },
  writing: { type: Boolean, default: false },
  wordCount: { type: Number, default: 0 }
})

defineEmits(['copy'])

const renderedHtml = computed(() => {
  if (!props.content) return ''
  return marked.parse(props.content)
})

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}
</script>

<style scoped>
.result-column-card {
  height: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-tags {
  display: flex;
  gap: 8px;
  align-items: center;
}
.result-content {
  padding: 10px;
}
.article-content {
  min-height: 200px;
}
.markdown-body {
  line-height: 1.8;
  color: #303133;
  word-break: break-word;
}
.markdown-body :deep(h1) {
  font-size: 22px; font-weight: bold; margin: 20px 0 12px;
  padding-bottom: 8px; border-bottom: 1px solid #eee;
}
.markdown-body :deep(h2) {
  font-size: 18px; font-weight: bold; margin: 18px 0 10px;
}
.markdown-body :deep(h3) {
  font-size: 16px; font-weight: bold; margin: 16px 0 8px;
}
.markdown-body :deep(p) { margin: 8px 0; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { padding-left: 24px; margin: 8px 0; }
.markdown-body :deep(li) { margin: 4px 0; }
.markdown-body :deep(blockquote) {
  border-left: 3px solid #409EFF;
  padding: 4px 12px; margin: 12px 0;
  color: #606266; background: #f5f7fa;
}
.markdown-body :deep(code) {
  background: #f5f7fa; color: #e74c3c;
  padding: 2px 6px; border-radius: 3px;
  font-family: 'Consolas', 'Monaco', monospace; font-size: 13px;
}
.markdown-body :deep(pre) {
  background: #f5f7fa; padding: 12px 16px;
  border-radius: 6px; overflow-x: auto; margin: 12px 0;
}
.markdown-body :deep(pre code) {
  background: none; color: #303133; padding: 0;
}
.markdown-body :deep(table) {
  border-collapse: collapse; width: 100%; margin: 12px 0;
}
.markdown-body :deep(th), .markdown-body :deep(td) {
  border: 1px solid #ddd; padding: 8px 12px; text-align: left;
}
.markdown-body :deep(th) { background: #f5f7fa; font-weight: bold; }
.markdown-body :deep(hr) {
  border: none; border-top: 1px solid #eee; margin: 20px 0;
}
.markdown-body :deep(strong) { font-weight: bold; }
.markdown-body :deep(em) { font-style: italic; }
.result-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}
.result-info {
  display: flex;
  gap: 16px;
}

.typing-indicator {
  display: inline-block;
  padding-left: 8px;
  vertical-align: middle;
}
.typing-indicator .dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #409EFF;
  margin: 0 1px;
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
