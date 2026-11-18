import { get, post, put } from '@/utils/request'
import request from '@/utils/request'
import type { PageResult } from '@/api/book'

/** 借阅记录 */
export interface BorrowRecord {
  id: number
  userId: number
  username?: string
  bookId: number
  bookTitle?: string
  /** 图书封面地址（为空时前端按 bookIsbn 回退到内置封面） */
  bookCover?: string | null
  /** 图书 ISBN，用于匹配内置封面图 */
  bookIsbn?: string | null
  categoryName?: string
  borrowTime: string
  dueTime: string
  returnTime: string | null
  /** 0 借出 1 已还 2 逾期 */
  status: number
  /** 动态逾期：未标记但已超过应还时间 */
  overdue?: boolean
}

export interface BorrowQuery {
  status?: number
  userId?: number
  keyword?: string
  page?: number
  size?: number
}

export function borrowBook(bookId: number) {
  return post<void>('/borrows', { bookId })
}

export function returnBook(id: number) {
  return put<void>(`/borrows/${id}/return`)
}

export function getMyBorrows(params: BorrowQuery) {
  return get<PageResult<BorrowRecord>>('/borrows/my', params)
}

/** 管理员：全部借阅记录 */
export function getAdminBorrows(params: BorrowQuery) {
  return get<PageResult<BorrowRecord>>('/borrows', params)
}

/** 管理员：立即执行逾期标记 */
export function markOverdue() {
  return post<number>('/borrows/mark-overdue')
}

/** 管理员：导出 Excel（带 token 的 blob 下载） */
export async function exportBorrows(params: BorrowQuery) {
  const res = await request.get('/borrows/export', { params, responseType: 'blob' })
  const url = window.URL.createObjectURL(new Blob([res.data]))
  const link = document.createElement('a')
  link.href = url
  link.download = `借阅记录_${new Date().toISOString().slice(0, 10)}.xlsx`
  link.click()
  window.URL.revokeObjectURL(url)
}
