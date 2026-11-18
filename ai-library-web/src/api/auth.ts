import request, { get, post, put, type ApiResponse } from '@/utils/request'

/** 用户信息 */
export interface UserInfo {
  id: number
  username: string
  phone: string
  role: number
  email: string | null
  avatar: string | null
  status: number
  createTime: string
}

export interface LoginResult {
  token: string
  user: UserInfo
}

export function register(data: { username: string; phone: string; password: string; confirmPassword: string }) {
  return post<void>('/auth/register', data)
}

export function login(data: { username: string; password: string }) {
  return post<LoginResult>('/auth/login', data)
}

export function getCurrentUser() {
  return get<UserInfo>('/users/me')
}

export function updateProfile(data: { email?: string; avatar?: string }) {
  return put<UserInfo>('/users/me', data)
}

export function updatePassword(data: { oldPassword: string; newPassword: string }) {
  return put<void>('/users/password', data)
}

/** 上传图片（头像/封面），返回可访问 URL */
export function uploadImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<ApiResponse<string>>('/upload/image', formData).then((res) => res.data.data)
}
