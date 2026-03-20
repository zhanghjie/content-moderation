import request from '@/utils/request'
import type {
  DashboardStatistics,
  RiskDistribution,
  ViolationDistributionItem,
  RiskTrend,
  HighRiskUserItem,
  RecentViolationItem
} from '@/types/dashboard'

// 是否使用 Mock 数据
const USE_MOCK = true

// Mock 数据导入
import {
  getDashboardStats,
  getRiskDistribution as mockGetRiskDistribution,
  getViolationDistribution as mockGetViolationDistribution,
  getRiskTrend as mockGetRiskTrend,
  getHighRiskUsers as mockGetHighRiskUsers,
  getRecentViolations as mockGetRecentViolations
} from '@/mock'

/**
 * Dashboard API
 */
export const dashboardApi = {
  /**
   * 获取统计数据
   */
  getStatistics: async (): Promise<DashboardStatistics> => {
    if (USE_MOCK) {
      const stats = getDashboardStats()
      return {
        totalUsers: stats.totalUsers,
        highRiskUsers: stats.highRiskUsers,
        mediumRiskUsers: stats.mediumRiskUsers,
        lowRiskUsers: stats.lowRiskUsers,
        trends: {
          totalUsers: String(stats.trends.totalUsers),
          highRiskUsers: String(stats.trends.highRiskUsers),
          mediumRiskUsers: String(stats.trends.mediumRiskUsers),
          lowRiskUsers: String(stats.trends.lowRiskUsers)
        }
      }
    }
    const res = await request.get<DashboardStatistics>('/v1/dashboard/statistics')
    return res.data
  },

  /**
   * 获取风险等级分布
   */
  getRiskDistribution: async (): Promise<RiskDistribution> => {
    if (USE_MOCK) {
      return mockGetRiskDistribution()
    }
    const res = await request.get<RiskDistribution>('/v1/dashboard/risk-distribution')
    return res.data
  },

  /**
   * 获取违规类型分布
   */
  getViolationDistribution: async (): Promise<ViolationDistributionItem[]> => {
    if (USE_MOCK) {
      return mockGetViolationDistribution()
    }
    const res = await request.get<ViolationDistributionItem[]>('/v1/dashboard/violation-distribution')
    return res.data
  },

  /**
   * 获取风险趋势
   */
  getRiskTrend: async (days: number = 30): Promise<RiskTrend> => {
    if (USE_MOCK) {
      return mockGetRiskTrend(days)
    }
    const res = await request.get<RiskTrend>('/v1/dashboard/risk-trend', { params: { days } })
    return res.data
  },

  /**
   * 获取高风险用户列表
   */
  getHighRiskUsers: async (limit: number = 10): Promise<HighRiskUserItem[]> => {
    if (USE_MOCK) {
      return mockGetHighRiskUsers(limit)
    }
    const res = await request.get<HighRiskUserItem[]>('/v1/dashboard/high-risk-users', { params: { limit } })
    return res.data
  },

  /**
   * 获取最新违规事件
   */
  getRecentViolations: async (limit: number = 10): Promise<RecentViolationItem[]> => {
    if (USE_MOCK) {
      return mockGetRecentViolations(limit)
    }
    const res = await request.get<RecentViolationItem[]>('/v1/dashboard/recent-violations', { params: { limit } })
    return res.data
  }
}