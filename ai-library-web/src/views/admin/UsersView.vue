<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, updateUserRole, updateUserStatus } from '@/api/user'
import type { AdminUser } from '@/api/user'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const loading = ref(false)
const users = ref<AdminUser[]>([])
const total = ref(0)

const query = reactive({ keyword: '', page: 1, size: 10 })

async function fetchData() {
  loading.value = true
  try {
    const data = await getUsers(query)
    users.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  fetchData()
}

async function handleStatus(row: AdminUser) {
  const action = row.status === 1 ? '禁用' : '启用'
  await ElMessageBox.confirm(`确认${action}用户「${row.username}」？`, '提示', { type: 'warning' })
  await updateUserStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(`${action}成功`)
  fetchData()
}

async function handleRole(row: AdminUser) {
  const action = row.role === 1 ? '降级为普通用户' : '提升为管理员'
  await ElMessageBox.confirm(`确认将用户「${row.username}」${action}？`, '角色调整', { type: 'warning' })
  await updateUserRole(row.id, row.role === 1 ? 0 : 1)
  ElMessage.success('角色调整成功')
  fetchData()
}

onMounted(fetchData)
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <el-form inline @submit.prevent>
        <el-form-item>
          <el-input v-model="query.keyword" placeholder="用户名/手机号" clearable style="width: 200px" @keyup.enter="search" @clear="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
        </el-form-item>
      </el-form>
    </template>

    <el-table v-loading="loading" :data="users" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="130" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="email" label="邮箱" min-width="150">
        <template #default="{ row }">{{ row.email || '-' }}</template>
      </el-table-column>
      <el-table-column label="角色" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.role === 1 ? 'warning' : 'info'">
            {{ row.role === 1 ? '管理员' : '普通用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="165" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <template v-if="row.id !== store.user?.id">
            <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" @click="handleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" @click="handleRole(row)">
              {{ row.role === 1 ? '降级' : '提升' }}
            </el-button>
          </template>
          <span v-else class="text-muted">当前账号</span>
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
