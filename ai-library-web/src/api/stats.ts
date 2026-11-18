import { get } from '@/utils/request'

/** 数据统计接口（管理员） */

export interface TrendPoint {
  date: string
  count: number
}

export interface CategoryStat {
  name: string
  value: number
}

export interface HotBook {
  title: string
  borrowCount: number
}

/** 看板聚合数据 */
export interface Dashboard {
  userCount: number
  bookCount: number
  borrowingCount: number
  overdueCount: number
  trend: TrendPoint[]
  categoryStats: CategoryStat[]
  hotBooks: HotBook[]
}

export const getDashboard = () => get<Dashboard>('/stats/dashboard')
