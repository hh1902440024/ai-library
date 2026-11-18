<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createBook, deleteBook, generateSummary, getBookDetail,
  getBooks, getCategories, updateBook,
} from '@/api/book'
import { uploadImage } from '@/api/auth'
import type { Book, Category } from '@/api/book'
import { resolveCover } from '@/utils/cover'

const loading = ref(false)
const books = ref<Book[]>([])
const total = ref(0)
const categories = ref<Category[]>([])

const query = reactive({
  keyword: '',
  categoryId: undefined as number | undefined,
  page: 1,
  size: 10,
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

// ---------- 新增/修改弹窗 ----------
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  isbn: '',
  title: '',
  author: '',
  publisher: '',
  categoryId: undefined as number | undefined,
  cover: '',
  description: '',
  tags: '',
  totalStock: 1,
  publishDate: '',
})

const rules: FormRules = {
  isbn: [{ required: true, message: '请输入 ISBN', trigger: 'blur' }],
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  totalStock: [{ required: true, message: '请输入总库存', trigger: 'blur' }],
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { isbn: '', title: '', author: '', publisher: '', categoryId: undefined, cover: '', description: '', tags: '', totalStock: 1, publishDate: '' })
  dialogVisible.value = true
}

async function openEdit(row: Book) {
  const detail = await getBookDetail(row.id)
  editingId.value = row.id
  Object.assign(form, {
    isbn: detail.isbn,
    title: detail.title,
    author: detail.author,
    publisher: detail.publisher || '',
    categoryId: detail.categoryId,
    cover: detail.cover || '',
    description: detail.description || '',
    tags: detail.tags || '',
    totalStock: detail.totalStock,
    publishDate: detail.publishDate || '',
  })
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form, publishDate: form.publishDate || undefined }
    if (editingId.value) {
      await updateBook(editingId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await createBook(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: Book) {
  await ElMessageBox.confirm(`确认删除《${row.title}》？`, '删除确认', { type: 'warning' })
  await deleteBook(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

// ---------- AI 摘要 ----------
const summaryLoading = ref<number | null>(null)

async function handleSummary(row: Book) {
  const { value: points } = await ElMessageBox.prompt(
    '可填写目录/要点辅助生成（留空则根据书名作者概括）',
    `AI 生成《${row.title}》简介与标签`,
    { inputType: 'textarea', inputValue: '' },
  ).catch(() => ({ value: null as string | null }))
  if (points === null) return

  summaryLoading.value = row.id
  try {
    const result = await generateSummary(row.id, points)
    await ElMessageBox.confirm(
      `【生成的简介】\n${result.description}\n\n【生成的标签】\n${result.tags}\n\n确认回填到图书信息？`,
      'AI 生成结果',
      { confirmButtonText: '回填并保存', cancelButtonText: '取消', customStyle: { whiteSpace: 'pre-line' } as never },
    )
    await updateBook(row.id, { description: result.description, tags: result.tags })
    ElMessage.success('已回填')
    fetchData()
  } catch (e) {
    // 用户取消或接口报错（接口错误已在拦截器提示）
    if (e !== 'cancel' && e !== 'close') {
      // ignore
    }
  } finally {
    summaryLoading.value = null
  }
}

// ---------- 封面上传 ----------
const coverUploading = ref(false)

async function handleCoverUpload(options: { file: File }) {
  coverUploading.value = true
  try {
    form.cover = await uploadImage(options.file)
    ElMessage.success('封面已上传')
  } finally {
    coverUploading.value = false
  }
}

onMounted(async () => {
  categories.value = await getCategories().catch(() => [])
  fetchData()
})
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <el-form inline @submit.prevent>
          <el-form-item>
            <el-input v-model="query.keyword" placeholder="书名/作者/标签" clearable style="width: 200px" @keyup.enter="search" @clear="search" />
          </el-form-item>
          <el-form-item>
            <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 150px" @change="search">
              <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="search">查询</el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon> 新增图书
        </el-button>
      </div>
    </template>

    <el-table v-loading="loading" :data="books" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="封面" width="76" align="center">
        <template #default="{ row }">
          <el-image :src="resolveCover(row)" fit="cover" class="book-cover" :preview-src-list="[resolveCover(row)]" preview-teleported>
            <template #error>
              <div class="book-cover-fallback"><el-icon><Reading /></el-icon></div>
            </template>
          </el-image>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="书名" min-width="160" show-overflow-tooltip />
      <el-table-column prop="author" label="作者" width="120" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="分类" width="100" />
      <el-table-column prop="isbn" label="ISBN" width="140" />
      <el-table-column label="库存(可借/总)" width="110" align="center">
        <template #default="{ row }">{{ row.availableStock }}/{{ row.totalStock }}</template>
      </el-table-column>
      <el-table-column prop="tags" label="标签" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button
            size="small"
            type="primary"
            plain
            :loading="summaryLoading === row.id"
            @click="handleSummary(row)"
          >AI 摘要</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/修改弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '修改图书' : '新增图书'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="ISBN" prop="isbn">
              <el-input v-model="form.isbn" placeholder="如 9787111213826" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="书名" prop="title">
              <el-input v-model="form.title" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者" prop="author">
              <el-input v-model="form.author" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出版社">
              <el-input v-model="form.publisher" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择" style="width: 100%">
                <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出版日期">
              <el-date-picker
                v-model="form.publishDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="总库存" prop="totalStock">
              <el-input-number v-model="form.totalStock" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标签">
              <el-input v-model="form.tags" placeholder="逗号分隔，如：Java,并发" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面">
              <div class="cover-row">
                <el-image
                  :src="resolveCover({ isbn: form.isbn, cover: form.cover })"
                  fit="cover"
                  class="cover-preview"
                />
                <el-upload
                  :show-file-list="false"
                  :http-request="handleCoverUpload"
                  accept="image/png,image/jpeg,image/webp,image/gif"
                >
                  <el-button :loading="coverUploading">
                    <el-icon><Upload /></el-icon> {{ form.cover ? '更换封面' : '上传封面' }}
                  </el-button>
                </el-upload>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="简介">
              <el-input v-model="form.description" type="textarea" :rows="4" placeholder="可点击「AI 摘要」自动生成" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.cover-row {
  display: flex;
  align-items: center;
  gap: 16px;
}
.cover-preview {
  width: 60px;
  height: 80px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}
.book-cover {
  width: 40px;
  height: 56px;
  border-radius: 4px;
  background: #f0f2f5;
  display: block;
  cursor: pointer;
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
