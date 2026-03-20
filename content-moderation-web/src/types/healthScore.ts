// 创作者健康分实体
export interface CreatorHealthScore {
  id: number
  userId: number
  healthScore: number
  level: number
  totalViolations: number
  lastViolationAt?: string
  createdAt: string
  updatedAt: string
}

// 健康分记录实体
export interface HealthScoreRecord {
  id: number
  userId: number
  scoreChange: number
  currentScore: number
  violationType: string
  operatorName: string
  content: string
  callId?: string
  idempotencyKey: string
  createdAt: string
}

// 健康分等级
export const HEALTH_SCORE_LEVELS = [
  { min: 90, max: 100, label: '优秀', color: 'green' },
  { min: 70, max: 89, label: '良好', color: 'blue' },
  { min: 60, max: 69, label: '中等', color: 'orange' },
  { min: 0, max: 59, label: '警告', color: 'red' }
]

// 获取健康分等级
export function getHealthLevel(score: number) {
  return HEALTH_SCORE_LEVELS.find(
    level => score >= level.min && score <= level.max
  ) || HEALTH_SCORE_LEVELS[3]
}
