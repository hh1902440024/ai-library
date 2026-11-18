<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import type { ChatMessage } from '@/api/ai'

const props = defineProps<{ message: ChatMessage }>()

// 关闭 html 原样输出，防 XSS
const md = new MarkdownIt({ html: false, linkify: true, breaks: true })

// computed 包装：内容流式追加时重新渲染（打字机效果）
const rendered = computed(() => md.render(props.message.content || ''))
</script>

<template>
  <div class="chat-message" :class="message.role">
    <el-avatar v-if="message.role === 'assistant'" :size="34" class="avatar assistant-avatar">
      <el-icon><Reading /></el-icon>
    </el-avatar>
    <el-avatar v-else :size="34" class="avatar user-avatar">
      <el-icon><User /></el-icon>
    </el-avatar>
    <div class="bubble">
      <div v-if="message.role === 'assistant'" class="content markdown-body" v-html="rendered" />
      <div v-else class="content">{{ message.content }}</div>
    </div>
  </div>
</template>

<style scoped>
.chat-message {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}
.chat-message.user {
  flex-direction: row-reverse;
}
.avatar {
  flex-shrink: 0;
}
.assistant-avatar {
  background: #409eff;
  color: #fff;
}
.user-avatar {
  background: #67c23a;
  color: #fff;
}
.bubble {
  max-width: 72%;
  padding: 10px 14px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e4e7ed;
  font-size: 14px;
  line-height: 1.7;
}
.user .bubble {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}
.markdown-body :deep(p) {
  margin: 0 0 8px;
}
.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 4px 0;
  padding-left: 20px;
}
.markdown-body :deep(code) {
  background: #f5f7fa;
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 13px;
}
</style>
