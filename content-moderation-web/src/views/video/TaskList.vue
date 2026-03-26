<template>
  <div class="task-list-page precision-pulse-page">
    <div class="page-header">
      <div class="page-title-wrapper">
        <h1 class="page-title">智能治理分析任务</h1>
        <p class="page-subtitle">管理基于 AI Agent 的全量多模态视频分析任务</p>
      </div>
      <el-button type="primary" @click="router.push('/video/new')" class="create-btn precision-primary-btn">
        <el-icon><Plus /></el-icon>
        新建任务
      </el-button>
  </div>

  <el-card shadow="never" class="search-card">
      <el-form :model="searchForm" inline class="search-form-compact">
        <el-form-item class="search-item-compact">
          <el-input v-model="searchForm.callId" placeholder="Call ID" clearable class="search-input-compact" />
        </el-form-item>
        <el-form-item class="search-item-compact">
          <el-select v-model="searchForm.status" placeholder="状态" clearable class="search-select-compact">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-item-compact">
          <el-select v-model="searchForm.result" placeholder="结果" clearable class="search-select-compact">
            <el-option label="未命中" value="NOT_HIT" />
            <el-option label="命中" value="HIT" />
            <el-option label="疑似" value="SUSPECTED" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-actions-compact">
          <el-button @click="handleReset" class="reset-btn-compact">重置</el-button>
          <el-button type="primary" @click="handleSearch" class="search-btn-compact">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card precision-table-card">
      <div class="task-list-container" v-loading="loading">
        <!-- 顶部统计区 - 紧凑版 -->
        <div class="stats-bar">
          <div class="stat-item">
            <span class="stat-label">总数</span>
            <span class="stat-value mono">{{ statistics.total }}</span>
          </div>
          <div class="stat-divider">|</div>
          <div class="stat-item stat-pending">
            <span class="stat-label">待处理</span>
            <span class="stat-value mono">{{ statistics.pending }}</span>
          </div>
          <div class="stat-divider">|</div>
          <div class="stat-item stat-processing">
            <span class="stat-label">处理中</span>
            <span class="stat-value mono">{{ statistics.processing }}</span>
          </div>
          <div class="stat-divider">|</div>
          <div class="stat-item stat-completed">
            <span class="stat-label">已完成</span>
            <span class="stat-value mono">{{ statistics.completed }}</span>
          </div>
        </div>

        <!-- 任务列表 -->
        <div class="task-list-body">
          <div 
            v-for="task in tableData" 
            :key="task.taskId" 
            class="task-card task-row"
            :class="['task-status-' + task.status.toLowerCase()]"
            @click="router.push(`/video/${task.callId}`)"
          >
            <!-- 第一行：状态 + 调用 ID + 结果标签 -->
            <div class="task-row-main">
              <div class="task-row-left">
                <div class="status-indicator-inline" :class="'status-' + task.status.toLowerCase()">
                  <span class="status-dot-inline"></span>
                  <el-icon v-if="task.status === 'COMPLETED'" class="status-icon-inline"><CircleCheck /></el-icon>
                  <el-icon v-else-if="task.status === 'FAILED'" class="status-icon-inline"><WarningFilled /></el-icon>
                  <el-icon v-else-if="task.status === 'PROCESSING'" class="status-icon-inline spinning"><Loading /></el-icon>
                </div>
                <span class="task-call-id-inline">{{ task.callId }}</span>
              </div>
              <div class="task-row-right">
                <el-tag :type="getStatusType(task.status)" effect="plain" size="small" class="status-tag-compact">
                  {{ getStatusText(task.status) }}
                </el-tag>
                <el-tag 
                  v-if="task.moderationResult" 
                  :type="getResultType(task.moderationResult)" 
                  effect="plain" 
                  size="small"
                  class="result-tag-compact"
                  :class="'result-' + task.moderationResult.toLowerCase()"
                >
                  {{ getResultText(task.moderationResult) }}
                </el-tag>
              </div>
            </div>

            <!-- 第二行：元数据信息 -->
            <div class="task-row-meta">
              <span class="meta-text">
                <span class="meta-label">Host:</span>
                <span class="meta-value">{{ task.callId.split('_')[0] || 'default' }}</span>
              </span>
              <span class="meta-divider">|</span>
              <span class="meta-text">
                <span class="meta-label">类型:</span>
                <span class="meta-value">{{ getAnalysisTypeText(task.analysisType) }}</span>
              </span>
              <span class="meta-divider">|</span>
              <span class="meta-text mono">
                {{ formatDateTimeCompact(task.createdAt) }}
              </span>
              <span class="meta-divider" v-if="task.completedAt && task.createdAt">|</span>
              <span class="meta-text mono" v-if="task.completedAt && task.createdAt">
                {{ calculateDurationCompact(task.createdAt, task.completedAt) }}
              </span>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="!loading && tableData.length === 0" class="empty-state">
            <el-empty description="暂无任务记录" />
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            @size-change="loadData"
            @current-change="loadData"
            class="custom-pagination"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search, Document, Clock, Loading, CircleCheck, ArrowRight, WarningFilled, Timer } from '@element-plus/icons-vue'
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
  completed: 0
})

