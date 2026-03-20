/**
 * Dashboard 类型定义
 */

import type { RiskLevel } from './profile'

// 统计数据
export interface DashboardStatistics {
  totalUsers: number
  highRiskUsers: number
  mediumRiskUsers: number
  lowRiskUsers: number
  trends: {
    totalUsers: string
    highRiskUsers: string
    mediumRiskUsers: string
    lowRiskUsers: string
  }
}

// 风险分布
export interface RiskDistribution {
  high: number
  medium: number
  low: number
}

// 违规类型分布项
export interface ViolationDistributionItem {
  type: string
  name: string
  count: number
}

// 风险趋势
export interface RiskTrend {
  dates: string[]
  highRisk: number[]
  mediumRisk: number[]
  lowRisk: number[]
}

// 高风险用户项
export interface HighRiskUserItem {
  userId: number
  trustScore: number
  totalViolations: number
  lastActiveAt: string
}

// 最新违规事件项
export interface RecentViolationItem {
  eventId: string
  userId: number
  violationType: string
  violationName: string
  confidence: number
  createdAt: string
}