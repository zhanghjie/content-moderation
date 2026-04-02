<template>
  <div class="execution-journey-page">
    <el-card shadow="never" class="header-card">
      <div class="header-row">
        <div>
          <h2 class="title">最近一次 Policy 执行日志</h2>
          <p class="subtitle">按最近一次执行记录或指定 Execution ID 展示执行过程、节点结果和最终状态</p>
        </div>
        <el-space>
          <el-button @click="backToPolicies">返回 Policy 列表</el-button>
          <el-button @click="backToExecutePage">去执行 Policy</el-button>
          <el-button :loading="loadingPolicies" @click="loadPolicies">刷新 Policy</el-button>
        </el-space>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="24">
        <el-card shadow="never" class="panel-card control-card">
          <template #header>
            <div class="panel-header">
              <span>执行对象</span>
              <el-tag effect="plain">最近一次执行 / 指定执行均可查看</el-tag>
            </div>
          </template>

          <el-row :gutter="12" align="middle">
            <el-col :xs="24" :sm="10" :md="8">
              <div class="field-label">Policy</div>
              <el-select
                v-model="selectedPolicyId"
                filterable
                class="full-width"
                placeholder="请选择 Policy"
              >
                <el-option
                  v-for="policy in policies"
                  :key="policy.policyId"
                  :label="`${policy.name || policy.policyId} (${policy.policyId})`"
                  :value="policy.policyId"
                />
              </el-select>
            </el-col>
            <el-col :xs="24" :sm="10" :md="8">
              <div class="field-label">Execution ID（可选）</div>
              <el-input v-model="executionIdInput" placeholder="留空则加载最近一次执行" clearable />
            </el-col>
            <el-col :xs="24" :sm="4" :md="8">
              <div class="field-label">操作</div>
              <el-space wrap>
                <el-button type="primary" :loading="loadingExecution" @click="loadExecution">加载</el-button>
                <el-button :disabled="!executionIdInput" @click="loadExecutionDetail">查看指定执行</el-button>
              </el-space>
            </el-col>
          </el-row>

          <el-alert
            v-if="!selectedPolicyId"
            type="info"
            :closable="false"
            show-icon
            title="请先选择一个 Policy，页面会自动读取最近一次执行日志"
            style="margin-top: 14px"
          />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="summary-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="metric-card">
          <div class="metric-label">执行状态</div>
          <div class="metric-value">
            <el-tag :type="executionResult?.success ? 'success' : executionResult ? 'danger' : 'info'" effect="dark">
              {{ executionResult ? (executionResult.success ? 'SUCCESS' : 'FAILED') : '-' }}
            </el-tag>
          </div>
          <div class="metric-hint">最近一次执行结果状态</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="metric-card">
          <div class="metric-label">执行节点数</div>
          <div class="metric-value mono">{{ traceStats.total }}</div>
          <div class="metric-hint">本次记录到的 Trace 数量</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="metric-card">
          <div class="metric-label">最终结果</div>
          <div class="metric-value mono ellipsis">{{ finalResultPreview }}</div>
          <div class="metric-hint">state.finalResult / step 输出聚合结果</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="metric-card">
          <div class="metric-label">State 关键字段</div>
          <div class="metric-value mono">{{ stateKeys.length }}</div>
          <div class="metric-hint">包含 finalResult / step 输出 / stateMapping 写入</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>执行过程解析</span>
              <el-tag effect="plain">按真实 Trace 展示</el-tag>
            </div>
          </template>

          <div v-if="traceRows.length" class="step-list">
            <div v-for="trace in traceRows" :key="trace.traceId || `${trace.stepId}-${trace.skillId}`" class="step-card" :class="{ selected: selectedTrace?.traceId === trace.traceId }" @click="onTraceRowClick(trace)">
              <div class="step-top">
                <div>
                  <div class="step-name">{{ trace.stepName }}</div>
                  <div class="step-sub">{{ trace.stepId || '-' }} · {{ trace.skillId }}</div>
                </div>
                <el-space>
                  <el-tag size="small" effect="plain">Step {{ trace.stepOrder }}</el-tag>
                  <el-tag size="small" :type="trace.skipped ? 'info' : trace.success ? 'success' : 'danger'">
                    {{ trace.skipped ? 'SKIPPED' : trace.success ? 'SUCCESS' : 'FAILED' }}
                  </el-tag>
                </el-space>
              </div>
              <div class="step-body">
                <div class="mini-block">
                  <div class="mini-title">执行状态</div>
                  <div>{{ trace.status || '-' }}</div>
                </div>
                <div class="mini-block">
                  <div class="mini-title">耗时</div>
                  <div>{{ trace.durationMs }} ms</div>
                </div>
                <div class="mini-block full">
                  <div class="mini-title">输出摘要</div>
                  <pre class="json-view">{{ formatJson(trace.output) }}</pre>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无执行记录" :image-size="88" />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>执行结果摘要</span>
              <el-tag v-if="executionResult?.success" type="success" effect="plain">已完成</el-tag>
            </div>
          </template>

          <el-descriptions :column="1" border>
            <el-descriptions-item label="Policy ID">{{ executionResult?.policyId || selectedPolicyId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="Plan ID">{{ executionResult?.planId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="Execution ID">{{ executionResult?.executionId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="Trace 数量">{{ traceStats.total }}</el-descriptions-item>
            <el-descriptions-item label="耗时">{{ executionResult ? `${executionResult.durationMs} ms` : '-' }}</el-descriptions-item>
            <el-descriptions-item label="最终结果">
              <span class="mono">{{ finalResultPreview }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="错误信息">
              <span class="mono">{{ executionResult?.errorMessage || '-' }}</span>
            </el-descriptions-item>
          </el-descriptions>

          <div class="raw-box">
            <div class="raw-title">解析要点</div>
            <div v-if="resultNotes.length" class="decision-list">
              <div v-for="note in resultNotes" :key="note" class="decision-item">
                <div class="decision-desc">{{ note }}</div>
              </div>
            </div>
            <el-empty v-else description="暂无解析要点" :image-size="72" />
          </div>
        </el-card>

        <el-card shadow="never" class="panel-card" style="margin-top: 16px">
          <template #header>
            <div class="panel-header">
              <span>最近一次状态快照</span>
              <el-tag effect="plain">state / plan / traces</el-tag>
            </div>
          </template>
          <div class="persistence-list">
            <div class="persistence-item">
              <div class="persistence-class">Final Result</div>
              <div class="persistence-desc">{{ finalResultPreview }}</div>
            </div>
            <div class="persistence-item">
              <div class="persistence-class">State Keys</div>
              <div class="persistence-desc">{{ stateKeys.join(', ') || '-' }}</div>
            </div>
            <div class="persistence-item">
              <div class="persistence-class">Trace 摘要</div>
              <div class="persistence-desc">成功 {{ traceStats.success }} / 失败 {{ traceStats.failed }} / 跳过 {{ traceStats.skipped }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="panel-header">
          <span>原始数据</span>
          <el-tag effect="plain">Plan / State / Traces</el-tag>
        </div>
      </template>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="Plan" name="plan">
          <pre class="json-view">{{ formatJson(executionResult?.plan || {}) }}</pre>
        </el-tab-pane>
        <el-tab-pane label="State" name="state">
          <pre class="json-view">{{ formatJson(executionResult?.state || {}) }}</pre>
        </el-tab-pane>
        <el-tab-pane label="Traces" name="traces">
          <pre class="json-view">{{ formatJson(executionResult?.traces || []) }}</pre>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { skillOsApi, type PolicyDefinition, type PolicyExecuteRes } from '@/api/skillos'

interface TraceRow {
  traceId?: string
  stepId?: string
  stepOrder: number
  stepName: string
  skillId: string
  status?: string
  success: boolean
  skipped: boolean
  durationMs: number
  input?: any
  output?: any
  message?: string
}

const route = useRoute()
const router = useRouter()
const loadingPolicies = ref(false)
const loadingExecution = ref(false)
const policies = ref<PolicyDefinition[]>([])
const selectedPolicyId = ref('')
const executionIdInput = ref('')
const executionResult = ref<PolicyExecuteRes | null>(null)
const activeTab = ref<'plan' | 'state' | 'traces'>('plan')
const selectedTraceIndex = ref(0)

const selectedPolicy = computed(() => policies.value.find(item => item.policyId === selectedPolicyId.value) || null)
const executionPlan = computed<any | null>(() => executionResult.value?.plan || null)
const planSteps = computed<any[]>(() => executionPlan.value?.steps || [])
const traceRows = computed<TraceRow[]>(() => {
  const traces = executionResult.value?.traces || []
  const stepMap = new Map<string, any>()
  planSteps.value.forEach((step: any, index: number) => {
    if (step?.stepId) {
      stepMap.set(step.stepId, { ...step, stepOrder: step.stepOrder ?? index + 1 })
    }
  })
  return traces.map((trace: any, index: number) => {
    const matchedStep = trace?.stepId ? stepMap.get(trace.stepId) : null
    return {
      ...trace,
      stepOrder: matchedStep?.stepOrder ?? index + 1,
      stepName: matchedStep?.skillSnapshot?.name || trace?.skillId || `Step ${index + 1}`
    }
  })
})
const selectedTrace = computed<TraceRow | null>(() => traceRows.value[selectedTraceIndex.value] || traceRows.value[0] || null)
const stateKeys = computed(() => Object.keys(executionResult.value?.state || {}))
const planTypeLabel = computed(() => String(executionPlan.value?.planType || 'UNKNOWN'))
const traceStats = computed(() => {
  const traces = traceRows.value
  return {
    total: traces.length,
    success: traces.filter(item => item.success).length,
    failed: traces.filter(item => !item.success && !item.skipped).length,
    skipped: traces.filter(item => item.skipped).length
  }
})
const finalResultPreview = computed(() => {
  const state = executionResult.value?.state || {}
  const value = state.finalResult ?? state.result ?? state.analysis ?? state.decision ?? state.summary ?? null
  return value === null || value === undefined ? '-' : typeof value === 'string' ? value : JSON.stringify(value)
})

const resultNotes = computed(() => {
  const notes: string[] = []
  const result = executionResult.value
  if (!result) return notes
  notes.push(`执行 ${result.success ? '成功' : '失败'}，总耗时 ${result.durationMs} ms`)
  notes.push(`Trace 共 ${traceStats.value.total} 条，其中成功 ${traceStats.value.success} 条、失败 ${traceStats.value.failed} 条、跳过 ${traceStats.value.skipped} 条`)
  if (result.executionId) notes.push(`Execution ID: ${result.executionId}`)
  if (result.planId) notes.push(`Plan ID: ${result.planId}`)
  if (stateKeys.value.length) notes.push(`State 关键字段：${stateKeys.value.join(', ')}`)
  if (result.errorMessage) notes.push(`错误信息：${result.errorMessage}`)
  return notes
})

async function loadPolicies() {
  loadingPolicies.value = true
  try {
    const res = await skillOsApi.listPolicies()
    policies.value = (res.policies || []).slice().sort((a, b) => a.policyId.localeCompare(b.policyId))
    if (!selectedPolicyId.value && policies.value.length) {
      selectedPolicyId.value = (route.query.policyId as string) || policies.value[0].policyId
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载 Policy 列表失败')
  } finally {
    loadingPolicies.value = false
  }
}

function onTraceRowClick(trace: TraceRow) {
  const index = traceRows.value.findIndex(item => item.traceId === trace.traceId && item.stepId === trace.stepId)
  if (index >= 0) {
    selectedTraceIndex.value = index
  }
}

async function loadExecution() {
  if (!selectedPolicyId.value) {
    ElMessage.warning('请先选择 Policy')
    return
  }
  loadingExecution.value = true
  try {
    if (executionIdInput.value.trim()) {
      executionResult.value = await skillOsApi.getExecutionDetail(executionIdInput.value.trim())
      ElMessage.success('已加载指定执行记录')
      return
    }
    executionResult.value = await skillOsApi.getLatestExecution(selectedPolicyId.value)
    if (!executionResult.value) {
      ElMessage.info('该 Policy 暂无执行记录')
      return
    }
    executionIdInput.value = executionResult.value.executionId || ''
  } catch (error: any) {
    ElMessage.error(error?.message || '加载执行记录失败')
  } finally {
    loadingExecution.value = false
  }
}

async function loadExecutionDetail() {
  if (!executionIdInput.value.trim()) {
    ElMessage.warning('请输入 Execution ID')
    return
  }
  loadingExecution.value = true
  try {
    executionResult.value = await skillOsApi.getExecutionDetail(executionIdInput.value.trim())
    if (executionResult.value?.policyId && executionResult.value.policyId !== selectedPolicyId.value) {
      selectedPolicyId.value = executionResult.value.policyId
    }
    ElMessage.success('执行详情加载成功')
  } catch (error: any) {
    ElMessage.error(error?.message || '加载执行详情失败')
  } finally {
    loadingExecution.value = false
  }
}

function formatJson(value: any) {
  try {
    return JSON.stringify(value ?? {}, null, 2)
  } catch {
    return String(value)
  }
}

function backToPolicies() {
  router.push('/settings/policies')
}

function backToExecutePage() {
  router.push('/video/new')
}

watch(
  () => selectedPolicyId.value,
  async () => {
    if (!selectedPolicyId.value) return
    executionIdInput.value = ''
    await loadExecution()
  }
)

watch(
  () => executionResult.value,
  () => {
    selectedTraceIndex.value = 0
  }
)

onMounted(async () => {
  const queryPolicyId = typeof route.query.policyId === 'string' ? route.query.policyId : ''
  const queryExecutionId = typeof route.query.executionId === 'string' ? route.query.executionId : ''
  await loadPolicies()
  if (queryPolicyId) {
    selectedPolicyId.value = queryPolicyId
  }
  if (queryExecutionId) {
    executionIdInput.value = queryExecutionId
    await loadExecutionDetail()
  } else if (selectedPolicyId.value) {
    await loadExecution()
  }
})
</script>

<style scoped>
.execution-journey-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px;
}

.header-card,
.panel-card,
.metric-card {
  border-radius: 14px;
}

.header-row,
.panel-header,
.stage-head,
.step-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
}

.subtitle {
  margin: 6px 0 0;
  color: #64748b;
}

.field-label,
.metric-label,
.mini-title,
.raw-title,
.decision-type,
.persistence-class,
.stage-class,
.stage-title {
  font-weight: 700;
}

.field-label,
.metric-label,
.raw-title {
  margin-bottom: 8px;
  color: #334155;
}

.full-width {
  width: 100%;
}

.summary-row {
  margin-top: 2px;
}

.metric-card {
  min-height: 126px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.metric-value {
  font-size: 24px;
  color: #0f172a;
  margin: 8px 0;
}

.metric-hint {
  font-size: 12px;
  color: #64748b;
}

.stage-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  padding: 14px 16px;
}

.stage-card.active {
  border-color: #22c55e;
  box-shadow: 0 8px 24px rgba(34, 197, 94, 0.08);
}

.stage-class {
  font-size: 16px;
  color: #0f172a;
}

.stage-title {
  margin-top: 4px;
  color: #334155;
  font-size: 13px;
}

.stage-desc {
  margin-top: 10px;
  color: #475569;
  line-height: 1.7;
}

.stage-evidence {
  margin-top: 12px;
}

.stage-evidence-title {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
}

.stage-evidence-list,
.decision-list,
.persistence-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.evidence-chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: #eef2ff;
  color: #4338ca;
  font-size: 12px;
  margin-right: 8px;
  margin-bottom: 8px;
}

.step-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.step-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 16px;
  background: #fff;
  cursor: pointer;
}

.step-card.selected {
  border-color: #3b82f6;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.08);
}

.step-name {
  color: #0f172a;
  font-weight: 700;
}

.step-sub,
.persistence-desc,
.decision-desc {
  color: #64748b;
  font-size: 12px;
  margin-top: 4px;
}

.step-body {
  margin-top: 10px;
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.mini-block.full {
  grid-column: 1 / -1;
}

.json-view {
  margin: 0;
  padding: 12px;
  border-radius: 10px;
  background: #0f172a;
  color: #e2e8f0;
  font-size: 12px;
  line-height: 1.6;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  word-break: break-all;
}

.raw-box {
  margin-top: 16px;
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
}

.decision-item,
.persistence-item {
  padding: 12px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #e2e8f0;
}

.decision-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.decision-step {
  font-size: 12px;
  color: #64748b;
}

.persistence-class {
  color: #0f172a;
  margin-bottom: 4px;
}
</style>
