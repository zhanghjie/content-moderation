<template>
  <div class="policy-center-page">
    <el-card shadow="never" class="header-card">
      <div class="header-row">
        <div>
          <h2 class="title">Policy 编排编辑</h2>
          <p class="subtitle">当前页用于完整编排、新增与调试，列表页仅保留摘要</p>
        </div>
        <el-space>
          <el-button @click="backToOverview">返回摘要页</el-button>
          <el-button :loading="loadingSkills" @click="loadSkills">刷新 Skill</el-button>
        </el-space>
      </div>
    </el-card>

    <el-card shadow="never" class="panel-card flow-card">
      <template #header>
        <div class="panel-header">
          <span>Policy 执行链路</span>
          <el-tag type="success" effect="dark" round>实时运行</el-tag>
        </div>
      </template>
      <div class="flow-track">
        <template v-for="(node, index) in executionFlow" :key="node.key">
          <div class="flow-node-wrap">
            <div class="flow-node" :style="{ '--node-color': node.color }">
              <span class="flow-node-index">{{ index + 1 }}</span>
            </div>
            <div class="flow-node-title">{{ node.title }}</div>
            <div class="flow-node-subtitle">{{ node.subtitle }}</div>
          </div>
          <div v-if="index < executionFlow.length - 1" class="flow-arrow">→</div>
        </template>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="24" :md="24" :lg="16">
        <el-card shadow="never" class="panel-card pipeline-panel">
          <template #header>
            <div class="panel-header">
              <span>Pipeline 编排</span>
              <el-tag type="info" effect="plain">{{ form.skillPipeline.length }} 步</el-tag>
            </div>
          </template>
          <div :key="`${form.policyId}-${pipelineRenderToken}`" class="pipeline-flow">
            <div class="flow-endpoint start">
              <span class="dot" />
              <span class="endpoint-text">起点</span>
            </div>
            <div class="flow-link-block">
              <div class="flow-side-line" />
              <div class="flow-semantic" :title="getEntrySemanticText()">{{ getEntrySemanticText() }}</div>
              <button class="flow-add-inline insert-point" @click="openAddSkillPanel(0)">+ 插入节点</button>
              <div class="flow-side-line" />
            </div>
            <div v-if="!form.skillPipeline.length" class="flow-empty-wrap">
              <el-empty description="请先添加 Skill 到 Pipeline" :image-size="80" />
            </div>
            <template v-for="(skillId, index) in form.skillPipeline" :key="`${skillId}-${index}`">
              <div class="flow-node-row">
                <div
                  class="pipeline-item"
                  :class="{
                    'drag-over': dragOverIndex === index,
                    [getTypeBorderClass(getSkillType(skillId))]: true,
                    [`status-${getPipelineStatus(index, skillId)}`]: true
                  }"
                  draggable="true"
                  @click="setActivePipelineIndex(index)"
                  @dragstart="onDragStart(index)"
                  @dragenter.prevent="onDragEnter(index)"
                  @dragover.prevent
                  @drop="onDrop(index)"
                  @dragend="onDragEnd"
                >
                  <div class="pipeline-card-header">
                    <div class="pipeline-card-title">
                      <span class="node-bullet" />
                      <span class="pipeline-index">{{ index + 1 }}</span>
                      <span>{{ getPipelineSkillName(skillId) }}</span>
                    </div>
                    <div class="pipeline-card-meta">
                      <span class="node-status" :class="`state-${getPipelineStatus(index, skillId)}`">
                        {{ getPipelineStatusText(index, skillId) }}
                      </span>
                      <el-tag size="small" :class="getTypeTagClass(getSkillType(skillId))">{{ getTypeText(getSkillType(skillId)) }}</el-tag>
                    </div>
                    <div class="pipeline-actions">
                      <el-button text type="primary" @click="openSkillConfig(skillId)">⚙</el-button>
                      <el-button text @click="toggleExpand(index)">{{ expandedSkillIndexes.includes(index) ? '收起' : '⋯' }}</el-button>
                      <el-button text type="danger" @click="removeSkill(index)">删除</el-button>
                    </div>
                  </div>
                  <div class="pipeline-card-divider" />
                  <div class="pipeline-readonly-hint">以下信息来自 Skill 注册中心定义（只读）</div>
                  <div class="pipeline-io-grid">
                    <div class="io-block">
                      <div class="io-heading">Prompt 摘要</div>
                      <div class="io-brief">{{ getSkillPromptSummary(skillId) }}</div>
                    </div>
                    <div class="io-block">
                      <div class="io-heading">模型配置</div>
                      <div class="io-items">
                        <span v-for="item in getSkillModelTags(skillId)" :key="`model-${index}-${item}`" class="io-chip">{{ item }}</span>
                        <span v-if="!getSkillModelTags(skillId).length" class="io-chip empty">未配置</span>
                      </div>
                    </div>
                    <div class="io-block">
                      <div class="io-heading">返回字段</div>
                      <div class="io-items">
                        <span v-for="item in getSkillOutputKeys(skillId)" :key="`output-${index}-${item}`" class="io-chip">{{ item }}</span>
                        <span v-if="!getSkillOutputKeys(skillId).length" class="io-chip empty">未配置</span>
                      </div>
                    </div>
                  </div>
                  <el-collapse-transition>
                    <div v-show="expandedSkillIndexes.includes(index)" class="pipeline-expand">
                      <div class="expand-line"><span class="info-label">Skill ID</span><span>{{ skillId }}</span></div>
                      <div class="expand-line"><span class="info-label">描述</span><span>{{ getSkillDescription(skillId) }}</span></div>
                    </div>
                  </el-collapse-transition>
                </div>
              </div>
              <div v-if="index < form.skillPipeline.length - 1" class="flow-link-block">
                <div class="flow-side-line" />
                <div class="flow-semantic" :title="getConnectorSemanticText(index)">{{ getConnectorSemanticText(index) }}</div>
                <button class="flow-add-inline insert-point" @click="openAddSkillPanel(index + 1)">+ 插入节点</button>
                <div class="flow-side-line" />
              </div>
            </template>
            <div class="flow-link-block">
              <div class="flow-side-line" />
              <div class="flow-semantic" :title="getExitSemanticText()">{{ getExitSemanticText() }}</div>
              <button class="flow-add-inline insert-point" @click="openAddSkillPanel(form.skillPipeline.length)">+ 插入节点</button>
              <div class="flow-side-line" />
            </div>
            <div class="flow-endpoint end">
              <span class="dot" />
              <span class="endpoint-text">结束</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="24" :lg="8">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>Policy 配置</span>
              <el-tag type="success" effect="plain">可编辑</el-tag>
            </div>
          </template>
          <el-form label-position="top">
            <el-form-item label="Policy ID">
              <el-input v-model="form.policyId" :disabled="isPolicySelected" placeholder="例如：video_risk_v1" />
            </el-form-item>
            <el-form-item label="名称">
              <el-input v-model="form.name" placeholder="例如：视频风控默认策略" />
            </el-form-item>
            <el-form-item label="版本">
              <el-input v-model="form.version" placeholder="例如：v1" />
            </el-form-item>
            <el-alert type="info" :closable="false" show-icon title="Policy 仅编排 Skill 顺序与策略参数；Schema 请在 Skill 注册中心维护" />
          </el-form>
          <el-space class="action-row" fill>
            <el-button type="primary" :loading="savingPolicy" @click="savePolicy">保存 Policy</el-button>
            <el-button :loading="executing" @click="runPolicy">运行 Policy</el-button>
          </el-space>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="panel-header">
          <div>
            <span>执行结果（State）</span>
            <el-tag v-if="executeResult" :type="executeResult.success ? 'success' : 'danger'" style="margin-left: 8px">
              {{ executeResult.success ? '成功' : '失败' }}
            </el-tag>
          </div>
          <el-button type="primary" plain size="small" :loading="savingInput" @click="saveExecutionInput">保存执行输入</el-button>
        </div>
      </template>
      <div class="state-stat-grid">
        <div class="stat-item">
          <div class="stat-label">执行状态</div>
          <div class="stat-value">{{ executeResult?.success ? 'SUCCESS' : executeResult ? 'FAILED' : '-' }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">总耗时</div>
          <div class="stat-value">{{ executeResult ? `${executeResult.durationMs} ms` : '-' }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">执行节点</div>
          <div class="stat-value">{{ executeResult ? executeResult.traces.length : 0 }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">执行ID</div>
          <div class="stat-value mono">{{ executeResult?.executionId || '-' }}</div>
        </div>
      </div>

      <div v-if="failureDebugLines.length" class="failure-debug">
        <div class="failure-debug-title">失败原因（调试）</div>
        <el-alert type="error" :closable="false" show-icon :title="failurePrimaryReason" />
        <div class="failure-debug-lines">
          <div v-for="line in failureDebugLines" :key="line" class="failure-line">{{ line }}</div>
        </div>
      </div>

      <div class="result-toolbar">
        <el-radio-group v-model="stateResultWrap">
          <el-radio-button :label="false">不换行</el-radio-button>
          <el-radio-button :label="true">自动换行</el-radio-button>
        </el-radio-group>
      </div>

      <el-tabs v-model="activeStateTab" class="state-tabs">
        <el-tab-pane label="概览" name="overview">
          <div class="json-section">
            <div class="json-panel full">
              <div class="json-panel-header">
                <span>执行输入</span>
                <el-space>
                  <el-button text size="small" @click="copyText(runInputText)">复制</el-button>
                  <el-button text size="small" @click="openJsonDialog('执行输入', runInputText)">放大</el-button>
                </el-space>
              </div>
              <el-input v-model="runInputText" type="textarea" :rows="7" />
            </div>
            <div class="json-panel">
              <div class="json-panel-header">
                <span>AI解析结果</span>
                <el-space>
                  <el-button text size="small" @click="copyText(aiParsedResultText)">复制</el-button>
                  <el-button text size="small" @click="openJsonDialog('AI解析结果', aiParsedResultText)">放大</el-button>
                </el-space>
              </div>
              <pre class="json-view" :class="{ wrap: stateResultWrap }">{{ aiParsedResultText }}</pre>
            </div>
            <div class="json-panel">
              <div class="json-panel-header">
                <span>执行状态全量 State</span>
                <el-space>
                  <el-button text size="small" @click="copyText(executeResultText)">复制</el-button>
                  <el-button text size="small" @click="openJsonDialog('执行状态全量 State', executeResultText)">放大</el-button>
                </el-space>
              </div>
              <pre class="json-view" :class="{ wrap: stateResultWrap }">{{ executeResultText }}</pre>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="节点明细" name="traces">
          <div class="trace-layout">
            <el-table
              :data="traceRows"
              size="small"
              highlight-current-row
              class="trace-table"
              @row-click="onTraceRowClick"
            >
              <el-table-column prop="skillId" label="Skill" min-width="170" />
              <el-table-column prop="durationMs" label="耗时(ms)" width="110" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.success ? 'success' : 'danger'">{{ row.skipped ? 'SKIPPED' : row.success ? 'OK' : 'FAIL' }}</el-tag>
                </template>
              </el-table-column>
            </el-table>

            <div class="trace-detail">
              <div class="json-panel">
                <div class="json-panel-header">
                  <span>节点输入</span>
                  <el-space>
                    <el-button text size="small" @click="copyText(selectedTraceInputText)">复制</el-button>
                    <el-button text size="small" @click="openJsonDialog('节点输入', selectedTraceInputText)">放大</el-button>
                  </el-space>
                </div>
                <pre class="json-view" :class="{ wrap: stateResultWrap }">{{ selectedTraceInputText }}</pre>
              </div>
              <div class="json-panel">
                <div class="json-panel-header">
                  <span>节点输出</span>
                  <el-space>
                    <el-button text size="small" @click="copyText(selectedTraceOutputText)">复制</el-button>
                    <el-button text size="small" @click="openJsonDialog('节点输出', selectedTraceOutputText)">放大</el-button>
                  </el-space>
                </div>
                <pre class="json-view" :class="{ wrap: stateResultWrap }">{{ selectedTraceOutputText }}</pre>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="原始数据" name="raw">
          <div class="json-section">
            <div class="json-panel">
              <div class="json-panel-header">
                <span>Execution Plan</span>
                <el-space>
                  <el-button text size="small" @click="copyText(executePlanText)">复制</el-button>
                  <el-button text size="small" @click="openJsonDialog('Execution Plan', executePlanText)">放大</el-button>
                </el-space>
              </div>
              <pre class="json-view" :class="{ wrap: stateResultWrap }">{{ executePlanText }}</pre>
            </div>
            <div class="json-panel">
              <div class="json-panel-header">
                <span>Traces 原始响应</span>
                <el-space>
                  <el-button text size="small" @click="copyText(executeTracesText)">复制</el-button>
                  <el-button text size="small" @click="openJsonDialog('Traces 原始响应', executeTracesText)">放大</el-button>
                </el-space>
              </div>
              <pre class="json-view" :class="{ wrap: stateResultWrap }">{{ executeTracesText }}</pre>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <el-dialog v-model="jsonDialogVisible" :title="jsonDialogTitle" width="78vw" top="4vh" destroy-on-close>
        <pre class="json-dialog-view" :class="{ wrap: stateResultWrap }">{{ jsonDialogContent }}</pre>
      </el-dialog>
    </el-card>

    <el-dialog v-model="addSkillDialogVisible" title="添加 Skill" width="620px">
      <el-input v-model="addSkillKeyword" placeholder="搜索 Skill 名称 / ID / 类型" clearable />
      <div class="add-skill-panel">
        <div v-for="skill in filteredAddSkills" :key="skill.skillId" class="add-skill-item" @click="appendSkill(skill.skillId)">
          <div class="add-skill-title">{{ skill.name || skill.skillId }}</div>
          <div class="add-skill-meta">{{ skill.skillId }} · {{ getTypeText(skill.type) }}</div>
        </div>
        <el-empty v-if="!filteredAddSkills.length" description="无匹配 Skill" :image-size="70" />
      </div>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { skillOsApi, type PolicyDefinition, type PolicyExecuteRes, type SkillDefinition } from '@/api/skillos'

const route = useRoute()
const router = useRouter()
const loadingPolicies = ref(false)
const loadingSkills = ref(false)
const savingPolicy = ref(false)
const savingInput = ref(false)
const deletingPolicy = ref(false)
const executing = ref(false)
const dragFromIndex = ref<number | null>(null)
const dragOverIndex = ref<number | null>(null)
const expandedSkillIndexes = ref<number[]>([])
const addSkillDialogVisible = ref(false)
const addSkillKeyword = ref('')
const addSkillInsertIndex = ref<number | null>(null)
const activePipelineIndex = ref<number | null>(null)
const pipelineRenderToken = ref(0)
const executionFlow = [
  { key: 'policy', title: 'Policy', subtitle: '加载策略定义', color: '#3b82f6' },
  { key: 'planner', title: 'Planner', subtitle: '产出可执行 Plan', color: '#0ea5e9' },
  { key: 'executor', title: 'PlanExecutor', subtitle: '统一执行语义', color: '#22c55e' },
  { key: 'feedback', title: '反馈 / 评估', subtitle: '结果反馈与质量评估', color: '#f97316' },
  { key: 'trace', title: 'Trace / 回放', subtitle: '执行轨迹可追溯', color: '#a855f7' }
]

const policies = ref<PolicyDefinition[]>([])
const skills = ref<SkillDefinition[]>([])
const executeResult = ref<PolicyExecuteRes | null>(null)
const executeResultByPolicy = ref<Record<string, PolicyExecuteRes>>({})
const defaultRunInputText = '{"videoUrl":"https://demo/video_call_black.mp4","transcript":"来床上聊，黑屏画面很多"}'
const runInputText = ref(defaultRunInputText)
const configText = ref('{}')
const activeStateTab = ref<'overview' | 'traces' | 'raw'>('overview')
const stateResultWrap = ref(false)
const selectedTraceIndex = ref(0)
const jsonDialogVisible = ref(false)
const jsonDialogTitle = ref('')
const jsonDialogContent = ref('')

const form = ref<PolicyDefinition>({
  policyId: '',
  name: '',
  version: 'v1',
  skillPipeline: [],
  config: {},
  executionInput: {}
})

const isPolicySelected = computed(() => policies.value.some(item => item.policyId === form.value.policyId))

const executeResultText = computed(() => JSON.stringify(executeResult.value?.state || {}, null, 2))
const executePlanText = computed(() => JSON.stringify(executeResult.value?.plan || {}, null, 2))
const executeTracesText = computed(() => JSON.stringify(executeResult.value?.traces || [], null, 2))
const traceRows = computed(() => executeResult.value?.traces || [])
const selectedTrace = computed<any | null>(() => {
  const rows = traceRows.value as any[]
  if (!rows.length) return null
  const safeIndex = Math.min(Math.max(selectedTraceIndex.value, 0), rows.length - 1)
  return rows[safeIndex] || null
})
const selectedTraceInputText = computed(() => formatTraceOutput(selectedTrace.value?.input))
const selectedTraceOutputText = computed(() => formatTraceOutput(selectedTrace.value?.output))
const aiParsedResultText = computed(() => {
  const payload = getAiParsedResult(executeResult.value)
  if (payload === null || payload === undefined) {
    return '暂无AI解析结果，请先运行 Policy'
  }
  if (typeof payload === 'string') {
    return payload
  }
  return JSON.stringify(payload, null, 2)
})
const failureDebugLines = computed(() => {
  const result = executeResult.value as any
  if (!result || result.success) return [] as string[]
  const lines: string[] = []
  if (result.errorMessage) {
    lines.push(`执行错误: ${result.errorMessage}`)
  }
  const stateError = result.state?.errorMessage
  if (stateError && stateError !== result.errorMessage) {
    lines.push(`状态错误: ${stateError}`)
  }
  if (result.executionId) {
    lines.push(`executionId: ${result.executionId}`)
  }
  if (result.planId) {
    lines.push(`planId: ${result.planId}`)
  }
  const traces = Array.isArray(result.traces) ? result.traces : []
  if (!traces.length) {
    lines.push('trace为空：失败发生在技能执行前或首个技能启动阶段')
  } else {
    const failed = traces.filter((item: any) => !item.success && !item.skipped)
    if (failed.length) {
      const latest = failed[failed.length - 1]
      lines.push(`失败节点: ${latest.skillId || '-'} / 状态: ${latest.status || (latest.success ? 'OK' : 'FAIL')}`)
      if (latest.message) {
        lines.push(`节点错误: ${latest.message}`)
      }
    }
  }
  if (String(result.errorMessage || '').includes('401')) {
    lines.push('401提示：当前模型配置鉴权失败，请检查 llm_config_code 对应的 key/endpoint')
  }
  return lines
})
const failurePrimaryReason = computed(() => {
  return failureDebugLines.value[0] || '执行失败'
})
const skillMap = computed(() => {
  const map = new Map<string, SkillDefinition>()
  skills.value.forEach(item => map.set(item.skillId, item))
  return map
})

const normalizeConfigText = (config?: Record<string, any>) => JSON.stringify(config || {}, null, 2)
const normalizeExecutionInputText = (executionInput?: Record<string, any>) =>
  JSON.stringify(executionInput && typeof executionInput === 'object' ? executionInput : {}, null, 2)

const getTypeText = (type?: string) => {
  const value = String(type || '').toUpperCase()
  if (value === 'PERCEPTION') return '感知类'
  if (value === 'SEMANTIC') return '语义类'
  if (value === 'DECISION') return '决策类'
  if (value === 'GUARD') return '校验类'
  if (value === 'OUTPUT') return '输出类'
  return type || '-'
}

const getPipelineSkillName = (skillId: string) => {
  const skill = skillMap.value.get(skillId)
  return skill?.name || skillId
}

const getSkillType = (skillId: string) => {
  const skill = skillMap.value.get(skillId)
  return skill?.type || ''
}

const getSkillDescription = (skillId: string) => {
  const skill = skillMap.value.get(skillId)
  return skill?.description || '-'
}

const extractListValues = (value: any) => {
  if (!value) return [] as string[]
  if (Array.isArray(value)) return value.map(item => String(item))
  if (typeof value === 'object') return Object.keys(value)
  return [String(value)]
}

const getSkillPromptSummary = (skillId: string) => {
  const skill = skillMap.value.get(skillId) as any
  if (!skill) return '未找到 Skill 定义'
  const prompt = String(skill.scriptConfig?.prompt || '').trim()
  if (!prompt) return '未配置 Prompt'
  const firstLine = prompt.replace(/\s+/g, ' ').trim()
  return firstLine.length > 72 ? `${firstLine.slice(0, 72)}...` : firstLine
}

const getSkillModelTags = (skillId: string) => {
  const skill = skillMap.value.get(skillId) as any
  if (!skill) return [] as string[]
  const executionConfig = skill.executionConfig || {}
  const mode = String(executionConfig.execution_mode || '').trim()
  const model = String(executionConfig.llm_model || '').trim()
  const configCode = String(executionConfig.llm_config_code || '').trim()
  const values = [mode && `mode:${mode}`, model && `model:${model}`, configCode && `config:${configCode}`].filter(Boolean) as string[]
  return values
}

const getSkillOutputKeys = (skillId: string) => {
  const skill = skillMap.value.get(skillId) as any
  if (!skill) return [] as string[]
  const outputSchema = skill.outputSchema || {}
  return extractListValues(outputSchema).slice(0, 8)
}

const getTypeTagClass = (type?: string) => {
  const value = String(type || '').toUpperCase()
  if (value === 'PERCEPTION') return 'tag-perception'
  if (value === 'SEMANTIC') return 'tag-semantic'
  if (value === 'GUARD') return 'tag-guard'
  if (value === 'DECISION') return 'tag-decision'
  return 'tag-output'
}

const getTypeBorderClass = (type?: string) => {
  const value = String(type || '').toUpperCase()
  if (value === 'PERCEPTION') return 'border-perception'
  if (value === 'SEMANTIC') return 'border-semantic'
  if (value === 'GUARD') return 'border-guard'
  if (value === 'DECISION') return 'border-decision'
  return 'border-output'
}

const filteredAddSkills = computed(() => {
  const keyword = addSkillKeyword.value.trim().toLowerCase()
  if (!keyword) return skills.value
  return skills.value.filter(skill => {
    const name = (skill.name || '').toLowerCase()
    const id = (skill.skillId || '').toLowerCase()
    const type = (skill.type || '').toLowerCase()
    return name.includes(keyword) || id.includes(keyword) || type.includes(keyword)
  })
})

const getAiParsedResult = (result: PolicyExecuteRes | null) => {
  if (!result) return null
  const state = (result.state || {}) as Record<string, any>
  const preferredKeys = ['finalResult', 'result', 'analysis', 'decision', 'summary']
  for (const key of preferredKeys) {
    if (key in state && state[key] !== null && state[key] !== undefined && state[key] !== '') {
      return state[key]
    }
  }
  const traces = result.traces || []
  for (let index = traces.length - 1; index >= 0; index -= 1) {
    const trace = traces[index] as any
    if (trace && trace.success && trace.output !== null && trace.output !== undefined) {
      return trace.output
    }
  }
  const ignored = new Set(['input', 'policyId', 'success', 'durationMs', 'errorMessage'])
  for (const key of Object.keys(state).reverse()) {
    if (!ignored.has(key) && state[key] !== null && state[key] !== undefined) {
      return state[key]
    }
  }
  return null
}

const formatTraceOutput = (value: any) => {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'string') return value
  return JSON.stringify(value, null, 2)
}

const onTraceRowClick = (row: any) => {
  const rows = traceRows.value as any[]
  const index = rows.findIndex(item => item === row)
  if (index >= 0) {
    selectedTraceIndex.value = index
  }
}

const openJsonDialog = (title: string, content: string) => {
  jsonDialogTitle.value = title
  jsonDialogContent.value = content || ''
  jsonDialogVisible.value = true
}

const copyText = async (content: string) => {
  const text = String(content || '')
  if (!text) {
    ElMessage.warning('无可复制内容')
    return
  }
  try {
    if (navigator?.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      ElMessage.success('复制成功')
      return
    }
  } catch (error) {
    console.warn(error)
  }
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  try {
    document.execCommand('copy')
    ElMessage.success('复制成功')
  } catch (error) {
    console.warn(error)
    ElMessage.error('复制失败')
  } finally {
    document.body.removeChild(textarea)
  }
}

const getTraceForPipelineIndex = (index: number, skillId: string) => {
  const traces = executeResult.value?.traces || []
  const occurrence = form.value.skillPipeline.slice(0, index + 1).filter(item => item === skillId).length
  let current = 0
  for (const trace of traces as any[]) {
    if (trace?.skillId !== skillId) continue
    current += 1
    if (current === occurrence) {
      return trace
    }
  }
  return null
}

const getPipelineStatus = (index: number, skillId: string) => {
  if (activePipelineIndex.value === index) return 'selected'
  if (executing.value) return index === (activePipelineIndex.value ?? 0) ? 'running' : 'pending'
  const trace = getTraceForPipelineIndex(index, skillId) as any
  if (!trace) return 'pending'
  if (trace.success) return 'done'
  return 'error'
}

const getPipelineStatusText = (index: number, skillId: string) => {
  const status = getPipelineStatus(index, skillId)
  if (status === 'selected') return '已选中'
  if (status === 'running') return '执行中'
  if (status === 'done') return '已完成'
  if (status === 'error') return '异常'
  return '待执行'
}

const getEntrySemanticText = () => {
  return form.value.skillPipeline[0] ? '会话输入 → 首节点' : '会话输入 → 待插入节点'
}

const getConnectorSemanticText = (index: number) => {
  const fromSkillId = form.value.skillPipeline[index]
  const toSkillId = form.value.skillPipeline[index + 1]
  if (!fromSkillId || !toSkillId) return '节点执行流转'
  return `${getPipelineSkillName(fromSkillId)} → ${getPipelineSkillName(toSkillId)}`
}

const getExitSemanticText = () => {
  const last = form.value.skillPipeline[form.value.skillPipeline.length - 1]
  return last ? `${getPipelineSkillName(last)} → 结果输出` : '待插入节点 → 结果输出'
}

const backToOverview = () => {
  router.push('/settings/policies')
}

const applyRoutePolicySelection = () => {
  const mode = String(route.query.mode || '').toLowerCase()
  const policyId = String(route.query.policyId || '').trim()
  if (mode === 'new') {
    createNewPolicy(false)
    return
  }
  if (policyId) {
    const matched = policies.value.find(item => item.policyId === policyId)
    if (matched) {
      selectPolicy(matched, false)
      return
    }
  }
  if (!form.value.policyId && policies.value.length) {
    selectPolicy(policies.value[0], false)
  }
}

const loadPolicies = async () => {
  loadingPolicies.value = true
  try {
    const res = await skillOsApi.listPolicies()
    policies.value = (res.policies || []).slice().sort((a, b) => a.policyId.localeCompare(b.policyId))
    applyRoutePolicySelection()
  } finally {
    loadingPolicies.value = false
  }
}

const loadSkills = async () => {
  loadingSkills.value = true
  try {
    const res = await skillOsApi.listSkills()
    skills.value = (res.skills || []).slice().sort((a, b) => a.skillId.localeCompare(b.skillId))
  } finally {
    loadingSkills.value = false
  }
}

const selectPolicy = (policy: PolicyDefinition, syncRoute = true) => {
  form.value = {
    policyId: policy.policyId,
    name: policy.name || '',
    version: policy.version || 'v1',
    skillPipeline: [...(policy.skillPipeline || [])],
    config: { ...(policy.config || {}) },
    executionInput: { ...(policy.executionInput || {}) }
  }
  pipelineRenderToken.value += 1
  activePipelineIndex.value = null
  configText.value = normalizeConfigText(policy.config)
  runInputText.value = normalizeExecutionInputText(policy.executionInput)
  executeResult.value = executeResultByPolicy.value[policy.policyId] || null
  loadLatestExecution(policy.policyId)
  if (syncRoute) {
    router.replace({ path: '/settings/policies/editor', query: { policyId: policy.policyId } })
  }
}

const createNewPolicy = (syncRoute = true) => {
  form.value = {
    policyId: '',
    name: '',
    version: 'v1',
    skillPipeline: [],
    config: {},
    executionInput: {}
  }
  pipelineRenderToken.value += 1
  activePipelineIndex.value = null
  configText.value = '{}'
  runInputText.value = defaultRunInputText
  executeResult.value = null
  if (syncRoute) {
    router.replace({ path: '/settings/policies/editor', query: { mode: 'new' } })
  }
}

const openAddSkillPanel = (insertIndex?: number) => {
  addSkillInsertIndex.value = typeof insertIndex === 'number' ? insertIndex : form.value.skillPipeline.length
  addSkillKeyword.value = ''
  addSkillDialogVisible.value = true
}

const appendSkill = (skillId: string) => {
  const index = addSkillInsertIndex.value ?? form.value.skillPipeline.length
  form.value.skillPipeline.splice(index, 0, skillId)
  activePipelineIndex.value = index
  addSkillDialogVisible.value = false
  addSkillInsertIndex.value = null
  ElMessage.success('Skill 已插入流程')
}

const removeSkill = (index: number) => {
  form.value.skillPipeline.splice(index, 1)
  if (activePipelineIndex.value !== null) {
    if (activePipelineIndex.value === index) activePipelineIndex.value = null
    if (activePipelineIndex.value > index) activePipelineIndex.value -= 1
  }
  expandedSkillIndexes.value = expandedSkillIndexes.value
    .filter(item => item !== index)
    .map(item => (item > index ? item - 1 : item))
}

const onDragStart = (index: number) => {
  dragFromIndex.value = index
}

const onDragEnter = (index: number) => {
  dragOverIndex.value = index
}

const onDrop = (targetIndex: number) => {
  if (dragFromIndex.value === null || dragFromIndex.value === targetIndex) return
  const copy = [...form.value.skillPipeline]
  const [moved] = copy.splice(dragFromIndex.value, 1)
  copy.splice(targetIndex, 0, moved)
  form.value.skillPipeline = copy
  activePipelineIndex.value = targetIndex
  dragOverIndex.value = null
  expandedSkillIndexes.value = []
  dragFromIndex.value = null
}

const onDragEnd = () => {
  dragFromIndex.value = null
  dragOverIndex.value = null
}

const toggleExpand = (index: number) => {
  if (expandedSkillIndexes.value.includes(index)) {
    expandedSkillIndexes.value = expandedSkillIndexes.value.filter(item => item !== index)
    return
  }
  expandedSkillIndexes.value = [...expandedSkillIndexes.value, index]
}

const setActivePipelineIndex = (index: number) => {
  activePipelineIndex.value = index
}

const openSkillConfig = (skillId: string) => {
  const skill = skillMap.value.get(skillId)
  if (!skill) {
    ElMessage.warning('未找到该 Skill 的配置')
    return
  }
  router.push({
    path: '/settings/skills',
    query: {
      mode: 'edit',
      skillId,
      from: 'policy-editor',
      policyId: form.value.policyId || ''
    }
  })
}

const savePolicy = async () => {
  if (!form.value.policyId.trim()) {
    ElMessage.warning('请输入 Policy ID')
    return
  }
  if (!form.value.skillPipeline.length) {
    ElMessage.warning('请至少添加一个 Skill')
    return
  }
  savingPolicy.value = true
  try {
    const parsedConfig = JSON.parse(configText.value || '{}')
    if (typeof parsedConfig !== 'object' || parsedConfig === null || Array.isArray(parsedConfig)) {
      ElMessage.warning('策略配置必须是 JSON 对象')
      savingPolicy.value = false
      return
    }
    const payload = {
      policyId: form.value.policyId.trim(),
      name: form.value.name.trim() || form.value.policyId.trim(),
      version: form.value.version || 'v1',
      skillPipeline: [...form.value.skillPipeline],
      config: parsedConfig,
      executionInput: parseRunInput()
    }
    const res = await skillOsApi.registerPolicy(payload)
    policies.value = (res.policies || []).slice().sort((a, b) => a.policyId.localeCompare(b.policyId))
    const current = policies.value.find(item => item.policyId === payload.policyId)
    if (current) selectPolicy(current)
    ElMessage.success('Policy 保存成功')
  } catch (error: any) {
    ElMessage.error(error?.message || 'Policy 保存失败')
  } finally {
    savingPolicy.value = false
  }
}

const deleteCurrentPolicy = async () => {
  const policyId = form.value.policyId.trim()
  if (!policyId) {
    ElMessage.warning('请先选择 Policy')
    return
  }
  if (!isPolicySelected.value) {
    ElMessage.warning('当前 Policy 尚未保存，无法删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除 Policy：${form.value.name || policyId}（${policyId}）？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  deletingPolicy.value = true
  try {
    const res = await skillOsApi.deletePolicy(policyId)
    policies.value = (res.policies || []).slice().sort((a, b) => a.policyId.localeCompare(b.policyId))
    if (policies.value.length) {
      selectPolicy(policies.value[0])
    } else {
      createNewPolicy()
    }
    ElMessage.success('Policy 已删除')
  } catch (error: any) {
    ElMessage.error(error?.message || 'Policy 删除失败')
  } finally {
    deletingPolicy.value = false
  }
}

const saveExecutionInput = async () => {
  if (!form.value.policyId.trim()) {
    ElMessage.warning('请先创建或选择 Policy')
    return
  }
  const currentSavedPolicy = policies.value.find(item => item.policyId === form.value.policyId.trim())
  if (!currentSavedPolicy) {
    ElMessage.warning('请先全局保存一次 Policy')
    return
  }
  
  savingInput.value = true
  try {
    const payload = {
      policyId: currentSavedPolicy.policyId,
      name: currentSavedPolicy.name,
      version: currentSavedPolicy.version,
      skillPipeline: [...(currentSavedPolicy.skillPipeline || [])],
      config: currentSavedPolicy.config || {},
      executionInput: parseRunInput()
    }
    const res = await skillOsApi.registerPolicy(payload)
    policies.value = (res.policies || []).slice().sort((a, b) => a.policyId.localeCompare(b.policyId))
    const current = policies.value.find(item => item.policyId === payload.policyId)
    if (current) {
      form.value.executionInput = { ...(current.executionInput || {}) }
    }
    ElMessage.success('执行输入保存成功')
  } catch (error: any) {
    ElMessage.error(error?.message || '保存执行输入失败')
  } finally {
    savingInput.value = false
  }
}

const parseRunInput = () => {
  if (!runInputText.value || !runInputText.value.trim()) return {}
  let parsed: any = {}
  try {
    parsed = JSON.parse(runInputText.value)
  } catch {
    throw new Error('执行输入必须是合法 JSON')
  }
  return typeof parsed === 'object' && parsed !== null ? parsed : {}
}

const runPolicy = async () => {
  if (!form.value.policyId.trim()) {
    ElMessage.warning('请先选择或保存 Policy')
    return
  }
  executing.value = true
  try {
    const result = await skillOsApi.executePolicy({
      policyId: form.value.policyId.trim(),
      input: parseRunInput()
    })
    executeResult.value = result
    executeResultByPolicy.value[form.value.policyId.trim()] = result
    ElMessage.success(executeResult.value.success ? '执行完成' : '执行结束但存在失败节点')
  } catch (error: any) {
    ElMessage.error(error?.message || '执行失败')
  } finally {
    executing.value = false
  }
}

const loadLatestExecution = async (policyId: string) => {
  try {
    const latest = await skillOsApi.getLatestExecution(policyId)
    if (latest) {
      executeResultByPolicy.value[policyId] = latest
      if (form.value.policyId === policyId) {
        executeResult.value = latest
      }
    }
  } catch (error) {
    console.warn(error)
  }
}

onMounted(async () => {
  await Promise.all([loadPolicies(), loadSkills()])
})

watch(
  () => [route.query.policyId, route.query.mode],
  () => {
    applyRoutePolicySelection()
  }
)

watch(
  () => executeResult.value?.executionId,
  () => {
    selectedTraceIndex.value = 0
  }
)
</script>

<style scoped>
.policy-center-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px;
  background:
    linear-gradient(180deg, rgba(238, 243, 252, 0.95), rgba(231, 238, 250, 0.9)),
    linear-gradient(90deg, rgba(100, 116, 139, 0.08) 1px, transparent 1px),
    linear-gradient(0deg, rgba(100, 116, 139, 0.08) 1px, transparent 1px);
  background-size: auto, 24px 24px, 24px 24px;
  border-radius: 14px;
}

.policy-center-page :deep(.el-row) {
  row-gap: 16px;
}

.header-card,
.panel-card {
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.88);
  border: 1px solid rgba(148, 163, 184, 0.26);
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.04);
}

.panel-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.header-row,
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  margin: 0;
  font-size: 22px;
  color: #1e293b;
}

