import request from '@/utils/request'

export interface LlmProfilePayload {
  configCode: string
  displayName: string
  provider: string
  endpoint: string
  model: string
  apiKey?: string
  timeoutMs: number
  maxTokens: number
  enabled: boolean
  isDefault?: boolean
}

export interface LlmProfile {
  configCode: string
  displayName: string
  provider: string
  endpoint: string
  model: string
  timeoutMs: number
  maxTokens: number
  enabled: boolean
  isDefault: boolean
  apiKeyConfigured: boolean
}

export interface LlmProfilesRes {
  defaultConfigCode?: string
  profiles: LlmProfile[]
}

export interface TestRes {
  success: boolean
  statusCode: number
  message: string
}

const normalizeProfilesRes = (payload: any): LlmProfilesRes => {
  const data = payload || {}
  if (Array.isArray(data.profiles)) {
    return data as LlmProfilesRes
  }
  if (data.llm) {
    return {
      defaultConfigCode: 'DEFAULT',
      profiles: [
        {
          configCode: 'DEFAULT',
          displayName: '默认模型',
          provider: data.llm.provider || 'custom',
          endpoint: data.llm.endpoint || '',
          model: data.llm.model || '',
          timeoutMs: data.llm.timeoutMs || 120000,
          maxTokens: data.llm.maxTokens || 3000,
          enabled: true,
          isDefault: true,
          apiKeyConfigured: !!data.llm.apiKeyConfigured
        }
      ]
    }
  }
  return {
    defaultConfigCode: '',
    profiles: []
  }
}

export const settingsApi = {
  getApiConfig: async () => {
    const res = await request.get<any>('/v1/settings/api-config')
    return normalizeProfilesRes(res.data)
  },
  saveApiConfig: async (payload: LlmProfilePayload) => {
    const res = await request.put<any>('/v1/settings/api-config', payload)
    return normalizeProfilesRes(res.data)
  },
  deleteApiConfig: async (configCode: string) => {
    const res = await request.delete<any>(`/v1/settings/api-config/${configCode}`)
    return normalizeProfilesRes(res.data)
  },
  setDefaultApiConfig: async (configCode: string) => {
    const res = await request.put<any>(`/v1/settings/api-config/default/${configCode}`)
    return normalizeProfilesRes(res.data)
  },
  testLlm: async (endpoint: string, timeoutMs: number, configCode?: string) => {
    const res = await request.post<TestRes>('/v1/settings/test-llm', { endpoint, timeoutMs, configCode })
    return res.data
  },
  testYidun: async (endpoint: string, timeoutMs: number) => {
    const res = await request.post<TestRes>('/v1/settings/test-yidun', { endpoint, timeoutMs })
    return res.data
  }
}
