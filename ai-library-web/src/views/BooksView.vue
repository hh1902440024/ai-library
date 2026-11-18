<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBooks, getCategories } from '@/api/book'
import type { Book, Category } from '@/api/book'
import BookCard from '@/components/BookCard.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const books = ref<Book[]>([])
const total = ref(0)
const categories = ref<Category[]>([])

const query = reactive({
  keyword: (route.query.keyword as string) || '',
  categoryId: route.query.categoryId ? Number(route.query.categoryId) : undefined,
  available: undefined as boolean | undefined,
  page: 1,
  size: 12,
})

async function fetchData() {
  loading.value = true
  try {
    const data = await getBooks(query)
    books.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  fetchData()
}

onMounted(async () => {
  categories.value = await getCategories().catch(() => [])
  fetchData()
})
</script>

<template>
  <div class="books page-container">
    <!-- 筛选区 -->
    <el-card class="filter-card" shadow="never">
      <el-form inline @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="书名 / 作者" clearable style="width: 200px" @keyup.enter="search" @clear="search" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 160px" @change="search">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="仅看可借">
          <el-switch v-model="query.available" @change="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 图书卡片 -->
    <div v-loading="loading" class="grid">
      <el-row :gutter="16">
        <el-col v-for="book in books" :key="book.id" :xs="12" :sm="8" :md="6" style="margin-bottom: 16px">
          <BookCard :book="book" @click="router.push(`/books/${book.id}`)" />
        </el-col>
      </el-row>
      <el-empty v-if="!loading && !books.length" description="没有找到相关图书" />
    </div>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchData"
        @size-change="search"
      />
    </div>
  </div>
</template>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}
.pagination {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}
</style>