.subtitle {
  margin-top: 6px;
  color: #64748b;
  font-size: 14px;
}

.flow-card :deep(.el-card__body) {
  padding-top: 8px;
}

.flow-track {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.flow-node-wrap {
  min-width: 168px;
  text-align: center;
  flex-shrink: 0;
}

.flow-node {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  margin: 0 auto 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--node-color);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.16);
}

.flow-node-index {
  color: #ffffff;
  font-weight: 700;
  font-size: 18px;
}

.flow-node-title {
  color: #0f172a;
  font-weight: 700;
  font-size: 18px;
  line-height: 1.2;
}

.flow-node-subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.3;
}

.flow-arrow {
  color: #94a3b8;
  font-size: 26px;
  font-weight: 700;
  margin: 0 2px;
  flex-shrink: 0;
}

.policy-item {
  padding: 10px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 10px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.policy-item.active {
  border-color: #2563eb;
  background: #e8f1ff;
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.2);
}

.policy-title {
  font-weight: 600;
  color: #0f172a;
}

.policy-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.policy-meta {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.full-width {
  width: 100%;
  margin-top: 8px;
}

.add-row {
  display: none;
}

.pipeline-panel :deep(.el-card__body) {
  align-items: center;
}

.pipeline-flow {
  width: min(620px, 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  border-radius: 16px;
  border: 1px solid rgba(147, 197, 253, 0.45);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(241, 247, 255, 0.96));
  box-shadow: 0 24px 48px rgba(30, 64, 175, 0.12);
  padding: 18px 14px;
  animation: policyFadeIn 0.28s ease;
}

.flow-endpoint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #475569;
  font-size: 12px;
}

