import { get, post, put, del } from '@/utils/request'

/** 图书 */
export interface Book {
  id: number
  isbn: string
  title: string
  author: string
  publisher: string | null
  categoryId: number
  categoryName?: string
  cover: string | null
  description: string | null
  tags: string | null
  totalStock: number
  availableStock: number
  publishDate: string | null
}

export interface Category {
  id: number
  name: string
  bookCount?: number
}

export interface PageResult<T> {
  total: number
  list: T[]
}

export interface BookQuery {
  keyword?: string
  categoryId?: number
  available?: boolean
  page?: number
  size?: number
}

export function getBooks(params: BookQuery) {
  return get<PageResult<Book>>('/books', params)
}

export function getBookDetail(id: number | string) {
  return get<Book>(`/books/${id}`)
}

/** 热门图书（按借阅量） */
export function getHotBooks(limit = 8) {
  return get<Book[]>('/books/hot', { limit })
}

/** 同分类推荐（详情页） */
export function getSimilarBooks(id: number | string, limit = 6) {
  return get<Book[]>(`/books/${id}/similar`, { limit })
}

export function createBook(data: Partial<Book>) {
  return post<void>('/books', data)
}

export function updateBook(id: number, data: Partial<Book>) {
  return put<void>(`/books/${id}`, data)
}

export function deleteBook(id: number) {
  return del<void>(`/books/${id}`)
}

/** AI 生成图书简介与标签（管理员） */
export function generateSummary(id: number, points?: string) {
  return post<{ description: string; tags: string }>(`/books/${id}/summary`, { points })
}

export function getCategories() {
  return get<Category[]>('/categories')
}

export function createCategory(name: string) {
  return post<void>('/categories', { name })
}

export function updateCategory(id: number, name: string) {
  return put<void>(`/categories/${id}`, { name })
}

export function deleteCategory(id: number) {
  return del<void>(`/categories/${id}`)
}
