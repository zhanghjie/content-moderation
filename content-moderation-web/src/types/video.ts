// 视频分析任务实体
export interface VideoAnalysisTask {
  taskId: string
  callId: string
  contentId: string
  videoUrl: string
  coverUrl?: string
  analysisType?: 'STANDARD' | 'HOST_VIOLATION'
  userId?: number
  status: 'DRAFT' | 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  moderationResult?: 'NOT_HIT' | 'HIT' | 'SUSPECTED'
  overallConfidence?: number
  promptModules?: string
  completedAt?: string
  errorMessage?: string
  resultJson?: string
  violations?: Violation[]
  summary?: VideoSummary
  analysisDuration?: string
  createdAt?: string
  policyId?: string
  policyName?: string
}

// 违规信息
export interface Violation {
  type: string
  detected: boolean
  confidence: number
  evidence: string
  startSec: number
  endSec: number
}

// 视频摘要
export interface VideoSummary {
  totalViolations: number
  highConfidenceCount: number
  primaryViolation: string
  videoDurationSec?: number
  overallConfidence?: number
}

// 发起视频分析请求
export interface VideoAnalyzeReq {
  callId: string
  contentId: string
  videoUrl: string
  coverUrl?: string
  analysisType?: 'STANDARD' | 'HOST_VIOLATION'
  userId?: number
  promptModules?: string[]
}

// 任务状态选项
export const TASK_STATUS_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待处理', value: 'PENDING' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '失败', value: 'FAILED' }
]

// 违规命中结果选项
export const MODERATION_RESULT_OPTIONS = [
  { label: '未命中违规', value: 'NOT_HIT', color: 'green' },
  { label: '命中违规', value: 'HIT', color: 'red' },
  { label: '疑似命中', value: 'SUSPECTED', color: 'orange' }
]
