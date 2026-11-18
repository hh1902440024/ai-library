<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import { updatePassword, updateProfile, uploadImage } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const store = useUserStore()

const profileForm = reactive({
  email: store.user?.email || '',
  avatar: store.user?.avatar || '',
})

const avatarUploading = ref(false)

/** 自定义上传：调 /api/upload/image 后回填 URL */
async function handleAvatarUpload(options: UploadRequestOptions) {
  avatarUploading.value = true
  try {
    profileForm.avatar = await uploadImage(options.file)
    ElMessage.success('头像已上传，点击"保存修改"生效')
  } finally {
    avatarUploading.value = false
  }
}

const pwdFormRef = ref<FormInstance>()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

async function saveProfile() {
  const user = await updateProfile(profileForm)
  store.setUser(user)
  ElMessage.success('个人信息已更新')
}

async function savePassword() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  await updatePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
  ElMessage.success('密码修改成功，请重新登录')
  store.logout()
  router.push('/login')
}
</script>

<template>
  <div class="profile page-container">
    <el-row :gutter="16">
      <!-- 个人信息 -->
      <el-col :md="12">
        <el-card shadow="never">
          <template #header>个人信息</template>
          <el-form label-width="70px">
            <el-form-item label="用户名">
              <el-input :model-value="store.user?.username" disabled />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input :model-value="store.user?.phone" disabled />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="头像">
              <div class="avatar-row">
                <el-avatar :size="56" :src="profileForm.avatar || undefined">
                  {{ store.user?.username?.charAt(0)?.toUpperCase() }}
                </el-avatar>
                <el-upload
                  :show-file-list="false"
                  :http-request="handleAvatarUpload"
                  accept="image/png,image/jpeg,image/webp,image/gif"
                >
                  <el-button :loading="avatarUploading">上传头像</el-button>
                </el-upload>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 密码修改 -->
      <el-col :md="12">
        <el-card shadow="never">
          <template #header>修改密码</template>
          <el-form ref="pwdFormRef" :model="pwdForm" label-width="90px">
            <el-form-item label="原密码">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6~20 位" />
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="savePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.avatar-row {
  display: flex;
  align-items: center;
  gap: 16px;
}
</style>
