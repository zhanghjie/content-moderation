<template>
  <div class="task-overview-page">
    <el-card shadow="never" class="panel-card">
      <div class="overview-header">
        <div>
          <h2 class="title">AI 审核执行流</h2>
          <p class="subtitle">以任务维度追踪审核执行状态、结论与违规证据</p>
        </div>
        <el-space>
          <el-button @click="loadData">刷新</el-button>
          <el-button type="primary" @click="router.push('/video/new')">新建分析</el-button>
        </el-space>
      </div>
    </el-card>

    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="12" :md="8" :lg="4">
        <el-card shadow="never" class="panel-card stat-card">
          <div class="stat-label">总任务数</div>
          <div class="stat-value">{{ statistics.total }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="8" :lg="4">
        <el-card shadow="never" class="panel-card stat-card">
          <div class="stat-label">待处理</div>
          <div class="stat-value warning">{{ statistics.pending }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="8" :lg="4">
        <el-card shadow="never" class="panel-card stat-card">
          <div class="stat-label">分析中</div>
          <div class="stat-value info">{{ statistics.processing }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="8" :lg="4">
        <el-card shadow="never" class="panel-card stat-card">
          <div class="stat-label">已完成</div>
          <div class="stat-value success">{{ statistics.completed }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="8" :lg="4">
        <el-card shadow="never" class="panel-card stat-card">
          <div class="stat-label">命中违规</div>
          <div class="stat-value danger">{{ statistics.hit }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="24" :md="24" :lg="17">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>任务流列表</span>
              <el-tag effect="plain">{{ tableData.length }} 条</el-tag>
            </div>
          </template>

          <div class="task-list-wrap" v-loading="loading">
            <section v-for="(group, idx) in groupedTasks" :key="idx" class="time-group">
              <div class="group-label">{{ group.label }}</div>
              <div class="nodes-grid">
                <article
                  v-for="(task, tIdx) in group.tasks"
                  :key="task.taskId"
                  class="task-card"
                  :class="[
                    'task-' + task.status.toLowerCase(),
                    'result-' + (task.moderationResult || 'unknown').toLowerCase()
                  ]"
                  :style="{ animationDelay: tIdx * 40 + 'ms' }"
                  @click="openTask(task)"
                >
                  <div class="task-header">
                    <div class="task-call-id">{{ task.callId }}</div>
                    <div class="task-status-badges">
                      <span class="status-pill" :class="'status-' + task.status.toLowerCase()">
                        {{ getStatusText(task.status) }}
                      </span>
                      <span
                        v-if="task.moderationResult"
                        class="result-pill"
                        :class="'result-' + task.moderationResult.toLowerCase()"
                      >
                        {{ getResultText(task.moderationResult) }}
                      </span>
                    </div>
                  </div>

                  <div class="decision-strip" :class="'strip-' + getDecisionClass(task)">
                    <div class="decision-strip-content">
                      <span class="decision-icon">{{ getDecisionIcon(task) }}</span>
                      <span class="decision-text">
                        <strong>{{ getDecisionTitle(task) }}</strong>
                        <template v-if="task.status !== 'DRAFT' && task.overallConfidence != null">
                          <span class="decision-sep">·</span>
                          <span>置信度 {{ formatConfidence(task.overallConfidence) }}</span>
                        </template>
                        <span class="decision-sep" v-if="task.summary?.totalViolations">·</span>
                        <span v-if="task.summary?.totalViolations">{{ task.summary.totalViolations }} 项违规</span>
                      </span>
                    </div>
                  </div>

                  <div class="task-meta-row">
                    <span class="meta-item policy-name">{{ task.policyName || '默认策略' }}</span>
                    <span class="meta-divider">|</span>
                    <span class="meta-item">{{ getHostFromCallId(task.callId) }}</span>
                    <span class="meta-divider">|</span>
                    <span class="meta-item">{{ calculateDuration(task.createdAt, task.completedAt) }}</span>
                    <span class="meta-divider">|</span>
                    <span class="meta-item">{{ formatTime(task.createdAt) }}</span>
                  </div>

                  <div
                    v-if="task.moderationResult === 'HIT' && task.violations?.length > 0"
                    class="violation-flag"
                    @click.stop
                  >
                    <el-popover
                      placement="bottom-end"
                      :width="400"
                      trigger="hover"
                      :popper-style="{
                        background: '#fff',
                        border: '1px solid rgba(0,0,0,0.06)',
                        borderRadius: '12px',
                        padding: '16px',
                        boxShadow: '0 8px 30px rgba(0,0,0,0.08)'
                      }"
                    >
                      <template #reference>
                        <div class="violation-flag-btn">
                          <span>违规 {{ task.violations.filter(v => v.detected).length }}</span>
                        </div>
                      </template>
                      <div class="violation-popover-content">
                        <div class="popover-title">违规检测详情</div>
                        <div
                          v-for="(v, i) in task.violations.filter(v => v.detected)"
                          :key="i"
                          class="violation-detail-item"
                        >
                          <div class="violation-detail-header">
                            <span class="violation-type-label">{{ v.type }}</span>
                            <span class="violation-time-label">{{ v.startSec }}s — {{ v.endSec }}s</span>
                            <span class="violation-conf-label">{{ formatConfidence(v.confidence) }}</span>
                          </div>
                          <div class="violation-evidence">{{ truncateText(v.evidence, 80) }}</div>
                        </div>
                      </div>
                    </el-popover>
                  </div>
                </article>
              </div>
            </section>

            <el-empty v-if="!loading && tableData.length === 0" description="暂无分析任务记录" :image-size="96">
              <el-button type="primary" @click="router.push('/video/new')">创建第一个任务</el-button>
            </el-empty>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="24" :lg="7">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>筛选条件</span>
            </div>
          </template>

          <div class="filter-block">
            <div class="filter-label">Call ID</div>
            <el-input
              v-model="searchForm.callId"
              placeholder="输入 Call ID 回车搜索"
              @keyup.enter="handleSearch"
            />
          </div>

          <div class="filter-block">
            <div class="filter-label">Policy</div>
            <el-select
              v-model="searchForm.policyId"
              filterable
              clearable
              placeholder="按 Policy 筛选"
              style="width: 100%"
            >
              <el-option
                v-for="policy in policyOptions"
                :key="policy.policyId"
                :label="`${policy.name || policy.policyId}（${policy.policyId}）`"
                :value="policy.policyId"
              />
            </el-select>
          </div>

          <div class="filter-block">
            <div class="filter-label">状态</div>
            <div class="toggle-list">
              <button
                v-for="opt in statusOptions"
                :key="opt.value"
                class="toggle-pill"
                :class="{ active: searchForm.status === opt.value }"
                @click="toggleStatus(opt.value)"
              >
                {{ opt.label }}
              </button>
            </div>
          </div>

          <div class="filter-block">
            <div class="filter-label">结果</div>
            <div class="toggle-list">
              <button
                v-for="opt in resultOptions"
                :key="opt.value"
                class="toggle-pill"
                :class="{ active: searchForm.result === opt.value }"
                @click="toggleResult(opt.value)"
              >
                {{ opt.label }}
              </button>
            </div>
          </div>

          <el-space>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-space>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { videoApi } from '@/api/video'
import type { VideoAnalysisTask } from '@/types/video'
import { skillOsApi, type PolicyDefinition } from '@/api/skillos'

const router = useRouter()
const loading = ref(false)

const searchForm = reactive({
  callId: '',
  policyId: '',
  status: '',
  result: ''
})

const statistics = reactive({
  total: 0,
  pending: 0,
  processing: 0,
  completed: 0,
  hit: 0
})

const tableData = ref<VideoAnalysisTask[]>([])
const policyOptions = ref<PolicyDefinition[]>([])
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待处理', value: 'PENDING' },
  { label: '分析中', value: 'PROCESSING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '失败', value: 'FAILED' }
]

const resultOptions = [
  { label: '全部', value: '' },
  { label: '未命中', value: 'NOT_HIT' },
  { label: '命中', value: 'HIT' },
  { label: '疑似', value: 'SUSPECTED' }
]

const groupedTasks = computed(() => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)

  const groups: Array<{ label: string; tasks: VideoAnalysisTask[] }> = [
    { label: '今天', tasks: [] },
    { label: '昨天', tasks: [] },
    { label: '更早', tasks: [] }
  ]

  tableData.value.forEach(task => {
    if (!task.createdAt) return
    const taskDate = new Date(task.createdAt)
    taskDate.setHours(0, 0, 0, 0)
    if (taskDate.getTime() === today.getTime()) groups[0].tasks.push(task)
    else if (taskDate.getTime() === yesterday.getTime()) groups[1].tasks.push(task)
    else groups[2].tasks.push(task)
  })

  return groups.filter(g => g.tasks.length > 0)
})

function toggleStatus(value: string) {
  searchForm.status = searchForm.status === value ? '' : value
}

function toggleResult(value: string) {
  searchForm.result = searchForm.result === value ? '' : value
}

function getStatusText(status: string) {
  return { DRAFT: '草稿', PENDING: '待处理', PROCESSING: '分析中', COMPLETED: '已完成', FAILED: '失败' }[status] || status
}

function getResultText(result: string) {
  return { NOT_HIT: '未命中违规', HIT: '命中违规', SUSPECTED: '疑似风险' }[result] || result
}

function getDecisionClass(task: VideoAnalysisTask): string {
  if (task.status === 'DRAFT') return 'draft'
  if (task.moderationResult === 'NOT_HIT') return 'clean'
  if (task.moderationResult === 'HIT') return 'hit'
  if (task.moderationResult === 'SUSPECTED') return 'suspect'
  if (task.status === 'PENDING') return 'pending'
  if (task.status === 'PROCESSING') return 'processing'
  if (task.status === 'FAILED') return 'failed'
  return 'default'
}

function getDecisionIcon(task: VideoAnalysisTask): string {
  if (task.status === 'DRAFT') return '✎'
  if (task.moderationResult === 'NOT_HIT') return '✓'
  if (task.moderationResult === 'HIT') return '⚠'
  if (task.moderationResult === 'SUSPECTED') return '？'
  if (task.status === 'PENDING') return '○'
  if (task.status === 'PROCESSING') return '◐'
  if (task.status === 'FAILED') return '✕'
  return '○'
}

function getDecisionTitle(task: VideoAnalysisTask): string {
  if (task.status === 'DRAFT') return '草稿待执行'
  if (task.moderationResult === 'NOT_HIT') return '未命中违规'
  if (task.moderationResult === 'HIT') return `命中：${getPrimaryViolation(task)}`
  if (task.moderationResult === 'SUSPECTED') return '疑似风险'
  if (task.status === 'COMPLETED') return '分析完成'
  if (task.status === 'PENDING') return '等待分析'
  if (task.status === 'PROCESSING') return 'AI 分析中'
  if (task.status === 'FAILED') return '分析失败'
  return '未知状态'
}

function getPrimaryViolation(task: VideoAnalysisTask): string {
  return task.summary?.primaryViolation?.replace(/_/g, ' ') ||
    task.violations?.find(v => v.detected)?.type.replace(/_/g, ' ') ||
    '未知违规'
}

function formatConfidence(value?: number): string {
  if (value == null) return '--'
  return `${Math.round(value * 100)}%`
}

function formatTime(dateStr: string): string {
  if (!dateStr) return 'N/A'
  const d = new Date(dateStr)
  return `${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function calculateDuration(startStr: string, endStr?: string): string {
  if (!startStr) return '0s'
  const start = new Date(startStr).getTime()
  const end = endStr ? new Date(endStr).getTime() : Date.now()
  const seconds = Math.floor((end - start) / 1000)
  if (seconds < 60) return `${seconds}s`
  const minutes = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${minutes}分${secs}秒`
}

function getHostFromCallId(callId: string): string {
  const parts = callId.split('_')
  return parts[0] || '默认'
}

function openTask(task: VideoAnalysisTask) {
  if (task.status === 'DRAFT') {
    router.push({
      path: '/video/new',
      query: { draftId: task.taskId }
    })
    return
  }
  router.push(`/video/${task.callId}`)
}

function truncateText(text: string, max: number): string {
  if (!text) return ''
  return text.length <= max ? text : `${text.slice(0, max)}...`
}

async function loadData() {
  loading.value = true
  try {
    const result = await videoApi.getList({
      callId: searchForm.callId || undefined,
      policyId: searchForm.policyId || undefined,
      status: searchForm.status || undefined,
      result: searchForm.result || undefined,
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    tableData.value = (result.list || []).sort((a, b) => {
      const at = a.createdAt ? new Date(a.createdAt).getTime() : 0
      const bt = b.createdAt ? new Date(b.createdAt).getTime() : 0
      return bt - at
    })
    pagination.total = result.total
  } catch (e) {
    console.error('加载失败:', e)
  } finally {
    loading.value = false
  }
}

async function loadPolicyOptions() {
  try {
    const res = await skillOsApi.listPolicies()
    policyOptions.value = (res.policies || []).slice().sort((a, b) => a.policyId.localeCompare(b.policyId))
  } catch (e) {
    console.error('Policy 列表加载失败:', e)
  }
}

async function loadStatistics() {
  try {
    const [all, pending, processing, completed, hit] = await Promise.all([
      videoApi.getList({ page: 1, pageSize: 1 }),
      videoApi.getList({ status: 'PENDING', page: 1, pageSize: 1 }),
      videoApi.getList({ status: 'PROCESSING', page: 1, pageSize: 1 }),
      videoApi.getList({ status: 'COMPLETED', page: 1, pageSize: 1 }),
      videoApi.getList({ result: 'HIT', page: 1, pageSize: 1 })
    ])
    statistics.total = all.total
    statistics.pending = pending.total
    statistics.processing = processing.total
    statistics.completed = completed.total
    statistics.hit = hit.total
  } catch (e) {
    console.error('统计加载失败:', e)
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  searchForm.callId = ''
  searchForm.policyId = ''
  searchForm.status = ''
  searchForm.result = ''
  pagination.page = 1
  loadData()
}

onMounted(() => {
  loadPolicyOptions()
  loadStatistics()
  loadData()
})
</script>

<style scoped>
.task-overview-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px;
}

.panel-card {
  border-radius: 14px;
}

.overview-header,
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
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

.stat-row {
  margin: 0;
}

.stat-card :deep(.el-card__body) {
  padding: 14px 16px;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
}

.stat-value {
  margin-top: 4px;
  font-size: 28px;
  line-height: 1;
  font-weight: 700;
  color: #0f172a;
}

.stat-value.warning { color: #b45309; }
.stat-value.info { color: #1d4ed8; }
.stat-value.success { color: #047857; }
.stat-value.danger { color: #b91c1c; }

.filter-block {
  margin-bottom: 14px;
}

.filter-label {
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}

.toggle-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.toggle-pill {
  height: 28px;
  border-radius: 999px;
  border: 1px solid #dbe3ee;
  background: #fff;
  color: #475569;
  font-size: 12px;
  padding: 0 10px;
  cursor: pointer;
}

.toggle-pill.active {
  border-color: #2563eb;
  background: #eff6ff;
  color: #2563eb;
}

.task-list-wrap {
  min-height: 520px;
}

.time-group {
  margin-bottom: 18px;
}

.group-label {
  margin-bottom: 10px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}

.nodes-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.task-card {
  position: relative;
  border: 1px solid #e2e8f0;
  border-left: 3px solid transparent;
  border-radius: 10px;
  padding: 14px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
  animation: cardFadeIn 0.3s ease forwards;
  opacity: 0;
}

.task-card:hover {
  border-color: #cbd5e1;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

@keyframes cardFadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.task-pending { border-left-color: #f59e0b; }
.task-processing { border-left-color: #3b82f6; }
.task-completed { border-left-color: #10b981; }
.task-failed { border-left-color: #ef4444; }
.task-draft { border-left-color: #6366f1; }

.result-hit {
  background: linear-gradient(90deg, rgba(254, 242, 242, 0.58) 0%, #fff 100%);
}

.task-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.task-call-id {
  font-family: 'JetBrains Mono', monospace;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.task-status-badges {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.status-pill,
.result-pill {
  border-radius: 8px;
  padding: 3px 8px;
  font-size: 11px;
  font-weight: 600;
}

.status-pill.status-pending { background: #fef3c7; color: #92400e; }
.status-pill.status-processing { background: #dbeafe; color: #1d4ed8; }
.status-pill.status-completed { background: #d1fae5; color: #047857; }
.status-pill.status-failed { background: #fee2e2; color: #dc2626; }
.status-pill.status-draft { background: #e0e7ff; color: #4338ca; }
.result-pill.result-not-hit { background: #d1fae5; color: #047857; }
.result-pill.result-hit { background: #fee2e2; color: #dc2626; }
.result-pill.result-suspected { background: #fef3c7; color: #92400e; }

.decision-strip {
  margin-top: 10px;
  border-radius: 10px;
  border-left: 3px solid;
  padding: 9px 10px;
}

.decision-strip-content {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #475569;
}

.decision-text strong {
  color: #0f172a;
}

.decision-sep {
  color: #cbd5e1;
}

.decision-strip.strip-clean { background: #f0fdf4; border-left-color: #10b981; }
.decision-strip.strip-hit { background: #fef2f2; border-left-color: #ef4444; }
.decision-strip.strip-suspect { background: #fef3c7; border-left-color: #f59e0b; }
.decision-strip.strip-draft { background: #eef2ff; border-left-color: #6366f1; }
.decision-strip.strip-pending { background: #f8fafc; border-left-color: #f59e0b; }
.decision-strip.strip-processing { background: #eff6ff; border-left-color: #3b82f6; }
.decision-strip.strip-failed { background: #fef2f2; border-left-color: #ef4444; }

.task-meta-row {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 11px;
  color: #94a3b8;
}

.meta-item.policy-name {
  color: #4f46e5;
  font-weight: 600;
}

.meta-divider {
  color: #e2e8f0;
}

.violation-flag {
  position: absolute;
  right: 14px;
  bottom: 14px;
}

.violation-flag-btn {
  border-radius: 8px;
  padding: 4px 8px;
  background: #fee2e2;
  color: #dc2626;
  font-size: 11px;
  font-weight: 600;
}

.violation-popover-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.popover-title {
  font-size: 12px;
  font-weight: 600;
  color: #0f172a;
}

.violation-detail-item {
  border: 1px solid rgba(220, 38, 38, 0.1);
  border-radius: 8px;
  background: #f8fafc;
  padding: 8px;
}

.violation-detail-header {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.violation-type-label {
  color: #dc2626;
  font-size: 11px;
  font-weight: 600;
}

.violation-time-label,
.violation-conf-label {
  font-size: 10px;
  color: #64748b;
}

.violation-evidence {
  margin-top: 6px;
  font-size: 10px;
  color: #475569;
}

@media (max-width: 768px) {
  .overview-header,
  .panel-header,
  .task-header {
    flex-wrap: wrap;
  }

  .violation-flag {
    position: static;
    margin-top: 10px;
  }
}
</style>
