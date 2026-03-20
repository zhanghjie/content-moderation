import request from '@/utils/request'

/**
 * 统计 API
 */
export const statisticsApi = {
  /**
   * 获取仪表盘数据
   */
  getDashboard: () => {
    return request.get('/v1/statistics/dashboard')
  },

  /**
   * 获取违规趋势
   */
  getViolationTrend: (days: number = 7) => {
    return request.get('/v1/statistics/violation-trend', { params: { days } })
  },

  /**
   * 获取违规类型分布
   */
  getViolationTypeDistribution: () => {
    return request.get('/v1/statistics/violation-types')
  },

  /**
   * 获取健康分分布
   */
  getHealthScoreDistribution: () => {
    return request.get('/v1/statistics/health-score')
  }
}
