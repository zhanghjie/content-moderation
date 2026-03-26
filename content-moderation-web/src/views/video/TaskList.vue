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

    <el-card shadow="never" class="monitor-card">
      <div class="monitor-grid">
        <div v-for="item in statNodes" :key="item.key" class="monitor-cell" :class="`tone-${item.tone}`">
          <div class="monitor-top">
            <div class="monitor-icon">
              <el-icon :size="14">
                <component :is="item.icon" />
              </el-icon>
            </div>
            <span class="monitor-label">{{ item.label }}</span>
          </div>
          <div class="monitor-value mono">{{ item.value }}</div>
          <svg class="pulse-line" viewBox="0 0 120 28" preserveAspectRatio="none" aria-hidden="true">
            <path class="pulse-area" d="M0,24 C16,18 24,22 36,17 C52,13 60,21 72,15 C84,11 98,19 120,12 L120,28 L0,28 Z" />
            <path class="pulse-curve" d="M0,24 C16,18 24,22 36,17 C52,13 60,21 72,15 C84,11 98,19 120,12" />
          </svg>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="search-card">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="Call ID" class="precision-item">
          <el-input v-model="searchForm.callId" placeholder="请输入 Call ID" clearable class="search-input precision-input" />
        </el-form-item>
        <el-form-item label="任务状态" class="precision-item">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable class="search-select precision-input">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="违规命中" class="precision-item">
          <el-select v-model="searchForm.result" placeholder="请选择" clearable class="search-select precision-input">
            <el-option label="未命中违规" value="NOT_HIT" />
            <el-option label="命中违规" value="HIT" />
            <el-option label="疑似命中" value="SUSPECTED" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button @click="handleReset" class="reset-btn precision-action-btn">重置</el-button>
          <el-button type="primary" @click="handleSearch" class="search-btn precision-action-btn">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card precision-table-card">
      <el-table :data="tableData" v-loading="loading" style="width: 100%"
        :header-cell-style="{ background: 'rgba(22, 27, 35, 0.012)', color: 'var(--text-secondary)', fontWeight: '600' }">
        <el-table-column prop="taskId" label="Task ID" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="id-text">{{ row.taskId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="callId" label="Call ID" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="id-text">{{ row.callId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contentId" label="Content ID" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="id-text">{{ row.contentId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="任务状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="plain" size="small" round class="precision-tag" :class="[getStatusBadgeClass(row.status), { 'processing-tag': row.status === 'PROCESSING' }]">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="moderationResult" label="违规命中" min-width="120">
          <template #default="{ row }">
            <el-tag v-if="row.moderationResult" :type="getResultType(row.moderationResult)" effect="plain" size="small" round class="precision-tag" :class="getResultBadgeClass(row.moderationResult)">
              <el-icon v-if="isViolationHit(row.moderationResult)" class="hit-alert-icon"><WarningFilled /></el-icon>
              {{ getResultText(row.moderationResult) }}
            </el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="违规数" min-width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.violations?.filter((v: any) => v.detected).length" type="danger" effect="plain" size="small" round class="precision-tag">
              {{ row.violations.filter((v: any) => v.detected).length }}
            </el-tag>
            <span v-else class="zero-count mono">0</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160">
          <template #default="{ row }">
            <span class="date-text mono">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button class="action-btn detail-btn" @click="router.push(`/video/${row.callId}`)">
              详情 <el-icon><ArrowRight /></el-icon>
            </el-button>
            <el-button v-if="row.status === 'FAILED'" link class="retry-btn" @click="handleReAnalyze(row)">
              <el-icon><Refresh /></el-icon>
              重试
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search, Document, Clock, Loading, CircleCheck, Refresh, ArrowRight, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
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

const statNodes = computed(() => [
  { key: 'total', label: '总任务数', value: statistics.total, tone: 'primary', icon: Document },
  { key: 'pending', label: '待处理', value: statistics.pending, tone: 'warning', icon: Clock },
  { key: 'processing', label: '处理中', value: statistics.processing, tone: 'info', icon: Loading },
  { key: 'completed', label: '已完成', value: statistics.completed, tone: 'success', icon: CircleCheck }
])

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

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
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

function handleReAnalyze(row: VideoAnalysisTask) {
  ElMessage.info(`重新分析：${row.callId}`)
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

.zero-count {
  color: #047857;
  font-weight: 700;
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

.retry-btn {
  display: flex;
  align-items: center;
  gap: 2px;
  font-weight: 600;
  font-size: 13px;
  color: #d97706;
}

.retry-btn:hover {
  color: #b45309;
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
</style>
