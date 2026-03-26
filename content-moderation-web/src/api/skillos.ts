import request from '@/utils/request'

export interface SkillDefinition {
  skillId: string
  name: string
  type: string
  description: string
  tags: string[]
  outputSchema: Record<string, any>
  stateMapping: Record<string, any>
  executionConfig: Record<string, any>
  scriptConfig: Record<string, any>
  status: string
  timeoutMs: number
  version: string
}

export interface SkillTemplate {
  templateId: string
  name: string
  type: string
  description: string
  tags: string[]
  outputSchema: Record<string, any>
  stateMapping: Record<string, any>
  executionConfig: Record<string, any>
  scriptConfig: Record<string, any>
}

export interface PolicyDefinition {
  policyId: string
  name: string
  skillPipeline: string[]
  config: Record<string, any>
  executionInput: Record<string, any>
  version: string
}

export interface SkillExecutionTrace {
  traceId?: string
  stepId?: string
  status?: string
  skillId: string
  attempt?: number
  success: boolean
  skipped: boolean
  durationMs: number
  startedAt?: string
  endedAt?: string
  input?: any
  output?: any
  message?: string
}

export interface PolicyExecuteRes {
  executionId?: string
  planId?: string
  policyId: string
  status?: string
  success: boolean
  durationMs: number
  plan?: Record<string, any>
  state: Record<string, any>
  traces: SkillExecutionTrace[]
  errorMessage?: string
}

export interface SkillScriptTestRes {
  success: boolean
  message: string
  output: Record<string, any>
  writeToState: Record<string, any>
}

export const skillOsApi = {
  registerSkill: async (payload: Partial<SkillDefinition> & { skillId: string; description: string; type: string }) => {
    const res = await request.post<{ skills: SkillDefinition[] }>('/skills/register', payload)
    return res.data
  },
  saveSkillDraft: async (payload: Partial<SkillDefinition> & { skillId: string; description: string; type: string }) => {
    const res = await request.post<{ skills: SkillDefinition[] }>('/skills/draft', payload)
    return res.data
  },
  updateSkill: async (skillId: string, payload: Partial<SkillDefinition>) => {
    const res = await request.put<{ skills: SkillDefinition[] }>(`/skills/${skillId}`, payload)
    return res.data
  },
  publishSkill: async (skillId: string) => {
    const res = await request.post<{ skills: SkillDefinition[] }>(`/skills/${skillId}/publish`)
    return res.data
  },
  cloneSkill: async (skillId: string, payload: { newSkillId: string; newName?: string }) => {
    const res = await request.post<{ skills: SkillDefinition[] }>(`/skills/${skillId}/clone`, payload)
    return res.data
  },
  deleteSkill: async (skillId: string) => {
    const res = await request.post<{ skills: SkillDefinition[] }>(`/skills/${skillId}/delete`)
    return res.data
  },
  listSkills: async () => {
    const res = await request.get<{ skills: SkillDefinition[] }>('/skills/list')
    return res.data
  },
  listSkillTemplates: async () => {
    const res = await request.get<{ templates: SkillTemplate[] }>('/skills/templates')
    return res.data
  },
  getSkillUsages: async (skillId: string) => {
    const res = await request.get<{ skillId: string; policyIds: string[] }>(`/skills/${skillId}/usages`)
    return res.data
  },
  testSkillScript: async (skillId: string, payload: { input?: Record<string, any>; state?: Record<string, any>; scriptContent?: string }) => {
    const res = await request.post<SkillScriptTestRes>(`/skills/${skillId}/script/test`, payload)
    return res.data
  },
  registerPolicy: async (payload: Partial<PolicyDefinition> & { policyId: string; skillPipeline: string[] }) => {
    const res = await request.post<{ policies: PolicyDefinition[] }>('/policy/register', payload)
    return res.data
  },
  listPolicies: async () => {
    const res = await request.get<{ policies: PolicyDefinition[] }>('/policy/list')
    return res.data
  },
  deletePolicy: async (policyId: string) => {
    const res = await request.post<{ policies: PolicyDefinition[] }>(`/policy/${policyId}/delete`)
    return res.data
  },
  executePolicy: async (payload: { policyId: string; input: Record<string, any> }) => {
    const res = await request.post<PolicyExecuteRes>('/execute', payload, {
      timeout: 120000
    })
    return res.data
  },
  getLatestExecution: async (policyId: string) => {
    const res = await request.get<PolicyExecuteRes | null>(`/policy/${policyId}/executions/latest`)
    return res.data
  },
  getExecutionDetail: async (executionId: string) => {
    const res = await request.get<PolicyExecuteRes>(`/executions/${executionId}`)
    return res.data
  },
  replayExecution: async (executionId: string) => {
    const res = await request.post<PolicyExecuteRes>(`/executions/${executionId}/replay`)
    return res.data
  },
  submitExecutionFeedback: async (
    executionId: string,
    payload: { traceId?: string; score?: number; label?: string; action?: string; comment?: string; metadata?: Record<string, any> }
  ) => {
    const res = await request.post<{ executionId: string; saved: boolean }>(`/executions/${executionId}/feedback`, payload)
    return res.data
  }
}
