<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { exportBorrows, getAdminBorrows, markOverdue } from '@/api/borrow'
import type { BorrowRecord } from '@/api/borrow'
import { resolveCover } from '@/utils/cover'

const loading = ref(false)
const exporting = ref(false)
const marking = ref(false)
const records = ref<BorrowRecord[]>([])
const total = ref(0)

const query = reactive({
  status: undefined as number | undefined,
  keyword: '',
  page: 1,
  size: 10,
})

const statusMap: Record<number, { text: string; type: 'primary' | 'success' | 'danger' }> = {
  0: { text: '借出中', type: 'primary' },
  1: { text: '已还', type: 'success' },
  2: { text: '逾期', type: 'danger' },
}

async function fetchData() {
  loading.value = true
  try {
    const data = await getAdminBorrows(query)
    records.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  fetchData()
}

async function handleMarkOverdue() {
  marking.value = true
  try {
    const count = await markOverdue()
    ElMessage.success(count > 0 ? `已标记 ${count} 条逾期记录` : '当前没有需要标记的逾期记录')
    fetchData()
  } finally {
    marking.value = false
  }
}

async function handleExport() {
  exporting.value = true
  try {
    await exportBorrows({ status: query.status, keyword: query.keyword })
    ElMessage.success('导出成功，已开始下载')
  } finally {
    exporting.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <el-form inline @submit.prevent>
          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="search">
              <el-option label="借出中" :value="0" />
              <el-option label="已还" :value="1" />
              <el-option label="逾期" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model="query.keyword" placeholder="书名/用户名" clearable style="width: 180px" @keyup.enter="search" @clear="search" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="search">查询</el-button>
          </el-form-item>
        </el-form>
        <el-button type="warning" plain :loading="marking" @click="handleMarkOverdue" style="margin-right: 8px">
          <el-icon><AlarmClock /></el-icon> 执行逾期标记
        </el-button>
        <el-button type="success" :loading="exporting" @click="handleExport">
          <el-icon><Download /></el-icon> 导出 Excel
        </el-button>
      </div>
    </template>

    <el-table v-loading="loading" :data="records" stripe>
      <el-table-column prop="id" label="记录ID" width="90" />
      <el-table-column label="封面" width="76" align="center">
        <template #default="{ row }">
          <el-image
            v-if="row.bookIsbn"
            :src="resolveCover({ isbn: row.bookIsbn, cover: row.bookCover })"
            fit="cover"
            class="book-cover"
          >
            <template #error>
              <div class="book-cover-fallback"><el-icon><Reading /></el-icon></div>
            </template>
          </el-image>
          <div v-else class="book-cover-fallback book-cover"><el-icon><Reading /></el-icon></div>
        </template>
      </el-table-column>
      <el-table-column prop="username" label="用户" width="120" />
      <el-table-column prop="bookTitle" label="书名" min-width="160" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="分类" width="100" />
      <el-table-column prop="borrowTime" label="借出时间" width="165" />
      <el-table-column prop="dueTime" label="应还时间" width="165" />
      <el-table-column prop="returnTime" label="归还时间" width="165">
        <template #default="{ row }">{{ row.returnTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 2 || row.overdue ? 'danger' : statusMap[row.status]?.type">
            {{ row.status === 2 || row.overdue ? '逾期' : statusMap[row.status]?.text || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <div style="display: flex; justify-content: center; margin-top: 16px">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchData"
      />
    </div>
  </el-card>
</template>

<style scoped>
.book-cover {
  width: 40px;
  height: 56px;
  border-radius: 4px;
  background: #f0f2f5;
  display: block;
}
.book-cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}
</style>
