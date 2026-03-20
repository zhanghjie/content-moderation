import request from '@/utils/request'
import type { CreatorHealthScore, HealthScoreRecord } from '@/types/healthScore'

/**
 * 健康分 API
 */
export const healthScoreApi = {
  /**
   * 获取创作者健康分
   */
  getScore: (userId: number) => {
    return request.get<CreatorHealthScore>(`/v1/health-score/${userId}`)
  },

  /**
   * 获取创作者列表（需要后端补充分页接口）
   */
  getList: (params?: {
    userId?: number
    minScore?: number
    maxScore?: number
    level?: number
    current?: number
    pageSize?: number
  }) => {
    return request.get<CreatorHealthScore[]>('/v1/health-scores', { params })
  },

  /**
   * 获取扣分记录列表
   */
  getRecords: (params?: {
    userId?: number
    violationType?: string
    current?: number
    pageSize?: number
  }) => {
    return request.get<HealthScoreRecord[]>('/v1/health-score/records', { params })
  },

  /**
   * 获取统计数据
   */
  getStatistics: () => {
    return request.get('/v1/health-score/statistics')
  }
}
