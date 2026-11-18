<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyBorrows, returnBook } from '@/api/borrow'
import type { BorrowRecord } from '@/api/borrow'
import { resolveCover } from '@/utils/cover'

const loading = ref(false)
const records = ref<BorrowRecord[]>([])
const total = ref(0)

const query = reactive({ status: undefined as number | undefined, page: 1, size: 10 })

const statusMap: Record<number, { text: string; type: 'primary' | 'success' | 'danger' }> = {
  0: { text: '借出中', type: 'primary' },
  1: { text: '已还', type: 'success' },
  2: { text: '逾期', type: 'danger' },
}

async function fetchData() {
  loading.value = true
  try {
    const data = await getMyBorrows(query)
    records.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function handleReturn(row: BorrowRecord) {
  await ElMessageBox.confirm('确认归还该书吗？', '归还确认', { type: 'info' })
  await returnBook(row.id)
  ElMessage.success('归还成功')
  fetchData()
}

onMounted(fetchData)
</script>

<template>
  <div class="my-borrow page-container">
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; align-items: center; gap: 12px">
          <span>我的借阅</span>
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="() => { query.page = 1; fetchData() }">
            <el-option label="借出中" :value="0" />
            <el-option label="已还" :value="1" />
            <el-option label="逾期" :value="2" />
          </el-select>
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
        <el-table-column prop="bookTitle" label="书名" min-width="180" />
        <el-table-column prop="borrowTime" label="借出时间" width="170" />
        <el-table-column prop="dueTime" label="应还时间" width="170" />
        <el-table-column prop="returnTime" label="归还时间" width="170">
          <template #default="{ row }">{{ row.returnTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 2 || row.overdue ? 'danger' : statusMap[row.status]?.type">
              {{ row.status === 2 || row.overdue ? '逾期' : statusMap[row.status]?.text || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="success" size="small" @click="handleReturn(row)">还书</el-button>
            <span v-else class="text-muted">-</span>
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
  </div>
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
