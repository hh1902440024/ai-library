<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getBooks, getCategories, getHotBooks } from '@/api/book'
import type { Book, Category } from '@/api/book'
import { getRecommend } from '@/api/ai'
import BookCard from '@/components/BookCard.vue'
import ChatMessage from '@/components/ChatMessage.vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const store = useUserStore()
const keyword = ref('')
const categories = ref<Category[]>([])
const latestBooks = ref<Book[]>([])
const hotBooks = ref<Book[]>([])

// AI 个性化推荐
const recommendVisible = ref(false)
const recommendLoading = ref(false)
const recommendText = ref('')

onMounted(async () => {
  const [cats, hot, latest] = await Promise.all([
    getCategories().catch(() => []),
    getHotBooks(8).catch(() => []),
    getBooks({ page: 1, size: 8 }).catch(() => ({ total: 0, list: [] as Book[] })),
  ])
  categories.value = cats
  hotBooks.value = hot
  latestBooks.value = latest.list
})

function search() {
  router.push({ path: '/books', query: keyword.value ? { keyword: keyword.value } : {} })
}

async function loadRecommend() {
  recommendVisible.value = true
  recommendLoading.value = true
  try {
    recommendText.value = await getRecommend()
  } catch {
    recommendText.value = 'AI 助手暂时开小差了，请稍后再试'
  } finally {
    recommendLoading.value = false
  }
}
</script>

<template>
  <div class="home page-container">
    <!-- 搜索区 -->
    <div class="hero">
      <h1>AI 智能书屋</h1>
      <p class="slogan">从"人找书"到"书找人" —— 对话式找书 · 个性化推荐 · 智能摘要</p>
      <div class="search-box">
        <el-input
          v-model="keyword"
          size="large"
          placeholder="输入书名、作者，或用一句话描述你想找的书"
          clearable
          @keyup.enter="search"
        >
          <template #append>
            <el-button type="primary" @click="search">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
          </template>
        </el-input>
      </div>
      <div class="hero-actions">
        <el-button type="primary" round @click="router.push('/ai/chat')">
          <el-icon><ChatDotRound /></el-icon> AI 助手找书
        </el-button>
        <el-button v-if="store.isLoggedIn" round @click="loadRecommend">
          <el-icon><MagicStick /></el-icon> 为我推荐
        </el-button>
      </div>
    </div>

    <!-- 分类快捷入口 -->
    <div class="section">
      <h3>分类浏览</h3>
      <div class="category-list">
        <el-tag
          v-for="cat in categories"
          :key="cat.id"
          size="large"
          class="category-tag"
          effect="plain"
          @click="router.push({ path: '/books', query: { categoryId: cat.id } })"
        >
          {{ cat.name }}
        </el-tag>
        <span v-if="!categories.length" class="text-muted">暂无分类数据</span>
      </div>
    </div>

    <!-- 热门图书（按借阅量） -->
    <div class="section" v-if="hotBooks.length">
      <h3>🔥 热门借阅</h3>
      <el-row :gutter="16">
        <el-col v-for="book in hotBooks" :key="book.id" :xs="12" :sm="8" :md="6" style="margin-bottom: 16px">
          <BookCard :book="book" @click="router.push(`/books/${book.id}`)" />
        </el-col>
      </el-row>
    </div>

    <!-- 最新上架 -->
    <div class="section">
      <h3>新书上架</h3>
      <el-row :gutter="16">
        <el-col v-for="book in latestBooks" :key="book.id" :xs="12" :sm="8" :md="6" style="margin-bottom: 16px">
          <BookCard :book="book" @click="router.push(`/books/${book.id}`)" />
        </el-col>
      </el-row>
      <el-empty v-if="!latestBooks.length" description="暂无图书数据，等待管理员上架" />
    </div>

    <!-- AI 个性化推荐抽屉 -->
    <el-drawer v-model="recommendVisible" title="AI 为你推荐" size="420px">
      <div v-loading="recommendLoading" style="min-height: 120px">
        <ChatMessage :message="{ role: 'assistant', content: recommendText }" />
        <p class="text-muted" style="text-align: center">
          推荐基于你的借阅历史生成，<router-link to="/ai/chat">去 AI 助手</router-link> 可以继续对话追问
        </p>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.hero {
  text-align: center;
  padding: 48px 0 32px;
}
.hero h1 {
  margin: 0 0 8px;
}
.slogan {
  color: #909399;
  margin-bottom: 24px;
}
.search-box {
  max-width: 640px;
  margin: 0 auto;
}
.hero-actions {
  margin-top: 18px;
  display: flex;
  justify-content: center;
  gap: 8px;
}
.section {
  margin: 24px 0;
}
.category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}
.category-tag {
  cursor: pointer;
}
</style>
