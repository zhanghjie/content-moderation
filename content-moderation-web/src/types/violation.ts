// 违规事件实体
export interface ViolationEvent {
  eventId: string
  taskId?: string
  callId: string
  userId?: number
  contentId?: string
  violationType: string
  confidence: number
  evidence: string
  startSec: number
  endSec: number
  promptVersion?: string
  modelVersion?: string
  processed: boolean
  createdAt: string
}

// 13 种违规类型
export const VIOLATION_TYPES = [
  { value: 'ENVIRONMENT_MESSY', label: '环境杂乱', score: -5 },
  { value: 'NOISY', label: '声音嘈杂', score: -5 },
  { value: 'SEXUAL_ACTION', label: '色情动作', score: -10 },
  { value: 'PUBLIC_PLACE', label: '公共场合', score: -10 },
  { value: 'OTHER_PLATFORM_OR_OFFLINE_JOB', label: '其他平台/实体工作', score: -10 },
  { value: 'MULTI_PERSON_CONTEXT', label: '多人出镜', score: -15 },
  { value: 'WATCH_TV_OR_PLAY_PHONE', label: '看电视/玩手机', score: -20 },
  { value: 'CALL_IN_BED', label: '躺在床上通话', score: -20 },
  { value: 'SILENT_ALL_TIME', label: '全程不说话', score: -30 },
  { value: 'NO_ONE_ON_CAMERA', label: '无人出镜', score: -30 },
  { value: 'SLEEPING', label: '睡觉', score: -30 },
  { value: 'BLACK_SCREEN', label: '黑屏', score: -50 },
  { value: 'PLAY_RECORDING', label: '播放录屏', score: -60 }
]

// 处理状态选项
export const PROCESSED_OPTIONS = [
  { label: '未处理', value: false, color: 'warning' },
  { label: '已处理', value: true, color: 'success' }
]
