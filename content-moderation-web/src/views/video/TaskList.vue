<template>
  <div class="bento-audit-stream">
    <!-- 顶部标题区 -->
    <div class="stream-header">
      <div class="header-content">
        <div class="title-block">
          <h1 class="stream-title">
            <div class="title-icon-box">
              <svg class="title-icon" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.5"/>
                <circle cx="12" cy="12" r="4" fill="currentColor"/>
              </svg>
            </div>
            <span>AI 审核执行流</span>
          </h1>
          <p class="stream-subtitle">AI Agent 多模态视频分析过程与风险判定实时追踪</p>
        </div>
        <button class="create-bento-btn" @click="router.push('/video/new')">
          <span class="create-btn-icon">+</span>
          <span>新建分析</span>
        </button>
      </div>
    </div>

    <!-- Bento 统计卡片区 -->
    <div class="bento-stats-grid">
      <div class="bento-stat-card">
        <div class="stat-icon-box total">
          <svg class="stat-icon" viewBox="0 0 24 24" fill="none">
            <rect x="4" y="4" width="16" height="16" rx="4" stroke="currentColor" stroke-width="1.5"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ statistics.total }}</span>
          <span class="stat-label">总任务数</span>
        </div>
        <div class="stat-trend">
          <span class="trend-indicator">全部</span>
        </div>
      </div>

      <div class="bento-stat-card pending">
        <div class="stat-icon-box pending">
          <svg class="stat-icon" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="8" stroke="currentColor" stroke-width="1.5" stroke-dasharray="4 4"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ statistics.pending }}</span>
          <span class="stat-label">待处理</span>
        </div>
        <div class="stat-trend">
          <span class="trend-indicator pending">等待中</span>
        </div>
      </div>

      <div class="bento-stat-card processing">
        <div class="stat-icon-box processing">
          <svg class="stat-icon" viewBox="0 0 24 24" fill="none">
            <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ statistics.processing }}</span>
          <span class="stat-label">分析中</span>
        </div>
        <div class="stat-trend">
          <span class="trend-indicator processing">扫描中</span>
        </div>
      </div>

      <div class="bento-stat-card completed">
        <div class="stat-icon-box completed">
          <svg class="stat-icon" viewBox="0 0 24 24" fill="none">
            <path d="M20 6L9 17l-5-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ statistics.completed }}</span>
          <span class="stat-label">已完成</span>
        </div>
        <div class="stat-trend">
          <span class="trend-indicator completed">正常</span>
        </div>
      </div>

      <div class="bento-stat-card hit">
        <div class="stat-icon-box hit">
          <svg class="stat-icon" viewBox="0 0 24 24" fill="none">
            <path d="M12 9v4M12 17h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ statistics.hit }}</span>
          <span class="stat-label">命中违规</span>
        </div>
        <div class="stat-trend">
          <span class="trend-indicator hit">需处理</span>
        </div>
      </div>
    </div>

    <!-- 筛选区 -->
    <div class="filter-bento-bar">
      <div class="search-bento-wrapper">
        <svg class="search-icon" viewBox="0 0 24 24" fill="none">
          <circle cx="11" cy="11" r="8" stroke="currentColor" stroke-width="1.5"/>
          <path d="M21 21l-4.35-4.35" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
        <input
          v-model="searchForm.callId"
          class="search-bento-input"
          placeholder="搜索 Call ID..."
          @keyup.enter="handleSearch"
        />
      </div>
      
      <div class="filter-bento-group">
        <span class="filter-label">状态：</span>
        <div class="filter-toggles">
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

      <div class="filter-bento-group">
        <span class="filter-label">结果：</span>
        <div class="filter-toggles">
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
    </div>

    <!-- 任务节点列表 -->
    <div class="task-nodes-container" v-loading="loading">
      <!-- 时间分组 -->
      <div v-for="(group, idx) in groupedTasks" :key="idx" class="time-group">
        <div class="group-label">{{ group.label }}</div>
        
        <div class="nodes-grid">
          <div
            v-for="(task, tIdx) in group.tasks"
            :key="task.taskId"
            class="task-bento-card"
            :class="[
              'task-' + task.status.toLowerCase(),
              'result-' + (task.moderationResult || 'unknown').toLowerCase()
            ]"
            :style="{ animationDelay: tIdx * 50 + 'ms' }"
            @click="router.push(`/video/${task.callId}`)"
          >
            <!-- 左侧 Icon Box -->
            <div class="task-icon-wrapper">
              <div class="task-icon-box" :class="'icon-' + getIconClass(task)">
                <svg class="task-icon-svg" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.5"/>
                  <circle cx="12" cy="12" r="4" fill="currentColor"/>
                </svg>
              </div>
            </div>

            <!-- 主体内容 -->
            <div class="task-bento-body">
              <!-- 标题行 -->
              <div class="task-bento-header">
                <div class="task-title-row">
                  <span class="task-call-id">{{ task.callId }}</span>
                </div>
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

              <!-- 决策条 -->
              <div class="decision-strip" :class="'strip-' + getDecisionClass(task)">
                <div class="decision-strip-content">
                  <span class="decision-icon">{{ getDecisionIcon(task) }}</span>
                  <span class="decision-text">
                    <strong>{{ getDecisionTitle(task) }}</strong>
                    <span class="decision-sep">·</span>
                    <span>置信度 {{ formatConfidence(task.overallConfidence) }}</span>
                    <span class="decision-sep" v-if="task.summary?.totalViolations">·</span>
                    <span v-if="task.summary?.totalViolations">{{ task.summary.totalViolations }} 项违规</span>
                  </span>
                </div>
              </div>

              <!-- 元数据 -->
              <div class="task-meta-row">
                <span class="meta-item policy-name" v-if="task.policyName">
                  {{ task.policyName }}
                </span>
                <span class="meta-item policy-name" v-else>
                  默认策略
                </span>
                <span class="meta-divider">|</span>
                <span class="meta-item">{{ getHostFromCallId(task.callId) }}</span>
                <span class="meta-divider">|</span>
                <span class="meta-item">{{ calculateDuration(task.createdAt, task.completedAt) }}</span>
                <span class="meta-divider">|</span>
                <span class="meta-item">{{ formatTime(task.createdAt) }}</span>
              </div>
            </div>

            <!-- 违规标记 -->
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
                    <svg class="flag-icon" viewBox="0 0 24 24" fill="none">
                      <path d="M12 9v4M12 17h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    <span>{{ task.violations.filter(v => v.detected).length }}</span>
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
                    <div class="violation-conf-bar">
                      <div class="conf-fill" :style="{ width: formatConfidence(v.confidence) }"></div>
                    </div>
                  </div>
                </div>
              </el-popover>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && tableData.length === 0" class="bento-empty-state">
        <div class="empty-icon-wrapper">
          <svg class="empty-icon-svg" viewBox="0 0 24 24" fill="none">
            <rect x="4" y="4" width="16" height="16" rx="4" stroke="currentColor" stroke-width="1.5"/>
            <path d="M12 8v8M8 12h8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="empty-text">暂无分析任务记录</div>
        <button class="empty-bento-btn" @click="router.push('/video/new')">
          <span>+</span> 创建第一个任务
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { videoApi } from '@/api/video'
import type { VideoAnalysisTask } from '@/types/video'

