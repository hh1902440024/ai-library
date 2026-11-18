<script setup lang="ts">
import type { Book } from '@/api/book'
import { resolveCover } from '@/utils/cover'

const props = defineProps<{ book: Book }>()
</script>

<template>
  <el-card shadow="hover" class="book-card">
    <div class="cover">
      <el-image :src="resolveCover(props.book)" fit="cover" class="cover-img">
        <template #error>
          <div class="cover-fallback">
            <el-icon :size="32"><Reading /></el-icon>
            <span>{{ props.book.title }}</span>
          </div>
        </template>
      </el-image>
    </div>
    <div class="info">
      <div class="title" :title="props.book.title">{{ props.book.title }}</div>
      <div class="author text-muted">{{ props.book.author }}</div>
      <div class="tags" v-if="props.book.tags">
        <el-tag v-for="tag in props.book.tags.split(',').slice(0, 2)" :key="tag" size="small" type="info">{{ tag.trim() }}</el-tag>
      </div>
      <div class="stock">
        <el-tag size="small" :type="props.book.availableStock > 0 ? 'success' : 'danger'">
          {{ props.book.availableStock > 0 ? `可借 ${props.book.availableStock}` : '暂不可借' }}
        </el-tag>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.book-card {
  cursor: pointer;
  width: 100%;
}
/* 封面按 5:7 固定比例展示，与内置封面图比例一致，避免裁切掉书名 */
.cover {
  aspect-ratio: 5 / 7;
  border-radius: 6px;
  overflow: hidden;
  background: #f0f2f5;
}
.cover-img {
  width: 100%;
  height: 100%;
  display: block;
}
.cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #909399;
  padding: 0 12px;
  text-align: center;
  font-size: 13px;
}
.info {
  padding-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.title {
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.author {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tags {
  display: flex;
  gap: 4px;
}
.stock {
  margin-top: 2px;
}
</style>
