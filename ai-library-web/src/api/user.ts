import { get, put } from '@/utils/request'
import type { PageResult } from '@/api/book'

/** 用户信息（管理员用户管理） */
export interface AdminUser {
  id: number
  username: string
  phone: string
  role: number
  email: string | null
  status: number
  createTime: string
}

export function getUsers(params: { keyword?: string; page?: number; size?: number }) {
  return get<PageResult<AdminUser>>('/users', params)
}

export function updateUserStatus(id: number, status: number) {
  return put<void>(`/users/${id}/status`, { status })
}

export function updateUserRole(id: number, role: number) {
  return put<void>(`/users/${id}/role`, { role })
}
