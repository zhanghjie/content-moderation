<template>
  <div class="execution-journey-page">
    <el-card shadow="never" class="header-card">
      <div class="header-row">
        <div>
          <h2 class="title">Policy 执行链路说明</h2>
          <p class="subtitle">展示你点击执行 Policy 之后，系统真实经历的类、方法和中文职责说明</p>
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
            title="请先选择一个 Policy，页面会自动读取最近一次执行结果"
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
          <div class="metric-hint">前端最终看到的执行结果状态</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="metric-card">
          <div class="metric-label">Plan 类型</div>
          <div class="metric-value mono">{{ planTypeLabel }}</div>
          <div class="metric-hint">STATIC / DYNAMIC / REPLAN</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="metric-card">
          <div class="metric-label">执行节点数</div>
          <div class="metric-value mono">{{ executionResult?.traces?.length || 0 }}</div>
          <div class="metric-hint">DefaultPlanExecutor 逐个执行的 Trace 数量</div>
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

    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="panel-header">
          <span>真实经历过的类与方法</span>
          <el-tag v-if="executionResult?.executionId" effect="plain">Execution {{ executionResult.executionId }}</el-tag>
        </div>
      </template>

      <el-timeline>
        <el-timeline-item
          v-for="stage in journeyStages"
          :key="stage.key"
          :type="stage.happened ? 'success' : 'info'"
          :timestamp="stage.methodName"
          placement="top"
        >
          <div class="stage-card" :class="{ active: stage.happened }">
            <div class="stage-head">
              <div>
                <div class="stage-class">{{ stage.className }}</div>
                <div class="stage-title">{{ stage.title }}</div>
              </div>
              <el-tag :type="stage.happened ? 'success' : 'info'" effect="plain">
                {{ stage.happened ? '本次发生' : '本次未触发' }}
              </el-tag>
            </div>
            <div class="stage-desc">{{ stage.description }}</div>
            <div v-if="stage.evidence.length" class="stage-evidence">
              <div class="stage-evidence-title">本次证据</div>
              <div class="stage-evidence-list">
                <span v-for="item in stage.evidence" :key="item" class="evidence-chip">{{ item }}</span>
              </div>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>执行顺序与轨迹</span>
              <el-tag effect="plain">按真实 Trace 展示</el-tag>
            </div>
          </template>

          <div v-if="planSteps.length" class="step-list">
            <div v-for="step in planSteps" :key="step.stepId" class="step-card">
              <div class="step-top">
                <div>
                  <div class="step-name">{{ step.skillSnapshot?.name || step.skillId }}</div>
                  <div class="step-sub">{{ step.skillId }} · {{ step.skillSnapshot?.version || 'v1' }}</div>
                </div>
                <el-tag size="small" effect="plain">Step {{ step.stepOrder }}</el-tag>
              </div>
              <div class="step-body">
                <div class="mini-block">
                  <div class="mini-title">对应类</div>
                  <div class="mono">DefaultPlanExecutor</div>
                </div>
                <div class="mini-block">
                  <div class="mini-title">执行结果</div>
                  <div>{{ getTraceByStepId(step.stepId)?.status || '未找到 Trace' }}</div>
                </div>
                <div class="mini-block full">
                  <div class="mini-title">输出 / State</div>
                  <pre class="json-view">{{ formatJson(getTraceByStepId(step.stepId)?.output || executionResult?.state?.[step.skillId] || null) }}</pre>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无执行步骤" :image-size="88" />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>本次执行摘要</span>
              <el-tag v-if="executionResult?.success" type="success" effect="plain">已完成</el-tag>
            </div>
          </template>

          <el-descriptions :column="1" border>
            <el-descriptions-item label="Policy ID">{{ executionResult?.policyId || selectedPolicyId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="Plan ID">{{ executionResult?.planId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="Execution ID">{{ executionResult?.executionId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="耗时">{{ executionResult ? `${executionResult.durationMs} ms` : '-' }}</el-descriptions-item>
            <el-descriptions-item label="最终结果">
              <span class="mono">{{ finalResultPreview }}</span>
            </el-descriptions-item>
          </el-descriptions>

          <div class="raw-box">
            <div class="raw-title">Planner 决策记录</div>
            <div v-if="plannerDecisions.length" class="decision-list">
              <div v-for="(decision, index) in plannerDecisions" :key="`${decision.type}-${index}`" class="decision-item">
                <div class="decision-head">
                  <span class="decision-type">{{ decision.type }}</span>
                  <span class="decision-step">{{ decision.stepId || '全局' }}</span>
                </div>
                <div class="decision-desc">{{ decision.reason || '无说明' }}</div>
              </div>
            </div>
            <el-empty v-else description="暂无 planner 决策" :image-size="72" />
          </div>
        </el-card>

        <el-card shadow="never" class="panel-card" style="margin-top: 16px">
          <template #header>
            <div class="panel-header">
              <span>持久化与反馈</span>
              <el-tag effect="plain">PolicyExecutionMapper / StepMapper / FeedbackMapper</el-tag>
            </div>
          </template>
          <div class="persistence-list">
            <div class="persistence-item">
              <div class="persistence-class">PolicyExecutionMapper</div>
              <div class="persistence-desc">保存执行头信息、Plan 快照和全量 State，便于后续回放与查询。</div>
            </div>
            <div class="persistence-item">
              <div class="persistence-class">PolicyExecutionStepMapper</div>
              <div class="persistence-desc">保存每一步 Trace，记录输入、输出、状态和错误信息。</div>
            </div>
            <div class="persistence-item">
              <div class="persistence-class">PolicyExecutionFeedbackMapper</div>
              <div class="persistence-desc">保存自动反馈或人工反馈，用于后续质量评估。</div>
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

interface JourneyStage {
  key: string
  className: string
  methodName: string
  title: string
  description: string
  happened: boolean
  evidence: string[]
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

const selectedPolicy = computed(() => policies.value.find(item => item.policyId === selectedPolicyId.value) || null)
const executionPlan = computed<any | null>(() => executionResult.value?.plan || null)
const planSteps = computed<any[]>(() => executionPlan.value?.steps || [])
const plannerDecisions = computed<any[]>(() => executionPlan.value?.plannerDecisions || [])
const stateKeys = computed(() => Object.keys(executionResult.value?.state || {}))
const planTypeLabel = computed(() => String(executionPlan.value?.planType || 'UNKNOWN'))
const finalResultPreview = computed(() => {
  const state = executionResult.value?.state || {}
  const value = state.finalResult ?? state.result ?? state.analysis ?? state.decision ?? state.summary ?? null
  return value === null || value === undefined ? '-' : typeof value === 'string' ? value : JSON.stringify(value)
})

const journeyStages = computed<JourneyStage[]>(() => {
  const result = executionResult.value
  const plan = executionPlan.value || {}
  const traces = result?.traces || []
  const planType = String(plan?.planType || '')
  const hasPlan = Boolean(plan && Object.keys(plan).length)
  const hasState = Boolean(result?.state && Object.keys(result.state).length)
  const hasReplan = planType === 'REPLAN'
  const hasDynamic = planType === 'DYNAMIC' || hasReplan

  return [
    {
      key: 'engine',
      className: 'PolicyExecutionEngine',
      methodName: 'execute(policyId, input)',
      title: '执行入口',
      description: '读取 Policy、整理输入、调用 Planner 生成计划，再交给 Executor 执行，最后持久化并返回前端结果。',
      happened: Boolean(result),
      evidence: result?.executionId ? [`executionId=${result.executionId}`] : []
    },
    {
      key: 'planner',
      className: 'DefaultPolicyPlanner',
      methodName: 'plan(policy, input)',
      title: '规划门面',
      description: '作为三层规划的统一入口，先走静态编译，再走受限动态补全，输出最终 ExecutionPlan。',
      happened: hasPlan,
      evidence: hasPlan ? [`planId=${plan.planId || '-'}`, `planType=${planType || '-'}`] : []
    },
    {
      key: 'static',
      className: 'DefaultStaticPolicyPlanner',
      methodName: 'plan(policy, input)',
      title: '静态编译层',
      description: '把 PolicyDefinition 里的 skillPipeline 编译成基础 ExecutionPlan，明确每个步骤、依赖和技能快照。',
      happened: hasPlan,
      evidence: plan?.steps?.length ? [`steps=${plan.steps.length}`] : []
    },
    {
      key: 'dynamic',
      className: 'DefaultDynamicPolicyPlanner',
      methodName: 'refine(plan, policy, input)',
      title: '受限动态补全层',
      description: '在不破坏核心结构的前提下，依据配置和候选技能对计划做约束内调整。',
      happened: hasDynamic,
      evidence: plannerDecisions.value.length ? [`plannerDecisions=${plannerDecisions.value.length}`] : (hasDynamic ? [`planType=${planType}`] : [])
    },
    {
      key: 'executor',
      className: 'DefaultPlanExecutor',
      methodName: 'execute(plan, policy, input)',
      title: '技能执行层',
      description: '逐步执行每个 Skill，收集 Trace，并把输出写入 ExecutionState，同时补充 finalResult。',
      happened: traces.length > 0,
      evidence: traces.length ? [`traces=${traces.length}`] : []
    },
    {
      key: 'replanner',
      className: 'DefaultReplanner',
      methodName: 'replan(plan, policy, input, state, failedStep, failedResult)',
      title: '运行时重规划层',
      description: '当首次执行失败时，根据失败步骤和失败结果生成新计划，再执行一次。',
      happened: hasReplan,
      evidence: hasReplan ? [`planType=REPLAN`, `steps=${plan?.steps?.length || 0}`] : ['本次未触发重规划']
    },
    {
      key: 'persistence',
      className: 'PolicyExecutionMapper / PolicyExecutionStepMapper / PolicyExecutionFeedbackMapper',
      methodName: 'insert(...)',
      title: '结果落库层',
      description: '把执行头、每一步 Trace 和自动反馈写入数据库，支持回放、列表和质量分析。',
      happened: Boolean(result?.executionId),
      evidence: result?.executionId ? [`executionId=${result.executionId}`, `stateKeys=${stateKeys.value.length}`] : []
    },
    {
      key: 'response',
      className: 'PolicyExecuteRes',
      methodName: 'builder() / return result',
      title: '前端消费对象',
      description: '最终返回给页面的结果对象，前端主要从 state、traces、plan 中取展示数据。',
      happened: Boolean(result),
      evidence: hasState ? [`stateKeys=${stateKeys.value.length}`] : []
    }
  ]
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

function getTraceByStepId(stepId: string) {
  return (executionResult.value?.traces || []).find(trace => trace?.stepId === stepId) || null
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