const router = useRouter()
const loading = ref(false)

const searchForm = reactive({
  callId: '',
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
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })

const statusOptions = [
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
    
    if (taskDate.getTime() === today.getTime()) {
      groups[0].tasks.push(task)
    } else if (taskDate.getTime() === yesterday.getTime()) {
      groups[1].tasks.push(task)
    } else {
      groups[2].tasks.push(task)
    }
  })
  
  return groups.filter(g => g.tasks.length > 0)
})

function toggleStatus(value: string) {
  searchForm.status = searchForm.status === value ? '' : value
}

function toggleResult(value: string) {
  searchForm.result = searchForm.result === value ? '' : value
}

function getIconClass(task: VideoAnalysisTask): string {
  if (task.moderationResult === 'HIT') return 'hit'
  if (task.moderationResult === 'NOT_HIT') return 'completed'
  if (task.status === 'PENDING') return 'pending'
  if (task.status === 'PROCESSING') return 'processing'
  if (task.status === 'FAILED') return 'failed'
  return 'completed'
}

function getStatusText(status: string) {
  return { PENDING: '待处理', PROCESSING: '分析中', COMPLETED: '已完成', FAILED: '失败' }[status] || status
}

function getResultText(result: string) {
  return { NOT_HIT: '未命中违规', HIT: '命中违规', SUSPECTED: '疑似风险' }[result] || result
}

function getDecisionClass(task: VideoAnalysisTask): string {
  if (task.moderationResult === 'NOT_HIT') return 'clean'
  if (task.moderationResult === 'HIT') return 'hit'
  if (task.moderationResult === 'SUSPECTED') return 'suspect'
  if (task.status === 'PENDING') return 'pending'
  if (task.status === 'PROCESSING') return 'processing'
  if (task.status === 'FAILED') return 'failed'
  return 'default'
}

