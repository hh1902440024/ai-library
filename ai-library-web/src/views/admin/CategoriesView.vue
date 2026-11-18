<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createCategory, deleteCategory, getCategories, updateCategory } from '@/api/book'
import type { Category } from '@/api/book'

const loading = ref(false)
const categories = ref<Category[]>([])

async function fetchData() {
  loading.value = true
  try {
    categories.value = await getCategories()
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  const { value: name } = await ElMessageBox.prompt('请输入分类名', '新增分类', {
    inputPattern: /\S+/, inputErrorMessage: '分类名不能为空',
  }).catch(() => ({ value: null as string | null }))
  if (!name) return
  await createCategory(name.trim())
  ElMessage.success('新增成功')
  fetchData()
}

async function handleEdit(row: Category) {
  const { value: name } = await ElMessageBox.prompt('请输入分类名', '修改分类', {
    inputValue: row.name,
    inputPattern: /\S+/, inputErrorMessage: '分类名不能为空',
  }).catch(() => ({ value: null as string | null }))
  if (!name || name === row.name) return
  await updateCategory(row.id, name.trim())
  ElMessage.success('修改成功')
  fetchData()
}

async function handleDelete(row: Category) {
  await ElMessageBox.confirm(
    `确认删除分类「${row.name}」？分类下有图书时将无法删除。`,
    '删除确认',
    { type: 'warning' },
  )
  await deleteCategory(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(fetchData)
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span>分类管理</span>
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon> 新增分类
        </el-button>
      </div>
    </template>

    <el-table v-loading="loading" :data="categories" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="分类名" min-width="160" />
      <el-table-column prop="bookCount" label="图书数量" width="120" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
