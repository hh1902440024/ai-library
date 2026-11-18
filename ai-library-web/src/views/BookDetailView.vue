<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBookDetail, getSimilarBooks } from '@/api/book'
import type { Book } from '@/api/book'
import { borrowBook } from '@/api/borrow'
import { useUserStore } from '@/store/user'
import { resolveCover } from '@/utils/cover'
import BookCard from '@/components/BookCard.vue'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const book = ref<Book | null>(null)
const similarBooks = ref<Book[]>([])
const borrowing = ref(false)

onMounted(async () => {
  book.value = await getBookDetail(route.params.id as string).catch(() => null)
  similarBooks.value = await getSimilarBooks(route.params.id as string).catch(() => [])
})

async function handleBorrow() {
  if (!store.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  await ElMessageBox.confirm(`借阅《${book.value?.title}》，借期默认 30 天，确认借阅？`, '借阅确认', { type: 'info' })
  borrowing.value = true
  try {
    await borrowBook(book.value!.id)
    ElMessage.success('借阅成功')
    book.value = await getBookDetail(book.value!.id)
  } finally {
    borrowing.value = false
  }
}
</script>

<template>
  <div class="detail page-container" v-if="book">
    <el-button text type="primary" @click="router.back()">
      <el-icon><Back /></el-icon> 返回
    </el-button>

    <el-card class="card-shadow" style="margin-top: 12px">
      <div class="layout">
        <div class="cover">
          <el-image :src="resolveCover(book)" fit="cover" class="cover-img">
            <template #error>
              <div class="cover-fallback">
                <el-icon :size="48"><Reading /></el-icon>
                <span>{{ book.title }}</span>
              </div>
            </template>
          </el-image>
        </div>

        <div class="info">
          <h2 style="margin-top: 0">{{ book.title }}</h2>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="作者">{{ book.author }}</el-descriptions-item>
            <el-descriptions-item label="出版社">{{ book.publisher || '-' }}</el-descriptions-item>
            <el-descriptions-item label="ISBN">{{ book.isbn }}</el-descriptions-item>
            <el-descriptions-item label="出版日期">{{ book.publishDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="总库存">{{ book.totalStock }}</el-descriptions-item>
            <el-descriptions-item label="可借库存">
              <el-tag size="small" :type="book.availableStock > 0 ? 'success' : 'danger'">{{ book.availableStock }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <div class="tags" v-if="book.tags">
            <el-tag v-for="tag in book.tags.split(',')" :key="tag" size="small" type="info" style="margin-right: 6px">
              {{ tag.trim() }}
            </el-tag>
          </div>

          <div class="desc">
            <h4>简介</h4>
            <p class="text-muted">{{ book.description || '暂无简介' }}</p>
          </div>

          <el-button
            type="primary"
            size="large"
            :disabled="book.availableStock <= 0"
            :loading="borrowing"
            @click="handleBorrow"
          >
            {{ book.availableStock > 0 ? '立即借阅' : '暂不可借' }}
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 同分类推荐 -->
    <el-card v-if="similarBooks.length" class="card-shadow" style="margin-top: 16px">
      <template #header>同分类推荐</template>
      <el-row :gutter="16">
        <el-col v-for="b in similarBooks" :key="b.id" :xs="12" :sm="8" :md="4">
          <BookCard :book="b" @click="router.push(`/books/${b.id}`)" />
        </el-col>
      </el-row>
    </el-card>
  </div>
  <div v-else class="page-container">
    <el-empty description="图书不存在或加载中" />
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  gap: 28px;
}
.cover-img {
  width: 220px;
  height: 308px;
  border-radius: 8px;
  display: block;
}
.cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: #f0f2f5;
  color: #c0c4cc;
  padding: 0 16px;
  text-align: center;
  font-size: 14px;
}
.info {
  flex: 1;
}
.tags {
  margin: 14px 0;
}
.desc h4 {
  margin-bottom: 6px;
}
</style>