function getDecisionIcon(task: VideoAnalysisTask): string {
  if (task.moderationResult === 'NOT_HIT') return '✓'
  if (task.moderationResult === 'HIT') return '⚠'
  if (task.moderationResult === 'SUSPECTED') return '？'
  if (task.status === 'PENDING') return '○'
  if (task.status === 'PROCESSING') return '◐'
  if (task.status === 'FAILED') return '✕'
  return '○'
}

function getDecisionTitle(task: VideoAnalysisTask): string {
  if (task.moderationResult === 'NOT_HIT') return '未命中违规'
  if (task.moderationResult === 'HIT') return `命中：${getPrimaryViolation(task)}`
  if (task.moderationResult === 'SUSPECTED') return '疑似风险'
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
  if (value == null) return '0%'
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

function truncateText(text: string, max: number): string {
  if (!text) return ''
  return text.length <= max ? text : text.slice(0, max) + '...'
}

async function loadData() {
  loading.value = true
  try {
    const result = await videoApi.getList({
      callId: searchForm.callId || undefined,
      status: searchForm.status || undefined,
      result: searchForm.result || undefined,
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    tableData.value = result.list
    pagination.total = result.total
  } catch (e) {
    console.error('加载失败:', e)
  } finally {
    loading.value = false
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
  searchForm.status = ''
  searchForm.result = ''
  pagination.page = 1
  loadData()
}

onMounted(() => {
  loadStatistics()
  loadData()
})
</script>

<style scoped>
/* ===== 基础 ===== */
.bento-audit-stream {
  min-height: 100vh;
  background: linear-gradient(135deg, #F8FAFC 0%, #F1F5F9 100%);
  color: #1E293B;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Inter', sans-serif;
  padding: 24px;
}

/* ===== 顶部 ===== */
.stream-header {
  max-width: 1400px;
  margin: 0 auto 24px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stream-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  color: #0F172A;
}

.title-icon-box {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #10B981 0%, #059669 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(16,185,129,0.2);
}

.title-icon {
  width: 18px;
  height: 18px;
  color: #fff;
}

.stream-subtitle {
  font-size: 13px;
  color: #64748B;
  margin: 0;
}

.create-bento-btn {
  height: 40px;
  padding: 0 20px;
  background: linear-gradient(135deg, #0F172A 0%, #1E293B 100%);
  border: none;
  border-radius: 10px;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(15,23,42,0.2);
}

.create-bento-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(15,23,42,0.25);
}

.create-btn-icon {
  font-size: 18px;
  color: #10B981;
}

/* ===== Bento 统计卡片 ===== */
.bento-stats-grid {
  max-width: 1400px;
  margin: 0 auto 20px;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.bento-stat-card {
  background: rgba(255,255,255,0.9);
  backdrop-filter: blur(12px);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  ring: 1px rgba(148,163,184,0.1);
  box-shadow: 0 8px 30px rgba(0,0,0,0.04);
  transition: all 0.2s ease;
}

.bento-stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(0,0,0,0.06);
}

.stat-icon-box {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon-box.total {
  background: linear-gradient(135deg, #F1F5F9 0%, #E2E8F0 100%);
  color: #475569;
}

.stat-icon-box.pending {
  background: linear-gradient(135deg, #FEF3C7 0%, #FDE68A 100%);
  color: #92400E;
}

.stat-icon-box.processing {
  background: linear-gradient(135deg, #DBEAFE 0%, #BFDBFE 100%);
  color: #1D4ED8;
}

.stat-icon-box.completed {
  background: linear-gradient(135deg, #D1FAE5 0%, #A7F3D0 100%);
  color: #047857;
}

.stat-icon-box.hit {
  background: linear-gradient(135deg, #FEE2E2 0%, #FECACA 100%);
  color: #DC2626;
}

.stat-icon {
  width: 20px;
  height: 20px;
}

.stat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #0F172A;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 11px;
  color: #64748B;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.stat-trend {
  display: flex;
  align-items: center;
}

.trend-indicator {
  font-size: 10px;
  padding: 3px 8px;
  background: #F1F5F9;
  color: #64748B;
  border-radius: 10px;
  font-weight: 500;
}

.trend-indicator.pending {
  background: #FEF3C7;
  color: #92400E;
}

.trend-indicator.processing {
  background: #DBEAFE;
  color: #1D4ED8;
}

.trend-indicator.completed {
  background: #D1FAE5;
  color: #047857;
}

.trend-indicator.hit {
  background: #FEE2E2;
  color: #DC2626;
}

/* ===== 筛选区 ===== */
.filter-bento-bar {
  max-width: 1400px;
  margin: 0 auto 24px;
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.search-bento-wrapper {
  position: relative;
  flex: 1;
  min-width: 240px;
  max-width: 320px;
}

.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  width: 16px;
  height: 16px;
  color: #94A3B8;
}

.search-bento-input {
  width: 100%;
  height: 40px;
  padding: 0 14px 0 42px;
  background: rgba(255,255,255,0.9);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(148,163,184,0.2);
  border-radius: 10px;
  font-size: 13px;
  color: #0F172A;
  transition: all 0.2s ease;
}

.search-bento-input:focus {
  outline: none;
  border-color: rgba(148,163,184,0.4);
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
}

.search-bento-input::placeholder {
  color: #94A3B8;
}

.filter-bento-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-label {
  font-size: 12px;
  color: #64748B;
  font-weight: 500;
}

.filter-toggles {
  display: flex;
  gap: 6px;
}

.toggle-pill {
  height: 32px;
  padding: 0 14px;
  background: rgba(255,255,255,0.6);
  border: 1px solid rgba(148,163,184,0.2);
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
  color: #64748B;
  cursor: pointer;
  transition: all 0.2s ease;
}

.toggle-pill:hover {
  background: rgba(255,255,255,0.9);
  color: #0F172A;
}

.toggle-pill.active {
  background: #0F172A;
  border-color: #0F172A;
  color: #fff;
}

/* ===== 任务节点容器 ===== */
.task-nodes-container {
  max-width: 1400px;
  margin: 0 auto;
  min-height: 500px;
}

/* 时间分组 */
.time-group {
  margin-bottom: 32px;
}

.group-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748B;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 16px;
  padding-left: 8px;
}

.nodes-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* ===== 任务 Bento 卡片 ===== */
.task-bento-card {
  position: relative;
  display: flex;
  gap: 16px;
  padding: 20px;
  background: rgba(255,255,255,0.9);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(148,163,184,0.15);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 8px 30px rgba(0,0,0,0.04);
  animation: cardFadeIn 0.4s ease forwards;
  opacity: 0;
}

@keyframes cardFadeIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.task-bento-card:hover {
  transform: translateY(-3px);
  border-color: rgba(148,163,184,0.25);
  box-shadow: 0 16px 50px rgba(0,0,0,0.08);
}

/* 状态色左边框 */
.task-pending { border-left: 3px solid #F59E0B; }
.task-processing { border-left: 3px solid #3B82F6; }
.task-completed { border-left: 3px solid #10B981; }
.task-failed { border-left: 3px solid #EF4444; }

.result-hit {
  background: linear-gradient(90deg, rgba(254,242,242,0.5) 0%, rgba(255,255,255,0.9) 100%);
}

/* Icon 容器 */
.task-icon-wrapper {
  flex-shrink: 0;
  padding-top: 4px;
}

.task-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.task-icon-box.icon-completed {
  background: linear-gradient(135deg, #D1FAE5 0%, #A7F3D0 100%);
  color: #047857;
}

.task-icon-box.icon-hit {
  background: linear-gradient(135deg, #FEE2E2 0%, #FECACA 100%);
  color: #DC2626;
}

.task-icon-box.icon-pending {
  background: linear-gradient(135deg, #FEF3C7 0%, #FDE68A 100%);
  color: #92400E;
}

.task-icon-box.icon-processing {
  background: linear-gradient(135deg, #DBEAFE 0%, #BFDBFE 100%);
  color: #1D4ED8;
}

.task-icon-box.icon-failed {
  background: linear-gradient(135deg, #F1F5F9 0%, #E2E8F0 100%);
  color: #64748B;
}

.task-icon-svg {
  width: 22px;
  height: 22px;
}

/* 主体内容 */
.task-bento-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 标题行 */
.task-bento-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.task-call-id {
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  font-size: 14px;
  font-weight: 600;
  color: #0F172A;
  letter-spacing: -0.01em;
}

.task-type-badge {
  font-size: 11px;
  padding: 3px 8px;
  background: #F1F5F9;
  color: #475569;
  border-radius: 6px;
  font-weight: 500;
}

.task-status-badges {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.status-pill {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 8px;
  font-weight: 500;
  background: #F1F5F9;
  color: #475569;
}

.status-pill.status-pending {
  background: #FEF3C7;
  color: #92400E;
}

.status-pill.status-processing {
  background: #DBEAFE;
  color: #1D4ED8;
}

.status-pill.status-completed {
  background: #D1FAE5;
  color: #047857;
}

.status-pill.status-failed {
  background: #FEE2E2;
  color: #DC2626;
}

.result-pill {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 8px;
  font-weight: 500;
  background: #F1F5F9;
  color: #475569;
}

.result-pill.result-not-hit {
  background: #D1FAE5;
  color: #047857;
}

.result-pill.result-hit {
  background: #FEE2E2;
  color: #DC2626;
}

.result-pill.result-suspected {
  background: #FEF3C7;
  color: #92400E;
}

/* 决策条 */
.decision-strip {
  padding: 12px 14px;
  border-radius: 10px;
  border-left: 3px solid;
  transition: all 0.2s ease;
}

.decision-strip-content {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  font-size: 12px;
  color: #475569;
  line-height: 1.5;
}

.decision-icon {
  font-size: 15px;
  flex-shrink: 0;
}

.decision-text {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.decision-text strong {
  color: #0F172A;
  font-weight: 600;
}

.decision-sep {
  color: #CBD5E1;
  font-size: 10px;
}

/* 决策条颜色 */
.decision-strip.strip-clean {
  background: #F0FDF4;
  border-left-color: #10B981;
}

.decision-strip.strip-hit {
  background: #FEF2F2;
  border-left-color: #EF4444;
}

.decision-strip.strip-suspect {
  background: #FEF3C7;
  border-left-color: #F59E0B;
}

.decision-strip.strip-pending {
  background: #F8FAFC;
  border-left-color: #F59E0B;
}

.decision-strip.strip-processing {
  background: #EFF6FF;
  border-left-color: #3B82F6;
}

.decision-strip.strip-failed {
  background: #FEF2F2;
  border-left-color: #EF4444;
}

/* 元数据行 */
.task-meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: #94A3B8;
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  opacity: 0.6;
}

.meta-item {
  color: inherit;
}

.meta-item.policy-name {
  font-weight: 500;
  color: #6366F1;
  opacity: 1;
}

.meta-divider {
  color: #E2E8F0;
  font-size: 9px;
}

/* 违规标记 */
.violation-flag {
  position: absolute;
  right: 20px;
  bottom: 20px;
}

.violation-flag-btn {
  padding: 8px 14px;
  background: linear-gradient(135deg, #FEE2E2 0%, #FECACA 100%);
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
  color: #DC2626;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(220,38,38,0.15);
}

.violation-flag-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(220,38,38,0.2);
}

.flag-icon {
  width: 16px;
  height: 16px;
}

/* 违规详情 Popover */
.violation-popover-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.popover-title {
  font-size: 12px;
  font-weight: 600;
  color: #0F172A;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(148,163,184,0.15);
}

.violation-detail-item {
  padding: 12px;
  background: #F8FAFC;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border: 1px solid rgba(220,38,38,0.08);
}

.violation-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.violation-type-label {
  font-size: 11px;
  font-weight: 600;
  color: #DC2626;
}

.violation-time-label,
.violation-conf-label {
  font-size: 10px;
  color: #64748B;
  font-family: 'JetBrains Mono', monospace;
}

.violation-evidence {
  font-size: 10px;
  color: #475569;
  line-height: 1.4;
}

.violation-conf-bar {
  height: 4px;
  background: #E2E8F0;
  border-radius: 2px;
  overflow: hidden;
}

.conf-fill {
  height: 100%;
  background: linear-gradient(90deg, #DC2626 0%, #F87171 100%);
  border-radius: 2px;
}

/* ===== 空状态 ===== */
.bento-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  gap: 20px;
}

.empty-icon-wrapper {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #F1F5F9 0%, #E2E8F0 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 30px rgba(0,0,0,0.04);
}

.empty-icon-svg {
  width: 36px;
  height: 36px;
  color: #94A3B8;
}

.empty-text {
  font-size: 14px;
  color: #64748B;
}

.empty-bento-btn {
  height: 42px;
  padding: 0 24px;
  background: linear-gradient(135deg, #0F172A 0%, #1E293B 100%);
  border: none;
  border-radius: 12px;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(15,23,42,0.2);
}

.empty-bento-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(15,23,42,0.25);
}

.empty-bento-btn span {
  font-size: 20px;
  color: #10B981;
}

/* ===== 响应式 ===== */
@media (max-width: 1200px) {
  .bento-stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .bento-audit-stream {
    padding: 16px;
  }

  .header-content {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .bento-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .filter-bento-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-bento-wrapper {
    max-width: none;
  }

  .filter-bento-group {
    flex-direction: column;
    align-items: flex-start;
  }

  .task-bento-header {
    flex-wrap: wrap;
    gap: 8px;
  }

  .task-meta-row {
    flex-wrap: wrap;
    gap: 6px;
  }

  .violation-flag {
    position: static;
    margin-top: 12px;
  }
}
</style>
