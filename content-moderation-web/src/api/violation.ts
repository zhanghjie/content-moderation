import request from '@/utils/request'
import type { ViolationEvent } from '@/types/violation'

/**
 * 违规事件 API
 */
export const violationApi = {
  /**
   * 获取事件列表
   */
  getList: (params?: {
    eventId?: string
    callId?: string
    violationType?: string
    processed?: boolean
    current?: number
    pageSize?: number
  }) => {
    return request.get<ViolationEvent[]>('/v1/violations', { params })
  },

  /**
   * 获取事件详情
   */
  getDetail: (eventId: string) => {
    return request.get<ViolationEvent>(`/v1/violations/${eventId}`)
  },

  /**
   * 标记已处理
   */
  markProcessed: (eventId: string) => {
    return request.put(`/v1/violations/${eventId}/processed`)
  },

  /**
   * 批量标记已处理
   */
  batchMarkProcessed: (eventIds: string[]) => {
    return request.put('/v1/violations/batch/processed', { eventIds })
  },

  /**
   * 获取统计数据
   */
  getStatistics: () => {
    return request.get('/v1/violations/statistics')
  }
}
