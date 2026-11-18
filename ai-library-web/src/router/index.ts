import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { title: '登录' } },
    { path: '/register', name: 'register', component: () => import('@/views/RegisterView.vue'), meta: { title: '注册' } },
    {
      path: '/',
      component: () => import('@/components/BasicLayout.vue'),
      children: [
        { path: '', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { title: '首页' } },
        { path: 'books', name: 'books', component: () => import('@/views/BooksView.vue'), meta: { title: '图书列表' } },
        { path: 'books/:id', name: 'bookDetail', component: () => import('@/views/BookDetailView.vue'), meta: { title: '图书详情' } },
        { path: 'ai/chat', name: 'aiChat', component: () => import('@/views/AiChatView.vue'), meta: { title: 'AI 助手', auth: true } },
        { path: 'borrow/my', name: 'myBorrow', component: () => import('@/views/MyBorrowView.vue'), meta: { title: '我的借阅', auth: true } },
        { path: 'profile', name: 'profile', component: () => import('@/views/ProfileView.vue'), meta: { title: '个人中心', auth: true } },
      ],
    },
    {
      path: '/admin',
      component: () => import('@/components/AdminLayout.vue'),
      meta: { admin: true },
      children: [
        { path: '', redirect: '/admin/dashboard' },
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/admin/DashboardView.vue'), meta: { title: '数据看板' } },
        { path: 'books', name: 'adminBooks', component: () => import('@/views/admin/BooksView.vue'), meta: { title: '图书管理' } },
        { path: 'categories', name: 'adminCategories', component: () => import('@/views/admin/CategoriesView.vue'), meta: { title: '分类管理' } },
        { path: 'borrows', name: 'adminBorrows', component: () => import('@/views/admin/BorrowsView.vue'), meta: { title: '借阅管理' } },
        { path: 'users', name: 'adminUsers', component: () => import('@/views/admin/UsersView.vue'), meta: { title: '用户管理' } },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

// 全局导航守卫
router.beforeEach((to) => {
  const store = useUserStore()

  // 未登录访问需登录页面 → 跳登录页
  if (to.meta.auth && !store.isLoggedIn) {
    ElMessage.warning('请先登录')
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 非管理员访问 /admin → 跳首页
  if (to.meta.admin && !store.isAdmin) {
    ElMessage.error('无权限访问管理后台')
    return { path: '/' }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - AI 智能书屋` : 'AI 智能书屋'
})

export default router
