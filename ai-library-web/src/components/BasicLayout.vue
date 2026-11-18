<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const store = useUserStore()

async function handleLogout() {
  await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
  store.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="basic-layout">
    <el-header class="header">
      <div class="header-inner">
        <div class="logo" @click="router.push('/')">
          <el-icon :size="24" color="#409eff"><Reading /></el-icon>
          <span class="title">AI 智能书屋</span>
        </div>

        <el-menu mode="horizontal" router :default-active="router.currentRoute.value.path" class="nav-menu" :ellipsis="false">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/books">图书列表</el-menu-item>
          <el-menu-item index="/ai/chat">AI 助手</el-menu-item>
          <el-menu-item index="/borrow/my">我的借阅</el-menu-item>
        </el-menu>

        <div class="header-right">
          <template v-if="store.isLoggedIn">
            <el-dropdown>
              <span class="user-info">
                <el-avatar :size="30" :src="store.user?.avatar || undefined">
                  {{ store.user?.username?.charAt(0)?.toUpperCase() }}
                </el-avatar>
                <span class="username">{{ store.user?.username }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item v-if="store.isAdmin" @click="router.push('/admin/dashboard')">管理后台</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" plain size="small" @click="router.push('/login')">登录</el-button>
            <el-button type="primary" size="small" @click="router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>

    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped>
.basic-layout {
  min-height: 100vh;
}
.header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0;
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 20px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;
}
.logo .title {
  font-size: 18px;
  font-weight: 600;
}
.nav-menu {
  flex: 1;
  border-bottom: none;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.username {
  font-size: 14px;
}
.main {
  padding: 0;
}
</style>
