import request from '@/utils/request'
import type { VideoAnalysisTask, VideoAnalyzeReq } from '@/types/video'

// 是否使用 Mock 数据
const USE_MOCK = false

// Mock 数据导入
import {
  getTaskList as mockGetTaskList,
  getTaskDetail as mockGetTaskDetail,
  reAnalyze as mockReAnalyze,
  createTask as mockCreateTask,
  getPromptModules as mockGetPromptModules
} from '@/mock'

// 任务列表响应类型
interface TaskListResponse {
  total: number
  page: number
  pageSize: number
  list: VideoAnalysisTask[]
}

/**
 * 视频分析 API
 */
export const videoApi = {
  /**
   * 发起视频分析
   */
  analyze: async (data: VideoAnalyzeReq): Promise<VideoAnalysisTask> => {
    if (USE_MOCK) {
      return mockCreateTask(data)
    }
    const res = await request.post<VideoAnalysisTask>('/v1/video/analyze', data)
    return res.data
  },

  /**
   * 查询分析结果
   */
  getResult: async (callId: string): Promise<VideoAnalysisTask | null> => {
    if (USE_MOCK) {
      return mockGetTaskDetail(callId)
    }
    const res = await request.get<VideoAnalysisTask>(`/v1/video/result/${callId}`)
    return res.data
  },

  reAnalyze: async (callId: string): Promise<VideoAnalysisTask> => {
    if (USE_MOCK) {
      return mockReAnalyze(callId)
    }
    const res = await request.post<VideoAnalysisTask>(`/v1/video/reanalyze/${callId}`)
    return res.data
  },

  /**
   * 获取任务列表
   */
  getList: async (params?: {
    callId?: string
    contentId?: string
    status?: string
    result?: string
    page?: number
    pageSize?: number
  }): Promise<TaskListResponse> => {
    if (USE_MOCK) {
      return mockGetTaskList(params)
    }
    const res = await request.get<TaskListResponse>('/v1/video/tasks', { params })
    return res.data
  },

  getPromptModules: async (analysisType: string): Promise<any> => {
    if (USE_MOCK) {
      return mockGetPromptModules(analysisType)
    }
    const res = await request.get<any>('/v1/video/prompt-modules', { params: { analysisType } })
    return res.data
  }
}
