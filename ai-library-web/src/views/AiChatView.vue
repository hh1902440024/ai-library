<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ChatMessage from '@/components/ChatMessage.vue'
import type { ChatMessage as ChatMessageItem, ChatSession } from '@/api/ai'
import { chatStream, deleteSession, getMessages, getSessions } from '@/api/ai'
import { useUserStore } from '@/store/user'

const store = useUserStore()

const sessions = ref<ChatSession[]>([])
const currentSessionId = ref<number | null>(null)
const messages = ref<ChatMessageItem[]>([])
const input = ref('')
const sending = ref(false)
const listEl = ref<HTMLElement>()

onMounted(loadSessions)

async function loadSessions() {
  sessions.value = await getSessions().catch(() => [])
}

async function openSession(session: ChatSession) {
  if (sending.value) return
  currentSessionId.value = session.id
  messages.value = await getMessages(session.id).catch(() => [])
  scrollToBottom()
}

function newChat() {
  if (sending.value) return
  currentSessionId.value = null
  messages.value = []
}

async function handleDeleteSession(session: ChatSession) {
  await ElMessageBox.confirm(`删除会话「${session.title}」？`, '提示', { type: 'warning' })
  await deleteSession(session.id)
  if (currentSessionId.value === session.id) {
    currentSessionId.value = null
    messages.value = []
  }
  loadSessions()
}

function scrollToBottom() {
  nextTick(() => {
    listEl.value?.scrollTo({ top: listEl.value.scrollHeight, behavior: 'smooth' })
  })
}

/** 发送消息：fetch + ReadableStream 读取 SSE，边收边渲染（打字机效果） */
async function handleSend() {
  const content = input.value.trim()
  if (!content || sending.value) return
  if (!store.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  sending.value = true
  input.value = ''
  messages.value.push({ role: 'user', content })
  messages.value.push({ role: 'assistant', content: '' })
  // 从响应式数组中取代理对象再追加内容，绕过代理直接改原始对象不会触发视图更新
  const assistantMsg = messages.value[messages.value.length - 1] as ChatMessageItem
  scrollToBottom()

  try {
    await chatStream(
      currentSessionId.value,
      content,
      (sessionId) => {
        // 新会话：记住 id 并刷新侧栏
        currentSessionId.value = sessionId
        loadSessions()
      },
      (delta) => {
        assistantMsg.content += delta
        scrollToBottom()
      },
    )
  } catch {
    assistantMsg.content = assistantMsg.content || 'AI 助手暂时开小差了，请稍后再试'
  } finally {
    sending.value = false
    scrollToBottom()
  }
}
</script>

<template>
  <div class="ai-chat">
    <!-- 左侧：会话列表 -->
    <div class="sidebar">
      <el-button type="primary" class="new-btn" @click="newChat">
        <el-icon><Plus /></el-icon> 新对话
      </el-button>
      <div class="session-list">
        <div
          v-for="s in sessions"
          :key="s.id"
          class="session-item"
          :class="{ active: s.id === currentSessionId }"
          @click="openSession(s)"
        >
          <el-icon><ChatDotRound /></el-icon>
          <span class="title">{{ s.title }}</span>
          <el-icon class="del" @click.stop="handleDeleteSession(s)"><Delete /></el-icon>
        </div>
        <el-empty v-if="!sessions.length" description="暂无会话" :image-size="60" />
      </div>
    </div>

    <!-- 右侧：对话区 -->
    <div class="chat-area">
      <div ref="listEl" class="message-list">
        <el-empty
          v-if="!messages.length"
          description="你好，我是馆藏智能助手「小书」，可以帮你找书、荐书、解答借阅问题～"
        />
        <ChatMessage v-for="(msg, i) in messages" :key="i" :message="msg" />
      </div>

      <div class="input-area">
        <el-input
          v-model="input"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="输入你的问题，例如：想找一本讲人工智能入门的书"
          @keydown.enter.exact.prevent="handleSend"
        />
        <el-button type="primary" :loading="sending" @click="handleSend">发送</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ai-chat {
  display: flex;
  height: calc(100vh - 60px);
}
.sidebar {
  width: 240px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  padding: 12px;
  display: flex;
  flex-direction: column;
}
.new-btn {
  width: 100%;
}
.session-list {
  flex: 1;
  overflow-y: auto;
  margin-top: 12px;
}
.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}
.session-item:hover {
  background: #f5f7fa;
}
.session-item.active {
  background: #ecf5ff;
  color: #409eff;
}
.session-item .title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.session-item .del {
  display: none;
  color: #f56c6c;
}
.session-item:hover .del {
  display: block;
}
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
.input-area {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  align-items: flex-end;
}
</style>
