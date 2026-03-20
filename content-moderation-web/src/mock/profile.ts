/**
 * 用户画像 Mock 数据
 * 
 * 模拟 AI 动态生成的维度和标签
 */

import type {
  ProfileListItem,
  ProfileDetail,
  ProfileDimension,
  UserTag,
  ViolationEvent,
  RiskLevel,
  RiskTrend,
  UserType,
  AnalysisResult
} from '@/types/profile'
import { violationTypeMap } from '@/types/profile'

// ============ AI 动态生成的维度模板 ============

// 主播维度模板
const streamerDimensionTemplates: { name: string; description: string }[] = [
  { name: '专业度', description: '直播专业程度' },
  { name: '互动能力', description: '与观众互动质量' },
  { name: '内容质量', description: '直播内容质量' },
  { name: '环境评分', description: '直播环境质量' },
  { name: '合规性', description: '平台规则遵守程度' },
  { name: '稳定性', description: '直播稳定性' },
  { name: '亲和力', description: '主播亲和力' },
  { name: '创新性', description: '内容创新程度' }
]

// 普通用户维度模板
const userDimensionTemplates: { name: string; description: string }[] = [
  { name: '活跃度', description: '平台活跃程度' },
  { name: '互动质量', description: '互动行为质量' },
  { name: '合规性', description: '规则遵守程度' },
  { name: '真实性', description: '账号真实性' },
  { name: '社交能力', description: '社交互动能力' },
  { name: '忠诚度', description: '平台忠诚度' }
]

// AI 动态生成的标签模板
const tagTemplates: { name: string; category: string }[] = [
  // 行为特征
  { name: '活跃用户', category: '行为特征' },
  { name: '沉默用户', category: '行为特征' },
  { name: '夜间活跃', category: '行为特征' },
  { name: '高频互动', category: '行为特征' },
  { name: '低频登录', category: '行为特征' },
  
  // 内容风格
  { name: '幽默风趣', category: '内容风格' },
  { name: '专业严谨', category: '内容风格' },
  { name: '亲和力强', category: '内容风格' },
  { name: '表达清晰', category: '内容风格' },
  
  // 互动方式
  { name: '主动互动', category: '互动方式' },
  { name: '被动响应', category: '互动方式' },
  { name: '情感丰富', category: '互动方式' },
  
  // 风险特征
  { name: '高风险', category: '风险特征' },
  { name: '中风险', category: '风险特征' },
  { name: '低风险', category: '风险特征' },
  { name: '合规良好', category: '风险特征' },
  
  // 专业能力（主播）
  { name: '专业主播', category: '专业能力' },
  { name: '新手主播', category: '专业能力' },
  { name: '内容丰富', category: '专业能力' }
]

// ============ 工具函数 ============

// 随机选择元素
function randomSelect<T>(arr: T[], min: number, max: number): T[] {
  const count = Math.floor(Math.random() * (max - min + 1)) + min
  const shuffled = [...arr].sort(() => Math.random() - 0.5)
  return shuffled.slice(0, count)
}

// 随机数
function randomInt(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min
}

// 生成动态维度
function generateDimensions(userType: UserType): ProfileDimension[] {
  const templates = userType === 'STREAMER' ? streamerDimensionTemplates : userDimensionTemplates
  const selected = randomSelect(templates, 4, 6)
  
  return selected.map((t, index) => ({
    code: `dim_${index}`,
    name: t.name,
    score: randomInt(50, 100),
    weight: Math.random() * 0.5 + 0.5,
    description: t.description,
    source: 'VIDEO_ANALYSIS'
  }))
}

// 生成动态标签
function generateTags(violationCount: number): UserTag[] {
  const count = randomInt(3, 6)
  const selected = randomSelect(tagTemplates, count, count)
  
  return selected.map((t, index) => ({
    code: `tag_${index}`,
    name: t.name,
    category: t.category,
    confidence: 0.6 + Math.random() * 0.38,
    evidence: `基于 ${randomInt(1, 10)} 次分析得出`,
    source: 'AI_ANALYSIS',
    createdAt: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString()
  }))
}

// 计算风险等级
function calculateRiskLevel(violations: number): RiskLevel {
  if (violations > 5) return 'HIGH'
  if (violations > 2) return 'MEDIUM'
  return 'LOW'
}

// 计算信任分
function calculateTrustScore(violations: number): number {
  const base = randomInt(70, 95)
  const penalty = violations * 8
  return Math.max(0, Math.min(100, base - penalty))
}

// 生成趋势
function generateTrend(): RiskTrend {
  const trends: RiskTrend[] = ['IMPROVING', 'STABLE', 'WORSENING']
  return trends[randomInt(0, 2)]
}

// ============ 数据存储 ============

// 预生成的画像列表
let mockProfileList: ProfileListItem[] = []

// 初始化数据
function initMockData() {
  if (mockProfileList.length > 0) return
  
  const profiles: ProfileListItem[] = []
  
  // 生成 100 个用户
  for (let i = 1; i <= 100; i++) {
    const userType: UserType = Math.random() > 0.3 ? 'STREAMER' : 'USER'
    const violationCount = Math.floor(Math.random() * 10)
    const tags = generateTags(violationCount)
    const totalCalls = randomInt(10, 200)
    
    profiles.push({
      userId: 10000 + i,
      nickname: `${userType === 'STREAMER' ? '主播' : '用户'}${i.toString().padStart(4, '0')}`,
      userType,
      riskLevel: calculateRiskLevel(violationCount),
      trustScore: calculateTrustScore(violationCount),
      riskTrend: generateTrend(),
      tags,
      totalViolations: violationCount,
      totalCalls,
      lastActiveAt: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
      createdAt: new Date(Date.now() - Math.random() * 90 * 24 * 60 * 60 * 1000).toISOString()
    })
  }
  
  mockProfileList = profiles.sort((a, b) => b.trustScore - a.trustScore)
}

