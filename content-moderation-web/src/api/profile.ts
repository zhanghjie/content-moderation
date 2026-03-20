import request from '@/utils/request'
import type {
  ProfileListItem,
  ProfileDetail,
  ViolationEvent,
  ProfileQueryParams
} from '@/types/profile'

// 是否使用 Mock 数据
const USE_MOCK = true

// Mock 数据导入
import {
  getProfileList as mockGetProfileList,
  getProfileDetail as mockGetProfileDetail,
  getUserViolations as mockGetUserViolations
} from '@/mock'

// 列表响应类型
interface ListResponse<T> {
  total: number
  page: number
  pageSize: number
  list: T[]
}

/**
 * 用户画像 API
 */
export const profileApi = {
  /**
   * 获取用户画像列表
   */
  getList: async (params?: ProfileQueryParams): Promise<ListResponse<ProfileListItem>> => {
    if (USE_MOCK) {
      return mockGetProfileList(params)
    }
    const res = await request.get<ListResponse<ProfileListItem>>('/v1/profile/list', { params })
    return res.data
  },

  /**
   * 获取用户画像详情
   */
  getDetail: async (userId: number): Promise<ProfileDetail | null> => {
    if (USE_MOCK) {
      return mockGetProfileDetail(userId)
    }
    const res = await request.get<ProfileDetail>(`/v1/profile/${userId}`)
    return res.data
  },

  /**
   * 获取用户违规记录
   */
  getViolations: async (userId: number, params?: {
    violationType?: string
    minConfidence?: number
    status?: string
    page?: number
    pageSize?: number
  }): Promise<{ total: number; list: ViolationEvent[] }> => {
    if (USE_MOCK) {
      return mockGetUserViolations(userId, params)
    }
    const res = await request.get<{ total: number; list: ViolationEvent[] }>(`/v1/profile/${userId}/violations`, { params })
    return res.data
  }
}