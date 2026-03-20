/**
 * 视频分析 Mock 数据
 */

import type { VideoAnalysisTask, Violation } from '@/types/video'

// 违规类型映射
export const violationTypeMap: Record<string, string> = {
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

// 生成随机违规信息
function generateViolations(): Violation[] {
  const types = Object.keys(violationTypeMap)
  const count = Math.floor(Math.random() * 4) // 0-3 个违规
  const violations: Violation[] = []
  
  const selectedTypes = types.sort(() => Math.random() - 0.5).slice(0, count)
  
  selectedTypes.forEach(type => {
    const detected = Math.random() > 0.3 // 70% 概率检测到
    violations.push({
      type: violationTypeMap[type],
      detected,
      confidence: detected ? 0.7 + Math.random() * 0.28 : 0.3 + Math.random() * 0.3,
      evidence: detected 
        ? `检测到${violationTypeMap[type]}行为，置信度较高。视频中出现明显的违规特征。`
        : `未检测到明显的${violationTypeMap[type]}行为`,
      startSec: Math.floor(Math.random() * 60),
      endSec: Math.floor(Math.random() * 120) + 60
    })
  })
  
  return violations
}

// 生成随机任务
function generateTask(index: number): VideoAnalysisTask {
  const taskId = `task_${Date.now()}_${index}`
  const callId = `call_${Date.now()}_${index}`
  const contentId = `content_${index.toString().padStart(3, '0')}`
  
  const statuses: Array<'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'> = 
    ['PENDING', 'PROCESSING', 'COMPLETED', 'COMPLETED', 'COMPLETED', 'FAILED']
  const status = statuses[Math.floor(Math.random() * statuses.length)]
  
  let moderationResult: 'NOT_HIT' | 'HIT' | 'SUSPECTED' | undefined
  let violations: Violation[] | undefined
  let summary: VideoAnalysisTask['summary']
  
  if (status === 'COMPLETED') {
    violations = generateViolations()
    const detectedCount = violations.filter(v => v.detected).length
    
    if (detectedCount === 0) {
      moderationResult = 'NOT_HIT'
    } else if (detectedCount >= 2 || violations.some(v => v.detected && v.confidence > 0.9)) {
      moderationResult = 'HIT'
    } else {
      moderationResult = 'SUSPECTED'
    }
    
    const highConfidenceCount = violations.filter(v => v.detected && v.confidence > 0.85).length
    const primaryViolation = violations.find(v => v.detected)?.type || ''
    
    summary = {
      totalViolations: detectedCount,
      highConfidenceCount,
      primaryViolation,
      videoDurationSec: Math.floor(Math.random() * 600) + 60
    }
  }
  
  const date = new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000)
  
  return {
    taskId,
    callId,
    contentId,
    videoUrl: `https://example.com/videos/${callId}.mp4`,
    coverUrl: `https://example.com/covers/${callId}.jpg`,
    status,
    moderationResult,
    violations,
    summary,
    analysisDuration: status === 'COMPLETED' ? `${Math.floor(Math.random() * 30) + 5}s` : undefined,
    createdAt: date.toISOString()
  }
}

// 生成任务列表
export function generateTaskList(count: number = 20): VideoAnalysisTask[] {
  const tasks: VideoAnalysisTask[] = []
  for (let i = 1; i <= count; i++) {
    tasks.push(generateTask(i))
  }
  return tasks.sort((a, b) => new Date(b.createdAt!).getTime() - new Date(a.createdAt!).getTime())
}

// 预生成的任务列表
export const mockTaskList = generateTaskList(50)

// 获取任务列表
export function getTaskList(params?: {
  callId?: string
  contentId?: string
  status?: string
  result?: string
  page?: number
  pageSize?: number
}) {
  const { callId, contentId, status, result, page = 1, pageSize = 10 } = params || {}
  
  let filtered = [...mockTaskList]
  
  if (callId) {
    filtered = filtered.filter(t => t.callId.includes(callId))
  }
  if (contentId) {
    filtered = filtered.filter(t => t.contentId.includes(contentId))
  }
  if (status) {
    filtered = filtered.filter(t => t.status === status)
  }
  if (result) {
    filtered = filtered.filter(t => t.moderationResult === result)
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

// 获取任务详情
export function getTaskDetail(callId: string): VideoAnalysisTask | null {
  const task = mockTaskList.find(t => t.callId === callId)
  return task || null
}

export function reAnalyze(callId: string): VideoAnalysisTask {
  const existed = getTaskDetail(callId)
  if (!existed) {
    return createTask({
      callId,
      contentId: callId,
      videoUrl: '',
      coverUrl: undefined
    })
  }
  return createTask({
    callId: existed.callId,
    contentId: existed.contentId,
    videoUrl: existed.videoUrl,
    coverUrl: existed.coverUrl
  })
}

// 创建新任务
export function createTask(data: {
  callId: string
  contentId: string
  videoUrl: string
  coverUrl?: string
}): VideoAnalysisTask {
  const task: VideoAnalysisTask = {
    taskId: `task_${Date.now()}`,
    callId: data.callId,
    contentId: data.contentId,
    videoUrl: data.videoUrl,
    coverUrl: data.coverUrl,
    status: 'PENDING',
    createdAt: new Date().toISOString()
  }
  
  mockTaskList.unshift(task)
  return task
}

// 模拟任务处理（用于测试）
export function simulateTaskProcessing(callId: string): Promise<VideoAnalysisTask> {
  return new Promise((resolve) => {
    setTimeout(() => {
      const task = mockTaskList.find(t => t.callId === callId)
      if (task) {
        task.status = 'PROCESSING'
      }
      
      setTimeout(() => {
        if (task) {
          task.status = 'COMPLETED'
          task.violations = generateViolations()
          const detectedCount = task.violations.filter(v => v.detected).length
          
          if (detectedCount === 0) {
            task.moderationResult = 'NOT_HIT'
          } else if (detectedCount >= 2) {
            task.moderationResult = 'HIT'
          } else {
            task.moderationResult = 'SUSPECTED'
          }
          
          task.summary = {
            totalViolations: detectedCount,
            highConfidenceCount: task.violations.filter(v => v.detected && v.confidence > 0.85).length,
            primaryViolation: task.violations.find(v => v.detected)?.type || '',
            videoDurationSec: Math.floor(Math.random() * 600) + 60
          }
          task.analysisDuration = `${Math.floor(Math.random() * 30) + 5}s`
        }
        resolve(task!)
      }, 3000)
    }, 500)
  })
}

export function getPromptModules(analysisType: string) {
  if (analysisType === 'HOST_VIOLATION') {
    return {
      analysisType: 'HOST_VIOLATION',
      defaultModules: ['HOST_VIOLATION_BASE', 'HOST_VIOLATION_RULES', 'HOST_VIOLATION_JSON'],
      modules: [
        { code: 'HOST_VIOLATION_BASE', title: '任务说明与核心原则' },
        { code: 'HOST_VIOLATION_RULES', title: '违规类型判定规则' },
        { code: 'HOST_VIOLATION_JSON', title: 'JSON 输出规范' }
      ]
    }
  }
  return { analysisType, defaultModules: [], modules: [] }
}
