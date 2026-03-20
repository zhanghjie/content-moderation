/**
 * Mock 数据入口
 */

export {
  getProfileList,
  getProfileDetail,
  getUserViolations
} from './profile'

export {
  getDashboardStats,
  getRiskDistribution,
  getViolationDistribution,
  getRiskTrend,
  getHighRiskUsers,
  getRecentViolations
} from './dashboard'

export {
  getTaskList,
  getTaskDetail,
  reAnalyze,
  createTask,
  simulateTaskProcessing,
  getPromptModules
} from './video'

// Mock 延迟模拟
export function mockDelay(ms: number = 300): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

// Mock API 响应包装
export function mockResponse<T>(data: T, delay: number = 300): Promise<{ code: number; message: string; data: T }> {
  return mockDelay(delay).then(() => ({
    code: 200,
    message: 'success',
    data
  }))
}