.flow-endpoint .dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.12);
}

.flow-endpoint.end .dot {
  background: linear-gradient(135deg, #06b6d4, #3b82f6);
}

.flow-side-line {
  width: 2px;
  height: 28px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(37, 99, 235, 0.32), rgba(125, 211, 252, 0.16));
  position: relative;
  overflow: hidden;
}

.flow-side-line::after {
  content: '';
  position: absolute;
  inset: -40% 0;
  background: linear-gradient(180deg, transparent, rgba(96, 165, 250, 0.9), transparent);
  animation: flowPulse 1.8s linear infinite;
}

@keyframes flowPulse {
  from {
    transform: translateY(-100%);
  }
  to {
    transform: translateY(100%);
  }
}

.flow-add-inline {
  border: 1px dashed rgba(96, 165, 250, 0.55);
  color: #1e40af;
  background: rgba(239, 246, 255, 0.94);
  border-radius: 999px;
  padding: 4px 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 12px;
}

.flow-add-inline {
  min-width: 102px;
}

.flow-add-inline:hover {
  border-color: rgba(59, 130, 246, 0.9);
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.16);
  transform: translateY(-1px);
}

.flow-node-row {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.flow-empty-wrap {
  width: 100%;
  padding: 14px 0;
}

.flow-link-block {
  width: min(560px, 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.flow-semantic {
  max-width: 320px;
  border-radius: 999px;
  border: 1px solid rgba(147, 197, 253, 0.7);
  background: rgba(219, 234, 254, 0.72);
  color: #1e3a8a;
  font-size: 12px;
  font-family: Menlo, Monaco, Consolas, 'Courier New', monospace;
  padding: 3px 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: all 0.2s ease;
}

.flow-semantic:hover {
  background: rgba(191, 219, 254, 0.9);
  border-color: rgba(37, 99, 235, 0.85);
}

.flow-gap-placeholder {
  height: 30px;
}

.insert-point {
  position: relative;
}

.insert-point::before {
  content: '';
  position: absolute;
  left: -18px;
  right: -18px;
  top: 50%;
  border-top: 1px dashed rgba(148, 163, 184, 0.45);
  z-index: -1;
}

.add-skill-panel {
  margin-top: 12px;
  max-height: 420px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.add-skill-item {
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.2s ease;
}

.add-skill-item:hover {
  border-color: #6366f1;
  background: #eef2ff;
}

.add-skill-title {
  font-weight: 600;
  color: #0f172a;
}

.add-skill-meta {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.action-row {
  width: 100%;
  margin-top: 6px;
}

.action-row :deep(.el-button) {
  width: 100%;
}

.pipeline-list {
  width: 100%;
  min-height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
}

.pipeline-item {
  width: min(560px, 100%);
  padding: 14px 16px;
  border: 1px solid rgba(191, 219, 254, 0.8);
  border-radius: 14px;
  background: #ffffff;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  box-shadow: 0 10px 24px rgba(30, 41, 59, 0.08);
  cursor: pointer;
}

.pipeline-item.drag-over {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.18), 0 14px 34px rgba(59, 130, 246, 0.12);
}

.pipeline-item:hover {
  transform: translateY(-2px);
  border-color: rgba(59, 130, 246, 0.85);
  box-shadow: 0 16px 36px rgba(37, 99, 235, 0.12), 0 0 0 1px rgba(125, 211, 252, 0.35);
}

.pipeline-item.status-selected {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.24), 0 16px 34px rgba(37, 99, 235, 0.16);
}

.pipeline-item.status-running {
  border-color: rgba(14, 165, 233, 0.95);
  animation: nodeBreath 1.4s ease-in-out infinite;
}

.pipeline-item.status-done {
  border-color: rgba(16, 185, 129, 0.75);
}

.pipeline-item.status-error {
  border-color: rgba(239, 68, 68, 0.85);
  box-shadow: 0 0 0 1px rgba(248, 113, 113, 0.2);
}

.pipeline-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.pipeline-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: #0f172a;
}

.node-bullet {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #38bdf8, #6366f1);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.14);
}

.pipeline-index {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.8);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.pipeline-card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.node-status {
  border-radius: 999px;
  border: 1px solid transparent;
  padding: 2px 8px;
  font-size: 12px;
  line-height: 1.2;
}

.node-status.state-pending {
  color: #64748b;
  background: #f1f5f9;
  border-color: #e2e8f0;
}

.node-status.state-selected {
  color: #1d4ed8;
  background: #dbeafe;
  border-color: #93c5fd;
}

.node-status.state-running {
  color: #0f766e;
  background: #ccfbf1;
  border-color: #5eead4;
}

.node-status.state-done {
  color: #047857;
  background: #dcfce7;
  border-color: #86efac;
}

.node-status.state-error {
  color: #b91c1c;
  background: #fee2e2;
  border-color: #fca5a5;
}

.pipeline-actions {
  opacity: 0;
  transition: opacity 0.2s ease;
  pointer-events: none;
}

.pipeline-item:hover .pipeline-actions {
  opacity: 1;
  pointer-events: auto;
}

.pipeline-card-divider {
  height: 1px;
  background: #e2e8f0;
  margin: 10px 0;
}

.pipeline-readonly-hint {
  margin-bottom: 8px;
  font-size: 12px;
  color: #64748b;
}

.pipeline-io-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.io-block {
  border-radius: 10px;
  border: 1px solid #dbeafe;
  background: #f8fbff;
  padding: 8px;
}

.io-heading {
  font-size: 11px;
  letter-spacing: 0.08em;
  color: #1e3a8a;
  margin-bottom: 8px;
  font-weight: 700;
}

.io-brief {
  color: #334155;
  font-size: 12px;
  line-height: 1.5;
  min-height: 22px;
  word-break: break-all;
}

.io-items {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.io-chip {
  border-radius: 999px;
  border: 1px solid #c7d2fe;
  background: #eef2ff;
  color: #3730a3;
  font-size: 12px;
  padding: 2px 8px;
}

.io-chip.empty {
  border-color: #e2e8f0;
  background: #f8fafc;
  color: #94a3b8;
}

.info-label {
  color: #64748b;
  font-size: 12px;
  min-width: 44px;
}

.expand-line span:last-child {
  flex: 1;
  word-break: break-all;
}

.pipeline-expand {
  margin-top: 8px;
  padding: 10px;
  border-radius: 8px;
  background: rgba(248, 250, 252, 0.85);
}

.expand-line {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  color: #334155;
  font-size: 13px;
}

.expand-line:last-child {
  margin-bottom: 0;
}

.border-perception {
  border-left: 4px solid rgba(59, 130, 246, 0.8);
}

.border-semantic {
  border-left: 4px solid rgba(139, 92, 246, 0.8);
}

.border-guard {
  border-left: 4px solid rgba(249, 115, 22, 0.85);
}

.border-decision {
  border-left: 4px solid rgba(34, 197, 94, 0.8);
}

.border-output {
  border-left: 4px solid rgba(148, 163, 184, 0.8);
}

.tag-perception {
  border-color: #bfdbfe !important;
  background: #eff6ff !important;
  color: #1d4ed8 !important;
}

.tag-semantic {
  border-color: #ddd6fe !important;
  background: #f5f3ff !important;
  color: #6d28d9 !important;
}

.tag-guard {
  border-color: #fed7aa !important;
  background: #fff7ed !important;
  color: #c2410c !important;
}

.tag-decision {
  border-color: #bbf7d0 !important;
  background: #f0fdf4 !important;
  color: #15803d !important;
}

.tag-output {
  border-color: #e5e7eb !important;
  background: #f9fafb !important;
  color: #4b5563 !important;
}

.pipeline-skill {
  flex: 1;
  color: #1e293b;
  min-width: 0;
  word-break: break-all;
}

.state-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.stat-item {
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(248, 250, 252, 0.92);
  border-radius: 10px;
  padding: 10px 12px;
  min-height: 72px;
}

.stat-label {
  color: #64748b;
  font-size: 12px;
}

.stat-value {
  margin-top: 6px;
  color: #0f172a;
  font-size: 15px;
  font-weight: 600;
}

.mono {
  font-family: Menlo, Monaco, Consolas, 'Courier New', monospace;
  font-size: 12px;
  word-break: break-all;
}

.failure-debug {
  margin: 2px 0 4px;
  border: 1px solid #fecaca;
  background: #fff7f7;
  border-radius: 10px;
  padding: 8px;
}

.failure-debug-title {
  margin-bottom: 6px;
  color: #b91c1c;
  font-weight: 700;
  font-size: 13px;
}

.failure-debug-lines {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.failure-line {
  font-size: 12px;
  color: #7f1d1d;
  line-height: 1.4;
  word-break: break-all;
}

.result-toolbar {
  display: flex;
  justify-content: flex-end;
}

.state-tabs :deep(.el-tabs__content) {
  padding-top: 6px;
}

.json-section {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.json-panel {
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(255, 255, 255, 0.92);
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  min-height: 260px;
}

.json-panel.full {
  grid-column: 1 / -1;
  min-height: 200px;
}

.json-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.24);
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

.json-view {
  margin: 0;
  padding: 10px;
  height: 280px;
  overflow: auto;
  white-space: pre;
  font-size: 12px;
  line-height: 1.5;
  color: #1e293b;
  font-family: Menlo, Monaco, Consolas, 'Courier New', monospace;
}

.json-view.wrap {
  white-space: pre-wrap;
  word-break: break-word;
}

.trace-layout {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 10px;
}

.trace-table {
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 10px;
  overflow: hidden;
}

.trace-detail {
  display: grid;
  grid-template-rows: 1fr 1fr;
  gap: 10px;
}

.json-dialog-view {
  margin: 0;
  padding: 12px;
  height: 70vh;
  overflow: auto;
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 10px;
  white-space: pre;
  font-family: Menlo, Monaco, Consolas, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
}

.json-dialog-view.wrap {
  white-space: pre-wrap;
  word-break: break-word;
}

.panel-card :deep(textarea) {
  font-family: Menlo, Monaco, Consolas, 'Courier New', monospace;
}

@media (max-width: 1100px) {
  .state-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .trace-layout {
    grid-template-columns: 1fr;
  }

  .trace-detail {
    grid-template-rows: auto;
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .json-section {
    grid-template-columns: 1fr;
  }

  .stat-item {
    min-height: 66px;
  }
}

@keyframes nodeBreath {
  0% {
    box-shadow: 0 0 0 0 rgba(14, 165, 233, 0.22);
  }
  50% {
    box-shadow: 0 0 0 5px rgba(14, 165, 233, 0.08);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(14, 165, 233, 0.22);
  }
}

@keyframes policyFadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
