/**
 * Dashboard Mock 数据
 */

import { getProfileList } from './profile'
import type { ViolationType } from '@/types/profile'

// 违规类型映射
const violationTypeNames: Record<ViolationType, string> = {
  CALL_IN_BED: '躺在床上通话',
  WATCH_TV_OR_PLAY_PHONE: '看电视/玩手机',
  ENVIRONMENT_MESSY: '环境杂乱',
  NOISY: '声音嘈杂',
  SEXUAL_ACTION: '色情动作',
  PUBLIC_PLACE: '公共场合',
  OTHER_PLATFORM_OR_OFFLINE_JOB: '其他平台/实体工作',
  MULTI_PERSON_CONTEXT: '多人出镜',
  SILENT_ALL_TIME: '全程不说话',
  NO_ONE_ON_CAMERA: '无人出镜',
  SLEEPING: '睡觉',
  BLACK_SCREEN: '黑屏',
  PLAY_RECORDING: '播放录屏'
}

// 获取统计数据
export function getDashboardStats() {
  const allProfiles = getProfileList({ page: 1, pageSize: 1000 })
  const list = allProfiles.list
  
  const totalUsers = allProfiles.total
  const highRiskUsers = list.filter(p => p.riskLevel === 'HIGH').length
  const mediumRiskUsers = list.filter(p => p.riskLevel === 'MEDIUM').length
  const lowRiskUsers = list.filter(p => p.riskLevel === 'LOW').length

  return {
    totalUsers,
    highRiskUsers,
    mediumRiskUsers,
    lowRiskUsers,
    trends: {
      totalUsers: Math.floor(Math.random() * 10 - 3),
      highRiskUsers: Math.floor(Math.random() * 6 - 3),
      mediumRiskUsers: Math.floor(Math.random() * 4 - 2),
      lowRiskUsers: Math.floor(Math.random() * 8 - 2)
    }
  }
}

// 获取风险等级分布
export function getRiskDistribution() {
  const allProfiles = getProfileList({ page: 1, pageSize: 1000 })
  const list = allProfiles.list
  
  return {
    high: list.filter(p => p.riskLevel === 'HIGH').length,
    medium: list.filter(p => p.riskLevel === 'MEDIUM').length,
    low: list.filter(p => p.riskLevel === 'LOW').length
  }
}

// 获取违规类型分布
export function getViolationDistribution() {
  const typeCount: Record<string, number> = {}
  const types = Object.keys(violationTypeNames) as ViolationType[]

  // 模拟违规类型分布
  types.forEach(type => {
    typeCount[type] = Math.floor(Math.random() * 50) + 5
  })

  return Object.entries(typeCount)
    .map(([type, count]) => ({
      type,
      name: violationTypeNames[type as ViolationType] || type,
      count
    }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 10)
}

// 获取风险趋势
export function getRiskTrend(days: number = 30) {
  const dates = []
  const highRisk = []
  const mediumRisk = []
  const lowRisk = []

  const stats = getDashboardStats()
  const baseHigh = stats.highRiskUsers
  const baseMedium = stats.mediumRiskUsers
  const baseLow = stats.lowRiskUsers

  for (let i = days - 1; i >= 0; i--) {
    const date = new Date(Date.now() - i * 24 * 60 * 60 * 1000)
    dates.push(date.toISOString().split('T')[0])

    // 添加一些随机波动
    highRisk.push(Math.max(0, baseHigh + Math.floor(Math.random() * 20 - 10)))
    mediumRisk.push(Math.max(0, baseMedium + Math.floor(Math.random() * 50 - 25)))
    lowRisk.push(Math.max(0, baseLow + Math.floor(Math.random() * 100 - 50)))
  }

  return {
    dates,
    highRisk,
    mediumRisk,
    lowRisk
  }
}

// 获取高风险用户列表
export function getHighRiskUsers(limit: number = 10) {
  const allProfiles = getProfileList({ page: 1, pageSize: 1000, riskLevel: 'HIGH' })
  
  return allProfiles.list
    .sort((a, b) => a.trustScore - b.trustScore)
    .slice(0, limit)
    .map(p => ({
      userId: p.userId,
      trustScore: p.trustScore,
      totalViolations: p.totalViolations,
      lastActiveAt: p.lastActiveAt
    }))
}

// 获取最新违规事件
export function getRecentViolations(limit: number = 10) {
  const violations: any[] = []
  const types = Object.keys(violationTypeNames) as ViolationType[]
  const allProfiles = getProfileList({ page: 1, pageSize: 20 })

  allProfiles.list.forEach(profile => {
    if (violations.length < limit) {
      const type = types[Math.floor(Math.random() * types.length)]
      violations.push({
        eventId: `EVT-${profile.userId}-${Date.now()}`,
        userId: profile.userId,
        violationType: type,
        violationName: violationTypeNames[type],
        confidence: 0.8 + Math.random() * 0.15,
        createdAt: new Date(Date.now() - Math.random() * 24 * 60 * 60 * 1000).toISOString()
      })
    }
  })

  return violations.slice(0, limit)
}