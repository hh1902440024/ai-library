<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名或手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const data = await login(form)
    store.setLogin(data)
    ElMessage.success(`欢迎回来，${data.user.username}`)
    const redirect = (route.query.redirect as string) || (data.user.role === 1 ? '/admin/dashboard' : '/')
    router.push(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card card-shadow">
      <div class="header">
        <el-icon :size="36" color="#409eff"><Reading /></el-icon>
        <h2>AI 智能书屋</h2>
        <p class="text-muted">登录后开启智能阅读之旅</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名 / 手机号" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>

      <div class="footer text-muted">
        还没有账号？
        <router-link to="/register" class="link">立即注册</router-link>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts">
import { User, Lock } from '@element-plus/icons-vue'
export default { components: { User, Lock } }
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e0f0ff 0%, #f5f7fa 100%);
}
.login-card {
  width: 400px;
  padding: 12px 8px;
}
.header {
  text-align: center;
  margin-bottom: 20px;
}
.header h2 {
  margin: 10px 0 4px;
}
.login-btn {
  width: 100%;
}
.footer {
  text-align: center;
}
.link {
  color: #409eff;
  text-decoration: none;
}
</style>