// ============ API 函数 ============

// 获取画像列表
export function getProfileList(params?: {
  userId?: number
  userType?: UserType
  riskLevel?: RiskLevel
  minScore?: number
  maxScore?: number
  page?: number
  pageSize?: number
}): { total: number; page: number; pageSize: number; list: ProfileListItem[] } {
  initMockData()
  
  const { userType, riskLevel, minScore, maxScore, page = 1, pageSize = 10 } = params || {}
  
  let filtered = [...mockProfileList]
  
  if (userType) {
    filtered = filtered.filter(p => p.userType === userType)
  }
  if (riskLevel) {
    filtered = filtered.filter(p => p.riskLevel === riskLevel)
  }
  if (minScore !== undefined && minScore !== null) {
    filtered = filtered.filter(p => p.trustScore >= minScore)
  }
  if (maxScore !== undefined && maxScore !== null) {
    filtered = filtered.filter(p => p.trustScore <= maxScore)
  }
  
  const start = (page - 1) * pageSize
  const end = start + pageSize
  
  return {
    total: filtered.length,
    page,
    pageSize,
    list: filtered.slice(start, end)
  }
}

// 获取画像详情
export function getProfileDetail(userId: number): ProfileDetail | null {
  initMockData()
  
  const profile = mockProfileList.find(p => p.userId === userId)
  if (!profile) return null
  
  const dimensions = generateDimensions(profile.userType)
  const totalCallDuration = randomInt(1000, 50000)
  
  // 生成信任分历史
  const scoreHistory = []
  for (let i = 30; i >= 0; i--) {
    const date = new Date(Date.now() - i * 24 * 60 * 60 * 1000)
    scoreHistory.push({
      date: date.toISOString().split('T')[0],
      score: Math.max(0, Math.min(100, profile.trustScore + randomInt(-10, 10)))
    })
  }
  
  // 生成分析结果
  const analysisResults: AnalysisResult[] = [
    {
      resultId: `ar_${userId}_1`,
      type: 'VIDEO_ANALYSIS',
      callId: `call_${userId}_001`,
      createdAt: new Date(Date.now() - randomInt(1, 7) * 24 * 60 * 60 * 1000).toISOString(),
      dimensions: dimensions.slice(0, 3),
      tags: profile.tags.slice(0, 2),
      confidence: 0.85
    },
    {
      resultId: `ar_${userId}_2`,
      type: 'BEHAVIOR_ANALYSIS',
      createdAt: new Date(Date.now() - randomInt(1, 3) * 24 * 60 * 60 * 1000).toISOString(),
      dimensions: dimensions.slice(2, 5),
      tags: profile.tags.slice(2, 4),
      confidence: 0.78
    }
  ]
  
  return {
    userId,
    nickname: profile.nickname,
    userType: profile.userType,
    
    basicInfo: {
      registerAt: profile.createdAt,
      firstCallAt: profile.createdAt,
      lastCallAt: profile.lastActiveAt,
      totalCalls: profile.totalCalls,
      totalCallDuration
    },
    
    riskProfile: {
      riskLevel: profile.riskLevel,
      trustScore: profile.trustScore,
      riskTrend: profile.riskTrend,
      scoreHistory
    },
    
    dimensions,
    tags: profile.tags,
    
    violationStats: {
      total: profile.totalViolations,
      highConfidence: Math.floor(profile.totalViolations * 0.6),
      avgConfidence: 0.75 + Math.random() * 0.2,
      recentTrend: ['INCREASING', 'STABLE', 'DECREASING'][randomInt(0, 2)] as any
    },
    
    behaviorStats: {
      avgCallDuration: Math.floor(totalCallDuration / profile.totalCalls),
      violationRate: profile.totalCalls > 0 ? profile.totalViolations / profile.totalCalls : 0,
      peakHours: [14, 15, 20, 21, 22]
    },
    
    analysisResults
  }
}

// 获取用户违规记录
export function getUserViolations(userId: number, params?: {
  violationType?: string
  minConfidence?: number
  status?: string
  page?: number
  pageSize?: number
}): { total: number; list: ViolationEvent[] } {
  initMockData()
  
  const profile = mockProfileList.find(p => p.userId === userId)
  if (!profile) return { total: 0, list: [] }
  
  const types = Object.keys(violationTypeMap) as (keyof typeof violationTypeMap)[]
  const violations: ViolationEvent[] = []
  
  for (let i = 0; i < profile.totalViolations; i++) {
    const type = types[randomInt(0, types.length - 1)]
    violations.push({
      eventId: `evt_${userId}_${i}`,
      userId,
      callId: `call_${userId}_${i}`,
      videoUrl: `https://example.com/videos/${userId}_${i}.mp4`,
      violationType: type,
      violationName: violationTypeMap[type],
      confidence: 0.6 + Math.random() * 0.38,
      evidence: `检测到${violationTypeMap[type]}行为`,
      startSec: randomInt(0, 60),
      endSec: randomInt(60, 180),
      videoDuration: randomInt(60, 300),
      status: Math.random() > 0.2 ? 'CONFIRMED' : 'PENDING',
      createdAt: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString()
    })
  }
  
  return {
    total: violations.length,
    list: violations
  }
}