import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

/** 后端统一响应格式 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 请求拦截器：自动附带 token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一错误处理
request.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse
    // 文件流等直接放行
    if (response.config.responseType === 'blob') {
      return response
    }
    if (res.code !== 200) {
      if (res.code === 401) {
        // token 失效：清除登录态，跳转登录页
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        ElMessage.error(res.message || '登录已过期，请重新登录')
        router.push('/login')
      } else {
        ElMessage.error(res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return response
  },
  (error) => {
    ElMessage.error(error.message === 'Network Error' ? '网络异常，请检查后端服务' : error.message || '请求失败')
    return Promise.reject(error)
  },
)

/** 泛型请求快捷方法：返回 data 字段 */
export async function get<T>(url: string, params?: object): Promise<T> {
  const res = await request.get<ApiResponse<T>>(url, { params })
  return res.data.data
}

export async function post<T>(url: string, data?: object): Promise<T> {
  const res = await request.post<ApiResponse<T>>(url, data)
  return res.data.data
}

export async function put<T>(url: string, data?: object): Promise<T> {
  const res = await request.put<ApiResponse<T>>(url, data)
  return res.data.data
}

export async function del<T>(url: string): Promise<T> {
  const res = await request.delete<ApiResponse<T>>(url)
  return res.data.data
}

export default request
