<template>
  <div class="policy-task-new-page">
    <div class="page-header">
      <el-page-header @back="goBack">
        <template #content>
          <span class="page-title">智能治理分析任务创建</span>
        </template>
        <template #extra>
          <el-space>
            <el-button @click="goBack">
              返回列表页
            </el-button>
            <el-button :loading="loadingPolicies" @click="loadPolicies">
              <el-icon><Refresh /></el-icon>
              刷新 Policy
            </el-button>
          </el-space>
        </template>
      </el-page-header>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="24">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>1. 选择 Policy</span>
              <el-tag effect="plain">{{ policies.length }} 个可用 Policy</el-tag>
            </div>
          </template>

          <el-form label-position="top">
            <el-form-item label="Policy">
              <el-select
                v-model="selectedPolicyId"
                filterable
                clearable
                placeholder="请选择要执行的 Policy"
                style="width: 100%"
                :loading="loadingPolicies"
              >
                <el-option
                  v-for="policy in policies"
                  :key="policy.policyId"
                  :label="`${policy.name || policy.policyId}（${policy.policyId}）`"
                  :value="policy.policyId"
                />
              </el-select>
            </el-form-item>
          </el-form>

          <el-alert
            v-if="!selectedPolicy"
            type="info"
            :closable="false"
            show-icon
            title="先选择一个 Policy，再填写它需要的执行输入"
          />

          <template v-else>
            <el-descriptions :column="2" border class="policy-desc">
              <el-descriptions-item label="名称">{{ selectedPolicy.name || selectedPolicy.policyId }}</el-descriptions-item>
              <el-descriptions-item label="Policy ID">{{ selectedPolicy.policyId }}</el-descriptions-item>
              <el-descriptions-item label="版本">{{ selectedPolicy.version || 'v1' }}</el-descriptions-item>
              <el-descriptions-item label="步骤数">{{ selectedPolicy.skillPipeline?.length || 0 }}</el-descriptions-item>
              <el-descriptions-item label="输入项数">{{ inputFields.length }}</el-descriptions-item>
              <el-descriptions-item label="执行状态">{{ latestExecution ? getStatusText(latestExecution.status || '') : '-' }}</el-descriptions-item>
            </el-descriptions>

            <div class="pipeline-wrap">
              <div class="pipeline-title">Policy 执行链路</div>
              <div class="pipeline-flow">
                <span class="flow-node">起点</span>
                <template v-for="(skillId, index) in selectedPolicy.skillPipeline || []" :key="`${skillId}-${index}`">
                  <span class="flow-arrow">→</span>
                  <span class="flow-node skill">{{ getSkillDisplayName(skillId) }}</span>
                </template>
                <span class="flow-arrow">→</span>
                <span class="flow-node">结果</span>
              </div>
            </div>
          </template>
        </el-card>

        <el-card shadow="never" class="panel-card" style="margin-top: 16px">
          <template #header>
            <div class="panel-header">
              <span>2. 填写执行内容</span>
              <el-space>
                <el-tag v-if="selectedPolicy" type="success" effect="plain">按 Policy 模板自动生成</el-tag>
                <el-tag v-if="draftSavedAtText" type="info" effect="plain">草稿已保存：{{ draftSavedAtText }}</el-tag>
                <el-button :disabled="!selectedPolicy" @click="saveDraft">
                  保存草稿
                </el-button>
                <el-button type="primary" :loading="executing" :disabled="!selectedPolicy" @click="runExecution">
                  <el-icon><Promotion /></el-icon>
                  执行并保存结果
                </el-button>
              </el-space>
            </div>
          </template>

          <template v-if="selectedPolicy">
            <div class="meta-panel">
              <div class="meta-panel-header">
                <div>
                  <div class="meta-title">基础任务信息</div>
                  <div class="meta-subtitle">这些参数会作为执行输入的一部分，和 Policy 模板字段一起提交</div>
                </div>
                <el-space>
                  <el-button text @click="openProjectManage">管理项目</el-button>
                  <el-tag effect="plain">{{ projects.length }} 个项目</el-tag>
                </el-space>
              </div>

              <el-row :gutter="12" class="meta-input-grid">
                <el-col :xs="24" :sm="12">
                  <el-form-item label="用户ID" required>
                    <el-input v-model="userIdText" placeholder="请输入用户ID" clearable />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12">
                  <el-form-item label="项目" required>
                    <el-select
                      v-model="selectedProjectId"
                      filterable
                      clearable
                      placeholder="请选择项目"
                      style="width: 100%"
                      :loading="loadingProjects"
                    >
                      <el-option
                        v-for="project in projects"
                        :key="project.projectId"
                        :label="`${project.name}（${project.projectId}）`"
                        :value="project.projectId"
                      />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </div>

            <el-empty v-if="!inputFields.length" description="该 Policy 没有额外执行输入，直接执行即可" :image-size="84" />
            <el-form v-else label-position="top" class="input-form">
              <el-row :gutter="12" class="input-form-grid">
                <el-col
                  v-for="field in inputFields"
                  :key="field.key"
                  :xs="24"
                  :lg="field.kind === 'json' ? 24 : 12"
                >
                  <el-form-item
                    :label="field.label"
                    :required="field.required"
                    class="input-form-item"
                  >
                    <template v-if="field.kind === 'string'">
                      <el-input v-model="inputDraft[field.key]" :placeholder="field.placeholder" clearable />
                    </template>
                    <template v-else-if="field.kind === 'number'">
                      <el-input-number v-model="inputDraft[field.key]" :min="field.min" :step="field.step" style="width: 100%" />
                    </template>
                    <template v-else-if="field.kind === 'boolean'">
                      <el-switch v-model="inputDraft[field.key]" :active-text="field.placeholder || '是'" :inactive-text="'否'" />
                    </template>
                    <template v-else>
                      <el-input
                        v-model="jsonDraft[field.key]"
                        type="textarea"
                        :rows="field.rows"
                        :placeholder="field.placeholder"
                      />
                      <div class="field-hint">
                        复杂结构请使用 JSON 填写，当前默认值：{{ formatJsonValue(field.defaultValue) }}
                      </div>
                    </template>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </template>

          <el-alert
            v-else
            type="warning"
            :closable="false"
            show-icon
            title="请先在上方选择一个 Policy"
          />
        </el-card>

        <el-card shadow="never" class="panel-card" style="margin-top: 16px">
          <template #header>
            <div class="panel-header">
              <span>3. 执行输入预览</span>
              <el-space>
                <el-button text @click="resetDraft" :disabled="!selectedPolicy">重置</el-button>
                <el-button text @click="copyPreview" :disabled="!selectedPolicy">复制 JSON</el-button>
              </el-space>
            </div>
          </template>
          <pre class="json-preview">{{ executionInputPreview }}</pre>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="24">
        <el-card shadow="never" class="panel-card result-card">
          <template #header>
            <div class="panel-header">
              <span>4. 执行结果</span>
              <el-tag v-if="result" :type="result.success ? 'success' : 'danger'" effect="plain">
                {{ result.success ? '执行成功' : '执行失败' }}
              </el-tag>
            </div>
          </template>

          <el-empty v-if="!result" description="执行后会在这里展示结果" :image-size="90" />

          <template v-else>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="Policy">{{ result.policyId }}</el-descriptions-item>
              <el-descriptions-item label="Execution ID">{{ result.executionId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="Plan ID">{{ result.planId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="状态">{{ getStatusText(result.status || '') }}</el-descriptions-item>
              <el-descriptions-item label="耗时">{{ result.durationMs }} ms</el-descriptions-item>
            </el-descriptions>

            <el-alert
              style="margin-top: 12px"
              type="success"
              :closable="false"
              show-icon
              title="结果已保存到执行记录"
            />

            <el-tabs v-model="activeResultTab" class="result-tabs">
              <el-tab-pane v-if="structuredPolicyResult" label="结构化结果" name="structured">
                <div class="structured-sections">
                  <el-card
                    v-for="(section, sectionIndex) in structuredPolicyResult.sections"
                    :key="`section-${sectionIndex}`"
                    shadow="never"
                    class="structured-section-card"
                  >
                    <template #header>
                      <div class="structured-section-title">
                        <span class="section-index">{{ sectionIndex + 1 }}</span>
                        <span>{{ section.title || `章节 ${sectionIndex + 1}` }}</span>
                      </div>
                    </template>

                    <div class="structured-content">
                      <template v-if="isObjectRecord(section.content)">
                        <div
                          v-for="field in toDisplayEntries(section.content)"
                          :key="field.key"
                          class="structured-field"
                        >
                          <div class="structured-field-label">{{ field.label }}</div>

                          <div v-if="isTimelineEntries(field.value)" class="timeline-flow">
                            <div class="timeline-flow-title">对话时间轴</div>
                            <div
                              v-for="(item, itemIndex) in field.value"
                              :key="`timeline-${field.key}-${itemIndex}`"
                              class="timeline-item"
                            >
                              <div class="timeline-time">{{ formatTimelineRange(item.startSec, item.endSec) }}</div>
                              <div class="timeline-dot" />
                              <div class="timeline-body">
                                <span class="timeline-speaker">{{ item.speaker || '未知' }}</span>
                                <span class="timeline-text-inline">{{ item.text || '-' }}</span>
                              </div>
                            </div>
                          </div>

                          <div v-else-if="isObjectRecord(field.value)" class="structured-object-grid">
                            <el-card
                              v-for="subField in toDisplayEntries(field.value)"
                              :key="`${field.key}-${subField.key}`"
                              shadow="never"
                              class="structured-sub-card"
                            >
                              <div class="structured-sub-title">{{ subField.label }}</div>

                              <template v-if="isObjectRecord(subField.value)">
                                <div
                                  v-for="leaf in toDisplayEntries(subField.value)"
                                  :key="`${field.key}-${subField.key}-${leaf.key}`"
                                  class="structured-leaf-row"
                                >
                                  <span class="leaf-label">{{ leaf.label }}</span>
                                  <span class="leaf-value">{{ formatStructuredText(leaf.value) }}</span>
                                </div>
                              </template>
                              <template v-else>
                                <div class="structured-text">{{ formatStructuredText(subField.value) }}</div>
                              </template>
                            </el-card>
                          </div>

                          <ul v-else-if="Array.isArray(field.value)" class="structured-list">
                            <li v-for="(item, itemIndex) in field.value" :key="`${field.key}-${itemIndex}`">
                              {{ formatStructuredText(item) }}
                            </li>
                          </ul>

                          <div v-else class="structured-text emphasis">{{ formatStructuredText(field.value) }}</div>
                        </div>
                      </template>

                      <template v-else>
                        <div class="structured-text">{{ formatStructuredText(section.content) }}</div>
                      </template>
                    </div>
                  </el-card>
                </div>

                <div v-if="dedupedSummary" class="structured-summary bottom">
                  <div class="structured-title">总结</div>
                  <div class="structured-text">{{ dedupedSummary }}</div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="概览" name="overview">
                <div class="stat-grid">
                  <div class="stat-item">
                    <div class="stat-label">成功</div>
                    <div class="stat-value">{{ result.success ? 'YES' : 'NO' }}</div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">节点数</div>
                    <div class="stat-value">{{ result.traces?.length || 0 }}</div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-label">错误信息</div>
                    <div class="stat-value small">{{ result.errorMessage || '-' }}</div>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="状态" name="state">
                <div class="json-block">
                  <div class="json-block-title">State</div>
                  <pre class="json-view">{{ formatJson(result.state) }}</pre>
                </div>
                <div class="json-block">
                  <div class="json-block-title">输入快照</div>
                  <pre class="json-view">{{ formatJson(result.state?.input) }}</pre>
                </div>
              </el-tab-pane>

              <el-tab-pane label="轨迹" name="traces">
                <el-table
                  :data="traceRows"
                  size="small"
                  highlight-current-row
                  @row-click="handleTraceClick"
                  style="width: 100%"
                >
                  <el-table-column label="Skill" min-width="220">
                    <template #default="{ row }">
                      <span>{{ getSkillDisplayName(row.skillId) }}</span>
                      <span v-if="row.skillId && getSkillDisplayName(row.skillId) !== row.skillId" class="skill-id-inline">（{{ row.skillId }}）</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="status" label="状态" width="110" />
                  <el-table-column prop="durationMs" label="耗时(ms)" width="110" />
                </el-table>
                <div class="trace-detail" v-if="selectedTrace">
                  <div class="json-block">
                    <div class="json-block-title">节点输入</div>
                    <pre class="json-view">{{ formatJson(selectedTrace.input) }}</pre>
                  </div>
                  <div class="json-block">
                    <div class="json-block-title">节点输出</div>
                    <pre class="json-view">{{ formatJson(selectedTrace.output) }}</pre>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="原始" name="raw">
                <div class="json-block">
                  <div class="json-block-title">Plan</div>
                  <pre class="json-view">{{ formatJson(result.plan) }}</pre>
                </div>
                <div class="json-block">
                  <div class="json-block-title">Traces</div>
                  <pre class="json-view">{{ formatJson(result.traces) }}</pre>
                </div>
              </el-tab-pane>
            </el-tabs>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <el-button class="floating-back-btn" @click="goBack">
      返回任务列表
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Promotion, Refresh } from '@element-plus/icons-vue'
import { skillOsApi, type PolicyDefinition, type PolicyExecuteRes, type SkillDefinition } from '@/api/skillos'
import { projectApi, type ProjectItem } from '@/api/project'
import { videoApi } from '@/api/video'

const router = useRouter()
const route = useRoute()

const loadingPolicies = ref(false)
const loadingSkills = ref(false)
const loadingProjects = ref(false)
const executing = ref(false)
const policies = ref<PolicyDefinition[]>([])
const skills = ref<SkillDefinition[]>([])
const projects = ref<ProjectItem[]>([])
const selectedPolicyId = ref('')
const userIdText = ref('')
const selectedProjectId = ref('')
const result = ref<PolicyExecuteRes | null>(null)
const latestExecution = computed(() => result.value)
const activeResultTab = ref<'structured' | 'overview' | 'state' | 'traces' | 'raw'>('overview')
const selectedTraceIndex = ref(0)

const inputDraft = reactive<Record<string, any>>({})
const jsonDraft = reactive<Record<string, string>>({})
const reservedInputKeys = new Set(['userId', 'projectId', 'projectName'])
const draftSavedAtText = ref('')
const activeDraftId = ref('')
const hydratingDraft = ref(false)

const selectedPolicy = computed(() => {
  return policies.value.find(item => item.policyId === selectedPolicyId.value) || null
})

const skillNameMap = computed(() => {
  const map = new Map<string, string>()
  skills.value.forEach(item => {
    map.set(item.skillId, item.name || item.skillId)
  })
  return map
})

const inputFields = computed(() => {
  const template = selectedPolicy.value?.executionInput
  if (!template || typeof template !== 'object' || Array.isArray(template)) {
    return [] as Array<{
      key: string
      label: string
      kind: 'string' | 'number' | 'boolean' | 'json'
      required: boolean
      placeholder: string
      rows: number
      defaultValue: any
      min?: number
      step?: number
    }>
  }

  return Object.entries(template)
    .filter(([key]) => !reservedInputKeys.has(key))
    .map(([key, value]) => {
      const kind = inferKind(value)
      return {
        key,
        label: prettifyKey(key),
        kind,
        required: value !== null && value !== undefined,
        placeholder: getPlaceholder(kind, key),
        rows: kind === 'json' ? 5 : 3,
        defaultValue: value,
        min: kind === 'number' ? 0 : undefined,
        step: kind === 'number' ? 1 : undefined
      }
    })
})

const traceRows = computed(() => result.value?.traces || [])
const selectedTrace = computed(() => traceRows.value[selectedTraceIndex.value] || null)
const structuredPolicyResult = computed(() => extractStructuredPolicyResult(result.value?.state))
const dedupedSummary = computed(() => buildDedupedSummary(structuredPolicyResult.value))

const executionInputPreview = computed(() => {
  if (!selectedPolicy.value) return '请先选择 Policy'
  try {
    return formatJson(buildExecutionInput())
  } catch (error: any) {
    return `输入尚未准备完成：${error?.message || '请检查 JSON 字段'}`
  }
})

function getSkillDisplayName(skillId: string) {
  if (!skillId) return ''
  return skillNameMap.value.get(skillId) || skillId
}

function prettifyKey(key: string) {
  return key
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/^./, char => char.toUpperCase())
}

function inferKind(value: any): 'string' | 'number' | 'boolean' | 'json' {
  if (typeof value === 'boolean') return 'boolean'
  if (typeof value === 'number') return 'number'
  if (typeof value === 'string') return 'string'
  return 'json'
}

function getPlaceholder(kind: 'string' | 'number' | 'boolean' | 'json', key: string) {
  if (kind === 'string') return `请输入 ${prettifyKey(key)}`
  if (kind === 'number') return `请输入 ${prettifyKey(key)} 数值`
  if (kind === 'boolean') return '切换开关'
  return '请输入 JSON 内容'
}

function cloneValue(value: any) {
  if (value === null || value === undefined) return ''
  if (typeof value === 'object') return JSON.parse(JSON.stringify(value))
  return value
}

function replaceReactiveRecord(target: Record<string, any>, source: Record<string, any>) {
  Object.keys(target).forEach(key => delete target[key])
  Object.assign(target, source)
}

function syncDraftWithPolicy(policy: PolicyDefinition | null) {
  replaceReactiveRecord(inputDraft, {})
  replaceReactiveRecord(jsonDraft, {})
  selectedTraceIndex.value = 0
  activeDraftId.value = ''
  draftSavedAtText.value = ''
  userIdText.value = ''
  result.value = null

  if (!policy) {
    return
  }

  const template = policy.executionInput && typeof policy.executionInput === 'object' && !Array.isArray(policy.executionInput)
    ? policy.executionInput
    : {}

  for (const [key, value] of Object.entries(template)) {
    const kind = inferKind(value)
    if (kind === 'json') {
      jsonDraft[key] = formatJson(value)
    } else {
      inputDraft[key] = cloneValue(value)
    }
  }
}

async function loadSkills() {
  loadingSkills.value = true
  try {
    const res = await skillOsApi.listSkills()
    skills.value = (res.skills || []).slice().sort((a, b) => a.skillId.localeCompare(b.skillId))
  } finally {
    loadingSkills.value = false
  }
}

async function loadProjectOptions() {
  loadingProjects.value = true
  try {
    const res = await projectApi.listProjects()
    projects.value = res.projects || []
    if (!selectedProjectId.value && projects.value.length) {
      selectedProjectId.value = projects.value[0].projectId
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载项目列表失败')
  } finally {
    loadingProjects.value = false
  }
}

function buildExecutionInput() {
  const userId = String(userIdText.value || '').trim()
  if (!userId) {
    throw new Error('请输入用户ID')
  }
  const project = projects.value.find(item => item.projectId === selectedProjectId.value)
  if (!project) {
    throw new Error('请选择项目')
  }

  const payload: Record<string, any> = {}
  payload.userId = userId
  payload.projectId = project.projectId
  payload.projectName = project.name

  for (const field of inputFields.value) {
    if (field.kind === 'json') {
      const raw = String(jsonDraft[field.key] || '').trim()
      if (!raw) {
        payload[field.key] = cloneValue(field.defaultValue)
        continue
      }
      payload[field.key] = JSON.parse(raw)
      continue
    }

    const value = inputDraft[field.key]
    if (field.kind === 'number') {
      payload[field.key] = value === '' || value === null || value === undefined ? value : Number(value)
      continue
    }
    if (field.kind === 'boolean') {
      payload[field.key] = Boolean(value)
      continue
    }
    payload[field.key] = value
  }
  return payload
}

function formatJson(value: any) {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'string') return value
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

function formatStructuredText(value: any) {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'string') return value.trim() || '-'
  return formatJson(value)
}

function toDisplayEntries(value: Record<string, any>) {
  return Object.entries(value)
    .filter(([key]) => !shouldHideStructuredField(key))
    .map(([key, itemValue]) => ({
      key,
      label: prettifyKey(key),
      value: itemValue
    }))
}

function shouldHideStructuredField(key: string) {
  return false
}

function isTimelineEntries(value: any): value is Array<{ speaker?: string; startSec?: number; endSec?: number; text?: string }> {
  if (!Array.isArray(value) || value.length === 0) return false
  return value.every(item => {
    if (!isObjectRecord(item)) return false
    return Object.prototype.hasOwnProperty.call(item, 'startSec')
      && Object.prototype.hasOwnProperty.call(item, 'endSec')
      && Object.prototype.hasOwnProperty.call(item, 'text')
  })
}

function formatSec(value: any) {
  const num = Number(value)
  if (!Number.isFinite(num)) return '-'
  return String(Math.round(num * 100) / 100)
}

function formatTimelineRange(startSec: any, endSec: any) {
  const start = Number(startSec)
  const end = Number(endSec)
  if (!Number.isFinite(start) || !Number.isFinite(end)) return '未知时间'
  return `${formatClockTime(start)} - ${formatClockTime(end)}`
}

function formatClockTime(sec: number) {
  const total = Math.max(0, Math.floor(sec))
  const min = Math.floor(total / 60)
  const second = total % 60
  return `${String(min).padStart(2, '0')}:${String(second).padStart(2, '0')}`
}

function buildDedupedSummary(payload: { sections: Array<{ title?: string; content?: any }>; summary: any } | null) {
  if (!payload) return ''
  const globalSummary = normalizeSummaryText(payload.summary)
  const sectionSummaryList = payload.sections
    .map(section => normalizeSummaryText(extractSectionSummary(section.content)))
    .filter(Boolean)
  if (!globalSummary) return sectionSummaryList[0] || ''
  if (sectionSummaryList.includes(globalSummary)) return ''
  return globalSummary
}

function extractSectionSummary(content: any) {
  if (!isObjectRecord(content)) return null
  for (const [key, value] of Object.entries(content)) {
    const normalizedKey = String(key || '').toLowerCase()
    if (normalizedKey.includes('总结') || normalizedKey.includes('summary')) {
      return value
    }
  }
  return null
}

function normalizeSummaryText(value: any) {
  if (typeof value === 'string') {
    return value.trim()
  }
  if (value === null || value === undefined) return ''
  return String(value).trim()
}

function extractStructuredPolicyResult(state: any): { sections: Array<{ title?: string; content?: any }>; summary: any } | null {
  const candidates = collectStructuredCandidates(state)
  for (const candidate of candidates) {
    if (!isStructuredPolicyPayload(candidate)) continue
    return {
      sections: Array.isArray(candidate.sections) ? candidate.sections : [],
      summary: candidate.summary
    }
  }
  return null
}

function collectStructuredCandidates(state: any) {
  const candidates: any[] = []
  if (!isObjectRecord(state)) {
    return candidates
  }
  candidates.push(state)
  candidates.push(state.finalResult)
  candidates.push(state.output)
  candidates.push(state.result)
  candidates.push(state.analysisResult)

  for (const value of Object.values(state)) {
    candidates.push(value)
    if (isObjectRecord(value)) {
      candidates.push(value.output)
      candidates.push(value.result)
      candidates.push(value.finalResult)
      candidates.push(value.analysisResult)
    }
  }
  return candidates.map(tryParseJsonObject).filter(Boolean)
}

function tryParseJsonObject(value: any) {
  if (isObjectRecord(value)) return value
  if (typeof value !== 'string') return null
  const trimmed = value.trim()
  if (!trimmed.startsWith('{') || !trimmed.endsWith('}')) return null
  try {
    const parsed = JSON.parse(trimmed)
    return isObjectRecord(parsed) ? parsed : null
  } catch {
    return null
  }
}

function isObjectRecord(value: any): value is Record<string, any> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isStructuredPolicyPayload(value: any): value is { sections: any[]; summary: any } {
  if (!isObjectRecord(value)) return false
  if (!Array.isArray(value.sections)) return false
  return Object.prototype.hasOwnProperty.call(value, 'summary')
}

function formatJsonValue(value: any) {
  if (value === null || value === undefined || value === '') return '-'
  return formatJson(value)
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    PENDING: '待处理',
    PROCESSING: '处理中',
    COMPLETED: '已完成',
    FAILED: '失败',
    SUCCEEDED: '成功',
    FAILED_OUTPUT_BINDING: '输出绑定失败',
    RUNNING: '运行中'
  }
  return map[String(status || '').toUpperCase()] || status || '-'
}

function handleTraceClick(row: any) {
  const index = traceRows.value.findIndex(item => item === row)
  if (index >= 0) {
    selectedTraceIndex.value = index
  }
}

async function loadPolicies() {
  loadingPolicies.value = true
  try {
    const res = await skillOsApi.listPolicies()
    policies.value = (res.policies || []).slice().sort((a, b) => a.policyId.localeCompare(b.policyId))

    const routePolicyId = String(route.query.policyId || '').trim()
    const targetPolicyId = routePolicyId || selectedPolicyId.value || policies.value[0]?.policyId || ''
    selectedPolicyId.value = targetPolicyId
  } finally {
    loadingPolicies.value = false
  }
}

function resetDraft() {
  syncDraftWithPolicy(selectedPolicy.value)
}

async function saveDraft() {
  await persistDraft(true)
}

async function persistDraft(showToast: boolean) {
  if (!selectedPolicy.value) {
    ElMessage.warning('请先选择 Policy')
    return ''
  }
  const validationError = validateDraftBeforeSave()
  if (validationError) {
    ElMessage.warning(validationError)
    return ''
  }
  try {
    const payload = buildExecutionInput()
    const res = await videoApi.saveDraft({
      taskId: activeDraftId.value || undefined,
      policyId: selectedPolicy.value.policyId,
      policyInput: payload,
      analysisType: 'STANDARD'
    })
    activeDraftId.value = res.taskId
    draftSavedAtText.value = formatDraftTimeFromIso(res.updatedAt || res.createdAt)
    if (showToast) {
      ElMessage.success('草稿已保存到服务端')
    }
    return res.taskId || ''
  } catch (error: any) {
    ElMessage.error(error?.message || '草稿保存失败')
    return ''
  }
}

async function hydrateDraftFromServer(draftId: string) {
  if (!draftId) return
  try {
    const draft = await videoApi.getDraft(draftId)
    if (!draft || !draft.policyId) {
      ElMessage.warning('草稿不存在或已失效')
      return
    }
    hydratingDraft.value = true
    selectedPolicyId.value = String(draft.policyId)
    await nextTick()
    syncDraftWithPolicy(selectedPolicy.value)

    const payload = isObjectRecord(draft.policyInput) ? draft.policyInput : {}
    if (typeof payload.userId === 'string' || typeof payload.userId === 'number') {
      userIdText.value = String(payload.userId)
    }
    if (typeof payload.projectId === 'string') {
      selectedProjectId.value = payload.projectId
    }
    for (const field of inputFields.value) {
      if (!Object.prototype.hasOwnProperty.call(payload, field.key)) continue
      const value = payload[field.key]
      if (field.kind === 'json') {
        jsonDraft[field.key] = formatJson(value)
      } else {
        inputDraft[field.key] = cloneValue(value)
      }
    }

    activeDraftId.value = draft.taskId
    draftSavedAtText.value = formatDraftTimeFromIso(draft.updatedAt || draft.createdAt)
  } catch (error: any) {
    draftSavedAtText.value = ''
    ElMessage.error(error?.message || '加载草稿失败')
  } finally {
    hydratingDraft.value = false
  }
}

function validateDraftBeforeSave() {
  const userId = String(userIdText.value || '').trim()
  if (!userId) {
    return '请输入用户ID'
  }
  const project = projects.value.find(item => item.projectId === selectedProjectId.value)
  if (!project) {
    return '请选择项目'
  }

  for (const field of inputFields.value) {
    if (field.kind === 'json') {
      const raw = String(jsonDraft[field.key] || '').trim()
      if (field.required && !raw) {
        return `请填写 ${field.label}`
      }
      if (raw) {
        try {
          JSON.parse(raw)
        } catch {
          return `${field.label} 不是合法 JSON`
        }
      }
      continue
    }

    const value = inputDraft[field.key]
    if (!field.required) continue
    if (field.kind === 'string' && String(value ?? '').trim() === '') {
      return `请填写 ${field.label}`
    }
    if (field.kind === 'number' && (value === '' || value === null || value === undefined || Number.isNaN(Number(value)))) {
      return `请填写 ${field.label}`
    }
  }
  return ''
}

function formatDraftTime(timestamp: number) {
  const date = new Date(timestamp)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
}

function formatDraftTimeFromIso(value?: string) {
  if (!value) return ''
  const time = new Date(value).getTime()
  if (!Number.isFinite(time)) return ''
  return formatDraftTime(time)
}

function openProjectManage() {
  router.push('/settings/projects')
}

async function runExecution() {
  if (!selectedPolicyId.value) {
    ElMessage.warning('请先选择 Policy')
    return
  }
  executing.value = true
  try {
    const draftId = activeDraftId.value || await persistDraft(false)
    if (!draftId) {
      throw new Error('请先保存草稿后再执行')
    }
    const executeRes = await videoApi.executeDraft(draftId)
    ElMessage.success('执行完成，草稿状态已更新')
    if (executeRes?.callId) {
      router.push(`/video/${executeRes.callId}`)
    } else {
      router.push('/video/tasks')
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '执行失败')
  } finally {
    executing.value = false
  }
}

async function copyPreview() {
  try {
    await navigator.clipboard.writeText(executionInputPreview.value)
    ElMessage.success('已复制输入预览')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

function goBack() {
  router.push('/video/tasks')
}

watch(
  () => selectedPolicyId.value,
  () => {
    if (hydratingDraft.value) return
    syncDraftWithPolicy(selectedPolicy.value)
  }
)

onMounted(async () => {
  await Promise.all([loadPolicies(), loadSkills(), loadProjectOptions()])
  const routeDraftId = String(route.query.draftId || '').trim()
  if (routeDraftId) {
    await hydrateDraftFromServer(routeDraftId)
    return
  }
  syncDraftWithPolicy(selectedPolicy.value)
})
</script>

<style scoped>
.policy-task-new-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header,
.panel-card {
  border-radius: 14px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.meta-panel {
  margin-bottom: 16px;
  padding: 14px;
  border-radius: 12px;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
  border: 1px solid #e0ecff;
}

.meta-input-grid {
  margin-top: 4px;
}

.meta-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.meta-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.meta-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.policy-desc {
  margin-top: 12px;
}

.pipeline-wrap {
  margin-top: 16px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #f8fbff;
  border: 1px dashed #bfdbfe;
}

.pipeline-title {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;
}

.pipeline-flow {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.input-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.input-form-grid {
  margin-top: 4px;
}

.input-form-item {
  margin-bottom: 0;
}

.input-form :deep(.el-form-item__label),
.meta-panel :deep(.el-form-item__label) {
  font-weight: 600;
}

.flow-node {
  padding: 4px 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
  font-size: 12px;
}

.flow-node.skill {
  background: #ffffff;
  color: #0f172a;
}

.flow-arrow {
  color: #94a3b8;
}

.input-form {
  margin-top: 6px;
}

.field-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}

.json-preview,
.json-view {
  margin: 0;
  padding: 12px;
  border-radius: 12px;
  background: #0f172a;
  color: #e2e8f0;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  line-height: 1.6;
  min-height: 140px;
}

.result-card {
  position: sticky;
  top: 16px;
}

.result-tabs {
  margin-top: 12px;
}

.structured-summary {
  margin-top: 4px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
}

.structured-summary.bottom {
  margin-top: 14px;
}

.structured-title {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.structured-text {
  margin-top: 8px;
  font-size: 13px;
  color: #0f172a;
  white-space: pre-wrap;
  line-height: 1.7;
}

.structured-sections {
  margin-top: 8px;
  display: grid;
  gap: 14px;
}

.structured-section-card {
  border-radius: 14px;
  border: 1px solid #dbeafe;
  background: #ffffff;
}

.structured-section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.section-index {
  width: 22px;
  height: 22px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #1d4ed8;
  background: #dbeafe;
}

.structured-content {
  display: grid;
  gap: 12px;
}

.structured-field {
  display: grid;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

.structured-field-label {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.timeline-table :deep(.cell) {
  white-space: pre-wrap;
  word-break: keep-all;
  line-break: auto;
  line-height: 1.65;
}

.timeline-flow {
  margin-top: 8px;
  padding: 8px 10px;
  border-radius: 10px;
  border: 1px dashed #cbd5e1;
  background: #ffffff;
  display: grid;
  gap: 8px;
}

.timeline-flow-title {
  font-size: 12px;
  font-weight: 700;
  color: #475569;
}

.timeline-item {
  display: grid;
  grid-template-columns: 90px 10px 1fr;
  gap: 8px;
  align-items: start;
}

.timeline-time {
  font-size: 12px;
  color: #334155;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
}

.timeline-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #3b82f6;
  margin-top: 5px;
}

.timeline-body {
  display: flex;
  gap: 8px;
  align-items: baseline;
  min-width: 0;
}

.timeline-speaker {
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  flex: 0 0 auto;
}

.timeline-text-inline {
  color: #0f172a;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: keep-all;
}

.timeline-text {
  white-space: pre-wrap;
  word-break: keep-all;
  line-height: 1.65;
}

.structured-object-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 10px;
}

.structured-sub-card {
  border-radius: 10px;
  border-color: #e2e8f0;
  background: #ffffff;
}

.structured-sub-title {
  font-size: 12px;
  font-weight: 700;
  color: #475569;
  margin-bottom: 8px;
}

.structured-leaf-row {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 8px;
  padding: 4px 0;
}

.leaf-label {
  font-size: 12px;
  color: #64748b;
}

.leaf-value {
  font-size: 13px;
  color: #0f172a;
  white-space: pre-wrap;
  word-break: keep-all;
}

.structured-list {
  margin: 0;
  padding-left: 18px;
  color: #0f172a;
  display: grid;
  gap: 6px;
}

.structured-text.emphasis {
  padding: 10px 12px;
  border-radius: 10px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.stat-item {
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
}

.stat-value {
  margin-top: 6px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.stat-value.small {
  font-size: 13px;
  font-weight: 500;
}

.json-block {
  margin-top: 12px;
}

.json-block-title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.trace-detail {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.skill-id-inline {
  margin-left: 6px;
  color: #94a3b8;
  font-size: 12px;
}

.floating-back-btn {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 40;
  border-radius: 999px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.16);
}

@media (max-width: 992px) {
  .result-card {
    position: static;
    top: auto;
    margin-top: 16px;
  }

  .stat-grid {
    grid-template-columns: 1fr;
  }

  .meta-panel-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .floating-back-btn {
    right: 12px;
    bottom: 12px;
  }
}
</style>