const tableData = ref<VideoAnalysisTask[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const statusMap: Record<string, { type: string; text: string }> = {
  PENDING: { type: 'warning', text: '待处理' },
  PROCESSING: { type: 'info', text: '处理中' },
  COMPLETED: { type: 'success', text: '已完成' },
  FAILED: { type: 'danger', text: '失败' }
}

const resultMap: Record<string, { type: string; text: string }> = {
  NOT_HIT: { type: 'success', text: '未命中违规' },
  HIT: { type: 'danger', text: '命中违规' },
  SUSPECTED: { type: 'warning', text: '疑似命中' }
}

function getStatusType(status: string) {
  return statusMap[status]?.type || 'info'
}

function getStatusText(status: string) {
  return statusMap[status]?.text || status
}

function getStatusBadgeClass(status: string) {
  const badgeClassMap: Record<string, string> = {
    PENDING: 'badge-pending',
    PROCESSING: 'badge-processing',
    COMPLETED: 'badge-completed',
    FAILED: 'badge-failed'
  }
  return badgeClassMap[status] || 'badge-processing'
}

function getResultType(result: string) {
  return resultMap[result]?.type || 'info'
}

function getResultText(result: string) {
  return resultMap[result]?.text || result
}

function getResultBadgeClass(result: string) {
  const badgeClassMap: Record<string, string> = {
    NOT_HIT: 'badge-not-hit',
    HIT: 'badge-hit',
    SUSPECTED: 'badge-suspected'
  }
  return badgeClassMap[result] || 'badge-processing'
}

function isViolationHit(result: string) {
  return result === 'HIT'
}

function getAnalysisTypeText(type?: string): string {
  if (!type) return 'STANDARD'
  const typeMap: Record<string, string> = {
    STANDARD: '标准分析',
    HOST_VIOLATION: '主播违规'
  }
  return typeMap[type] || type
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function formatDateTime(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

function formatDateTimeCompact(dateStr: string) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${month}/${day} ${hour}:${minute}`
}

function calculateDuration(startStr: string, endStr: string): string {
  if (!startStr || !endStr) return '-'
  const start = new Date(startStr).getTime()
  const end = new Date(endStr).getTime()
  const duration = end - start
  if (duration < 1000) return `${duration}ms`
  if (duration < 60000) return `${Math.round(duration / 1000)}s`
  const minutes = Math.floor(duration / 60000)
  const seconds = Math.round((duration % 60000) / 1000)
  return `${minutes}m ${seconds}s`
}

function calculateDurationCompact(startStr: string, endStr: string): string {
  if (!startStr || !endStr) return '-'
  const start = new Date(startStr).getTime()
  const end = new Date(endStr).getTime()
  const duration = Math.floor((end - start) / 1000)
  if (duration < 60) return `${duration}s`
  const minutes = Math.floor(duration / 60)
  const seconds = duration % 60
  return `${minutes}m${seconds}s`
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
  } catch (error) {
    console.error('加载失败:', error)
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  try {
    const [all, pending, processing, completed] = await Promise.all([
      videoApi.getList({ page: 1, pageSize: 1 }),
      videoApi.getList({ status: 'PENDING', page: 1, pageSize: 1 }),
      videoApi.getList({ status: 'PROCESSING', page: 1, pageSize: 1 }),
      videoApi.getList({ status: 'COMPLETED', page: 1, pageSize: 1 })
    ])
    statistics.total = all.total
    statistics.pending = pending.total
    statistics.processing = processing.total
    statistics.completed = completed.total
  } catch (error) {
    console.error('统计加载失败:', error)
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
.task-list-page {
  padding: 0;
}

.precision-pulse-page {
  display: grid;
  gap: 16px;
  font-family: var(--font-sans);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.page-title-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.01em;
  margin: 0;
}

.page-subtitle {
  font-size: 13px;
  color: #6c7785;
  letter-spacing: 0.02em;
  margin: 0;
}

.create-btn {
  height: 40px;
  padding-inline: 16px;
  border-radius: 10px;
  border: 0.5px solid rgba(16, 24, 40, 0.22);
  font-weight: 600;
}

.precision-primary-btn {
  box-shadow: none;
  transition: transform 120ms ease, border-width 120ms ease;
}

.precision-primary-btn:hover {
  transform: translateY(-1px);
  border-width: 1px;
}

.monitor-card,
.search-card,
.table-card {
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 8px 24px rgba(15, 36, 82, 0.06);
  background: #fff;
}

.monitor-card :deep(.el-card__body),
.search-card :deep(.el-card__body),
.table-card :deep(.el-card__body) {
  padding: 16px;
}

.monitor-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.monitor-cell {
  position: relative;
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 10px;
  padding: 16px;
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.98) 0%, rgba(246, 250, 255, 0.86) 100%);
  backdrop-filter: blur(8px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.66), 0 6px 16px rgba(15, 36, 82, 0.08);
  transition: transform 140ms ease, box-shadow 140ms ease;
}

.monitor-cell:hover {
  transform: translateY(-2px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72), 0 10px 20px rgba(15, 36, 82, 0.12);
}

.monitor-top {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}

.monitor-icon {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 2px 6px rgba(15, 36, 82, 0.1);
}

.monitor-label {
  font-size: 12px;
  color: #4b5c75;
  letter-spacing: 0.02em;
}

.monitor-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 10px;
  color: #1d2b43;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
}

.pulse-line {
  width: 100%;
  height: 28px;
  display: block;
}

.pulse-area {
  opacity: 0.3;
}

.pulse-curve {
  fill: none;
  stroke-width: 1.5;
  stroke-linecap: round;
  animation: pulseMove 2.2s ease-in-out infinite;
  filter: drop-shadow(0 0 4px currentColor);
}

.tone-primary,
.tone-primary .monitor-icon {
  background: linear-gradient(140deg, rgba(236, 246, 255, 0.95) 0%, rgba(225, 239, 255, 0.88) 100%);
  color: #2563eb;
}

.tone-primary .pulse-area {
  fill: rgba(37, 99, 235, 0.2);
}

.tone-primary .pulse-curve {
  stroke: #2563eb;
}

.tone-warning,
.tone-warning .monitor-icon {
  background: linear-gradient(140deg, rgba(255, 249, 237, 0.95) 0%, rgba(255, 243, 218, 0.86) 100%);
  color: #b45309;
}

.tone-warning .pulse-area {
  fill: rgba(180, 83, 9, 0.2);
}

.tone-warning .pulse-curve {
  stroke: #b45309;
}

.tone-info,
.tone-info .monitor-icon {
  background: linear-gradient(140deg, rgba(239, 250, 255, 0.95) 0%, rgba(224, 245, 253, 0.86) 100%);
  color: #0f6fa5;
}

.tone-info .pulse-area {
  fill: rgba(15, 111, 165, 0.2);
}

.tone-info .pulse-curve {
  stroke: #0f6fa5;
}

.tone-success,
.tone-success .monitor-icon {
  background: linear-gradient(140deg, rgba(239, 252, 244, 0.95) 0%, rgba(224, 247, 232, 0.86) 100%);
  color: #047857;
}

.tone-success .pulse-area {
  fill: rgba(4, 120, 87, 0.2);
}

.tone-success .pulse-curve {
  stroke: #047857;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 16px;
}

.precision-item {
  margin-bottom: 0;
}

.search-input {
  width: 216px;
}

.search-select {
  width: 168px;
}

.precision-input :deep(.el-input__wrapper),
.precision-input :deep(.el-select__wrapper) {
  min-height: 36px;
  border-radius: 8px;
  box-shadow: none;
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.32);
  transition: border-color 140ms ease, background-color 140ms ease;
}

.precision-input :deep(.el-input__wrapper:hover),
.precision-input :deep(.el-select__wrapper:hover) {
  border-color: rgba(71, 85, 105, 0.42);
}

.precision-input :deep(.el-input__wrapper.is-focus),
.precision-input :deep(.el-select__wrapper.is-focused) {
  border-color: #2563eb;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.precision-input :deep(.el-input__wrapper.is-focus)::after,
.precision-input :deep(.el-select__wrapper.is-focused)::after {
  display: none;
}

.search-actions {
  margin-left: auto;
}

.precision-action-btn {
  min-height: 36px;
  border-radius: 8px;
  font-weight: 600;
  transition: transform 140ms ease, border-color 140ms ease, box-shadow 140ms ease;
}

.search-btn {
  border: 1px solid #2563eb;
  background: #2563eb;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.22);
}

.search-btn:hover {
  border-color: #1d4ed8;
  background: #1d4ed8;
  transform: translateY(-1px);
}

.reset-btn {
  border: 1px solid rgba(148, 163, 184, 0.56);
  background: #fff;
  color: #334155;
}

.reset-btn:hover {
  border-color: #2563eb;
  color: #1d4ed8;
  transform: translateY(-1px);
}

.table-card {
  overflow: hidden;
}

.precision-table-card :deep(.el-table) {
  background: transparent;
}

.precision-table-card :deep(.el-table::before) {
  display: none;
}

.precision-table-card :deep(.el-table th.el-table__cell) {
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
}

.precision-table-card :deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}

.precision-table-card :deep(.el-table__row) {
  position: relative;
}

.precision-table-card :deep(.el-table__row td:first-child) {
  position: relative;
}

.precision-table-card :deep(.el-table__row:hover > td.el-table__cell) {
  background: rgba(37, 99, 235, 0.04) !important;
}

.precision-table-card :deep(.el-table__row:hover > td:first-child::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 2px;
  background: #2563eb;
  border-radius: 999px;
}

.id-text {
  font-family: "Inter", "SF Pro Text", "SF Pro Display", -apple-system, BlinkMacSystemFont, sans-serif;
  color: #1e293b;
  letter-spacing: 0.01em;
}

.call-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.call-id {
  font-family: "Inter", "SF Pro Text", "SF Pro Display", -apple-system, BlinkMacSystemFont, sans-serif;
  color: #1e293b;
  font-weight: 500;
  letter-spacing: 0.01em;
}

.analysis-type {
  font-size: 12px;
  color: #64748b;
  font-weight: 400;
}

.precision-tag {
  border-radius: 999px;
  border: 1px solid transparent !important;
  font-weight: 600;
  letter-spacing: 0.01em;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.precision-tag :deep(.el-tag__content) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  line-height: 1.25;
  white-space: nowrap;
}

.badge-completed {
  background: #dcfce7 !important;
  color: #166534 !important;
  border-color: #86efac !important;
}

.badge-pending {
  background: #fef3c7 !important;
  color: #92400e !important;
  border-color: #fcd34d !important;
}

.badge-processing {
  background: #dbeafe !important;
  color: #1d4ed8 !important;
  border-color: #93c5fd !important;
}

.badge-failed,
.badge-hit {
  background: #fee2e2 !important;
  color: #991b1b !important;
  border-color: #fca5a5 !important;
}

.badge-not-hit {
  background: #dcfce7 !important;
  color: #166534 !important;
  border-color: #86efac !important;
}

.badge-suspected {
  background: #fff7ed !important;
  color: #9a3412 !important;
  border-color: #fdba74 !important;
}

.hit-alert-icon {
  font-size: 12px;
  flex-shrink: 0;
}

.processing-tag {
  overflow: hidden;
  position: relative;
}

.processing-tag::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent 0%, rgba(37, 99, 235, 0.3) 50%, transparent 100%);
  transform: translateX(-120%);
  animation: processingScan 1.2s linear infinite;
}

.text-muted {
  color: #98a2b3;
}

.date-text {
  color: #5c6676;
  font-size: 13px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
  font-size: 13px;
  transition: transform 120ms ease, border-color 120ms ease, color 120ms ease;
}

.detail-btn {
  height: 30px;
  padding: 0 10px;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.42);
  background: rgba(248, 250, 252, 0.9);
  color: #334155;
}

.detail-btn:hover {
  transform: translateX(1px);
  border-color: #2563eb;
  color: #1d4ed8;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(148, 163, 184, 0.16);
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .monitor-grid {
    grid-template-columns: 1fr 1fr;
  }

  .search-form {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input,
  .search-select {
    width: 100%;
  }

  .search-actions {
    margin-left: 0;
  }
}

@keyframes pulseMove {
  0%, 100% { transform: translateX(0); opacity: 0.65; }
  50% { transform: translateX(1px); opacity: 1; }
}

@keyframes processingScan {
  0% { transform: translateX(-120%); }
  100% { transform: translateX(120%); }
}

/* ===== 任务卡片列表样式 ===== */
.task-list-container {
  padding: 0;
}

.task-list-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 200px;
}

.task-card {
  position: relative;
  background: #ffffff;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.task-card:hover {
  transform: translateY(-2px);
  border-color: rgba(37, 99, 235, 0.3);
  box-shadow: 0 8px 24px rgba(15, 36, 82, 0.12), 0 2px 6px rgba(0, 0, 0, 0.04);
}

.task-card:active {
  transform: translateY(0);
}

/* 状态主题色边框 */
.task-status-pending { border-left: 3px solid #f59e0b; }
.task-status-processing { border-left: 3px solid #3b82f6; }
.task-status-completed { border-left: 3px solid #10b981; }
.task-status-failed { border-left: 3px solid #ef4444; }

/* 第一行：主信息区 */
.task-card-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.task-card-left {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

/* 状态指示器 */
.status-indicator {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.status-dot {
  position: absolute;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  animation: pulse 2s ease-in-out infinite;
}

.status-dot-small {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  margin-right: 6px;
}

.status-pending { color: #f59e0b; }
.status-processing { color: #3b82f6; }
.status-completed { color: #10b981; }
.status-failed { color: #ef4444; }

.status-icon {
  font-size: 18px;
  z-index: 1;
}

.status-icon.spinning {
  animation: spin 1s linear infinite;
}

/* 任务标识 */
.task-identity {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  flex: 1;
}

.task-call-id {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  letter-spacing: 0.01em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-analysis-type {
  font-size: 12px;
  color: #64748b;
  font-weight: 400;
}

/* 操作按钮 */
.task-card-actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transform: translateX(-8px);
  transition: all 0.15s ease;
}

.task-card:hover .task-card-actions {
  opacity: 1;
  transform: translateX(0);
}

.action-detail-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.4);
  background: rgba(248, 250, 252, 0.8);
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.action-detail-btn:hover {
  border-color: #2563eb;
  background: #2563eb;
  color: #fff;
  transform: translateX(2px);
}

/* 第二行：辅助信息 */
.task-card-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #64748b;
}

.meta-icon {
  font-size: 14px;
  color: #94a3b8;
}

.meta-label {
  color: #94a3b8;
}

.meta-value {
  color: #475569;
  font-weight: 500;
}

/* 第三行：执行信息 */
.task-card-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 12px;
  color: #94a3b8;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.info-icon {
  font-size: 13px;
}

.info-divider {
  color: #cbd5e1;
  font-size: 10px;
}

/* 第四行：结果标签区 */
.task-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-status-badge {
  display: flex;
  align-items: center;
}

.task-result-badge {
  display: flex;
  align-items: center;
}

.result-placeholder {
  color: #cbd5e1;
  font-size: 13px;
}

.result-not-hit {
  background: rgba(16, 185, 129, 0.08) !important;
  border-color: rgba(16, 185, 129, 0.3) !important;
  color: #059669 !important;
}

.result-hit {
  background: rgba(239, 68, 68, 0.08) !important;
  border-color: rgba(239, 68, 68, 0.3) !important;
  color: #dc2626 !important;
}

.result-suspected {
  background: rgba(245, 158, 11, 0.08) !important;
  border-color: rgba(245, 158, 11, 0.3) !important;
  color: #d97706 !important;
}

.result-icon {
  margin-right: 4px;
  font-size: 14px;
}

/* 空状态 */
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

/* ===== 紧凑样式 ===== */
.task-list-page {
  padding: 0;
}

.precision-pulse-page {
  display: grid;
  gap: 12px;
  font-family: var(--font-sans);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0;
}

.page-title-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
}

.page-subtitle {
  font-size: 12px;
  color: #6c7785;
  margin: 0;
}

.create-btn {
  height: 32px;
  padding-inline: 12px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 13px;
}

/* 紧凑统计条 */
.stats-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: rgba(248, 250, 252, 0.6);
  border-radius: 8px;
  margin-bottom: 12px;
}

.stat-item {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.stat-pending .stat-value { color: #f59e0b; }
.stat-processing .stat-value { color: #3b82f6; }
.stat-completed .stat-value { color: #10b981; }

.stat-divider {
  color: #cbd5e1;
  font-size: 12px;
}

/* 紧凑搜索区 */
.search-card {
  border-radius: 8px;
}

.search-card :deep(.el-card__body) {
  padding: 12px 16px;
}

.search-form-compact {
  display: flex;
  gap: 8px;
  margin: 0;
}

.search-item-compact {
  margin-bottom: 0;
}

.search-input-compact,
.search-select-compact {
  width: 140px;
}

.search-input-compact :deep(.el-input__wrapper),
.search-select-compact :deep(.el-select__wrapper) {
  height: 32px;
  border-radius: 6px;
  padding: 0 10px;
}

.search-input-compact :deep(.el-input__inner),
.search-select-compact :deep(.el-input__inner) {
  font-size: 13px;
}

.search-actions-compact {
  margin-left: auto;
  margin-bottom: 0;
  display: flex;
  gap: 8px;
}

.reset-btn-compact,
.search-btn-compact {
  height: 32px;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 500;
  border-radius: 6px;
}

.search-btn-compact {
  border: 1px solid #2563eb;
  background: #2563eb;
}

/* 紧凑任务列表 */
.table-card {
  border-radius: 8px;
  overflow: hidden;
}

.table-card :deep(.el-card__body) {
  padding: 12px;
}

.task-list-container {
  padding: 0;
}

.task-list-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 任务行 - 紧凑两行结构 */
.task-row {
  padding: 12px 16px;
  min-height: 80px;
  max-height: 96px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.task-row-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.task-row-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

/* 内联状态指示器 */
.status-indicator-inline {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.status-dot-inline {
  position: absolute;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  animation: pulse 2s ease-in-out infinite;
}

.status-icon-inline {
  font-size: 16px;
  z-index: 1;
}

.status-pending { color: #f59e0b; }
.status-processing { color: #3b82f6; }
.status-completed { color: #10b981; }
.status-failed { color: #ef4444; }

.task-call-id-inline {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.task-row-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.status-tag-compact,
.result-tag-compact {
  height: 22px;
  padding: 0 8px;
  font-size: 12px;
  border-radius: 4px;
  font-weight: 500;
}

.result-not-hit {
  background: rgba(16, 185, 129, 0.08) !important;
  border-color: rgba(16, 185, 129, 0.3) !important;
  color: #059669 !important;
}

.result-hit {
  background: rgba(239, 68, 68, 0.08) !important;
  border-color: rgba(239, 68, 68, 0.3) !important;
  color: #dc2626 !important;
}

.result-suspected {
  background: rgba(245, 158, 11, 0.08) !important;
  border-color: rgba(245, 158, 11, 0.3) !important;
  color: #d97706 !important;
}

/* 第二行：元数据 */
.task-row-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #64748b;
}

.meta-text {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-label {
  color: #94a3b8;
  font-size: 12px;
}

.meta-value {
  color: #475569;
  font-weight: 500;
}

.meta-divider {
  color: #cbd5e1;
  font-size: 10px;
}

/* 状态边框 */
.task-status-pending { border-left: 3px solid #f59e0b; }
.task-status-processing { border-left: 3px solid #3b82f6; }
.task-status-completed { border-left: 3px solid #10b981; }
.task-status-failed { border-left: 3px solid #ef4444; }

/* 空状态 */
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

/* 动画 */
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.1); }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.spinning {
  animation: spin 1s linear infinite;
}

/* 响应式 */
@media (max-width: 768px) {
  .task-row {
    padding: 10px 12px;
  }

  .task-row-main {
    flex-wrap: wrap;
    gap: 6px;
  }

  .task-row-meta {
    flex-wrap: wrap;
    gap: 6px;
  }

  .stats-bar {
    flex-wrap: wrap;
    gap: 12px;
  }

  .search-form-compact {
    flex-wrap: wrap;
  }

  .search-input-compact,
  .search-select-compact {
    width: 100%;
  }

  .search-actions-compact {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
