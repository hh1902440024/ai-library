<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const store = useUserStore()

const menu = [
  { path: '/admin/dashboard', title: '数据看板', icon: 'DataAnalysis' },
  { path: '/admin/books', title: '图书管理', icon: 'Reading' },
  { path: '/admin/categories', title: '分类管理', icon: 'Collection' },
  { path: '/admin/borrows', title: '借阅管理', icon: 'Notebook' },
  { path: '/admin/users', title: '用户管理', icon: 'User' },
]
</script>

<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="aside">
      <div class="logo" @click="router.push('/')">
        <el-icon :size="22" color="#409eff"><Reading /></el-icon>
        <span>AI 智能书屋 · 管理后台</span>
      </div>
      <el-menu router :default-active="router.currentRoute.value.path" class="menu">
        <el-menu-item v-for="item in menu" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
      <div class="back-home">
        <el-button text type="primary" @click="router.push('/')">
          <el-icon><Back /></el-icon>
          返回前台
        </el-button>
      </div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span class="page-title">{{ router.currentRoute.value.meta.title }}</span>
        <div class="admin-info">
          <el-tag size="small" type="warning">管理员</el-tag>
          <span>{{ store.user?.username }}</span>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
}
.aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 60px;
  padding: 0 20px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  border-bottom: 1px solid #e4e7ed;
  white-space: nowrap;
}
.menu {
  border-right: none;
  flex: 1;
}
.back-home {
  padding: 12px 16px;
  border-top: 1px solid #e4e7ed;
}
.header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
}
.admin-info {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}
.main {
  background: #f5f7fa;
}
</style>
