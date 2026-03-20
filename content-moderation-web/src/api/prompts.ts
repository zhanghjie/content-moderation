import request from '@/utils/request'

export interface PromptModuleItem {
  code: string
  title: string
  category: 'REQUIRED' | 'PLUGGABLE' | 'FREE'
  content: string
  enabled: boolean
  sortOrder: number
}

export interface PromptModulesManageRes {
  analysisType: string
  defaultModules: string[]
  modules: PromptModuleItem[]
}

export interface PromptSplitRes {
  analysisType: string
  llmConfigCode: string
  rawModelResponse: string
  modules: PromptModuleItem[]
}

export interface PromptDslValidateRes {
  dslType: string
  valid: boolean
  errors: string[]
}

export interface WorkflowExecuteTrace {
  nodeId: string
  nodeType: string
  durationMs: number
  input: any
  output: any
  errorMessage?: string
}

export interface WorkflowExecuteRes {
  success: boolean
  durationMs: number
  output: Record<string, any>
  nodeTraces: WorkflowExecuteTrace[]
  errorMessage?: string
}

export const promptApi = {
  getModules: async (analysisType = 'HOST_VIOLATION') => {
    const res = await request.get<PromptModulesManageRes>('/v1/prompts/modules', { params: { analysisType } })
    return res.data
  },
  createModule: async (payload: Partial<PromptModuleItem> & { analysisType: string; code: string; content: string }) => {
    const res = await request.post<PromptModulesManageRes>('/v1/prompts/modules', payload)
    return res.data
  },
  updateModule: async (analysisType: string, code: string, payload: Partial<PromptModuleItem>) => {
    const res = await request.put<PromptModulesManageRes>(`/v1/prompts/modules/${analysisType}/${code}`, payload)
    return res.data
  },
  deleteModule: async (analysisType: string, code: string) => {
    const res = await request.delete<PromptModulesManageRes>(`/v1/prompts/modules/${analysisType}/${code}`)
    return res.data
  },
  saveDefaultModules: async (analysisType: string, defaultModules: string[]) => {
    const res = await request.put<PromptModulesManageRes>('/v1/prompts/default-modules', { analysisType, defaultModules })
    return res.data
  },
  composePreview: async (payload: {
    analysisType: string
    contentId: string
    callId: string
    videoUrl: string
    userId?: number
    modules: string[]
  }) => {
    const res = await request.post<{ analysisType: string; prompt: string }>('/v1/prompts/compose-preview', payload)
    return res.data
  },
  splitPrompt: async (payload: {
    analysisType: string
    rawPrompt: string
    llmConfigCode?: string
    applyToScene?: boolean
  }) => {
    const res = await request.post<PromptSplitRes>('/v1/prompts/split', payload)
    return res.data
  },
  validateDsl: async (dsl: string) => {
    const res = await request.post<PromptDslValidateRes>('/v1/prompts/dsl/validate', { dsl })
    return res.data
  },
  executeWorkflow: async (payload: {
    workflowDsl: string
    promptDsls?: string[]
    inputs?: Record<string, any>
  }) => {
    const res = await request.post<WorkflowExecuteRes>('/v1/prompts/workflow/execute', payload)
    return res.data
  }
}
