/**
 * 用户画像类型定义
 * 
 * 核心理念：
 * 1. 所有维度、标签由AI动态生成
 * 2. 不同用户类型有不同的画像结构
 * 3. 数据来源是视频分析等AI分析结果
 */

// 用户类型
export type UserType = 'STREAMER' | 'USER'  // 主播 | 普通用户

// 风险等级
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'

// 风险趋势
export type RiskTrend = 'IMPROVING' | 'STABLE' | 'WORSENING'

// ============ 动态维度（AI生成）============

// 画像维度（动态）
export interface ProfileDimension {
  code: string           // 维度代码（AI生成）
  name: string           // 维度名称
  score: number          // 分数 0-100
  weight?: number        // 权重
  description?: string   // 维度说明
  source: string         // 数据来源（VIDEO_ANALYSIS, etc.）
}

// 用户标签（AI生成）
export interface UserTag {
  code: string           // 标签代码
  name: string           // 标签名称
  category: string       // 标签类别（AI生成，如：行为特征、内容风格、互动方式...）
  confidence: number     // 置信度 0-1
  evidence?: string      // 标签依据
  source: string         // 来源
  createdAt: string      // 生成时间
}

// ============ 用户画像 ============

// 用户画像列表项
export interface ProfileListItem {
  userId: number
  nickname?: string
  avatar?: string
  userType: UserType     // 用户类型
  
  // 风险评估
  riskLevel: RiskLevel
  trustScore: number
  riskTrend: RiskTrend
  
  // AI生成的标签
  tags: UserTag[]
  
  // 统计
  totalViolations: number
  totalCalls: number
  lastActiveAt: string
  createdAt: string
}

// 用户画像详情
export interface ProfileDetail {
  userId: number
  nickname?: string
  avatar?: string
  userType: UserType
  
  // 基础信息
  basicInfo: {
    registerAt: string
    firstCallAt: string
    lastCallAt: string
    totalCalls: number
    totalCallDuration: number
  }
  
  // 风险画像
  riskProfile: {
    riskLevel: RiskLevel
    trustScore: number
    riskTrend: RiskTrend
    scoreHistory: { date: string; score: number }[]
  }
  
  // 动态维度（AI生成，用于雷达图）
  dimensions: ProfileDimension[]
  
  // AI生成的标签
  tags: UserTag[]
  
  // 违规统计
  violationStats: {
    total: number
    highConfidence: number
    avgConfidence: number
    recentTrend: 'INCREASING' | 'STABLE' | 'DECREASING'
  }
  
  // 行为统计
  behaviorStats: {
    avgCallDuration: number
    violationRate: number
    peakHours: number[]
  }
  
  // 原始分析结果（可追溯）
  analysisResults: AnalysisResult[]
}

// 分析结果（原始数据）
export interface AnalysisResult {
  resultId: string
  type: 'VIDEO_ANALYSIS' | 'MESSAGE_ANALYSIS' | 'BEHAVIOR_ANALYSIS'
  callId?: string
  createdAt: string
  
  // AI输出的原始维度
  dimensions: ProfileDimension[]
  
  // AI输出的标签
  tags: UserTag[]
  
  // 置信度
  confidence: number
}

// ============ 违规事件（独立模块）============

// 违规类型
export type ViolationType =
  | 'CALL_IN_BED'
  | 'WATCH_TV_OR_PLAY_PHONE'
  | 'ENVIRONMENT_MESSY'
  | 'NOISY'
  | 'SEXUAL_ACTION'
  | 'PUBLIC_PLACE'
  | 'OTHER_PLATFORM_OR_OFFLINE_JOB'
  | 'MULTI_PERSON_CONTEXT'
  | 'SILENT_ALL_TIME'
  | 'NO_ONE_ON_CAMERA'
  | 'SLEEPING'
  | 'BLACK_SCREEN'
  | 'PLAY_RECORDING'

// 违规类型映射
export const violationTypeMap: Record<ViolationType, string> = {
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

// 违规事件
export interface ViolationEvent {
  eventId: string
  userId: number
  callId: string
  videoUrl: string
  
  violationType: ViolationType
  violationName: string
  confidence: number
  evidence: string
  
  startSec: number
  endSec: number
  videoDuration: number
  
  status: 'PENDING' | 'CONFIRMED' | 'DISMISSED'
  reviewedAt?: string
  reviewedBy?: string
  
  createdAt: string
}

// ============ API 类型 ============

export interface ProfileListResponse {
  total: number
  page: number
  pageSize: number
  list: ProfileListItem[]
}

export interface ProfileQueryParams {
  userId?: number
  userType?: UserType
  riskLevel?: RiskLevel
  minScore?: number
  maxScore?: number
  page?: number
  pageSize?: number
}