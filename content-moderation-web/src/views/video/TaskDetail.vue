<template>
  <div class="task-detail-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-page-header @back="router.back()">
        <template #content>
          <span class="page-title">任务详情</span>
        </template>
        <template #extra>
          <el-button @click="loadData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button type="primary" @click="handleReAnalyze">
            <el-icon><RefreshRight /></el-icon>
            重新分析
          </el-button>
        </template>
      </el-page-header>
    </div>

    <el-skeleton :loading="loading" animated>
      <template #default>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-card shadow="never" class="info-card">
              <template #header>
                <div class="card-header">
                  <span>任务信息</span>
                  <el-space>
                    <el-tag :type="getStatusType(task.status)" size="small">
                      {{ getStatusText(task.status) }}
                    </el-tag>
                    <el-tag v-if="task.moderationResult" :type="getResultType(task.moderationResult)" size="small">
                      {{ getResultText(task.moderationResult) }}
                    </el-tag>
                  </el-space>
                </div>
              </template>
              <el-descriptions :column="3" border>
                <el-descriptions-item label="Task ID">{{ task.taskId }}</el-descriptions-item>
                <el-descriptions-item label="Call ID">{{ task.callId }}</el-descriptions-item>
                <el-descriptions-item label="Content ID">{{ task.contentId }}</el-descriptions-item>
                <el-descriptions-item label="分析类型">{{ task.analysisType || '-' }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ formatDate(task.createdAt) }}</el-descriptions-item>
                <el-descriptions-item label="完成时间">{{ formatDate(task.completedAt) }}</el-descriptions-item>
                <el-descriptions-item label="分析耗时">{{ task.analysisDuration || '-' }}</el-descriptions-item>
                <el-descriptions-item label="Prompt模块" :span="2">{{ task.promptModules || '-' }}</el-descriptions-item>
              </el-descriptions>
              <el-alert
                v-if="task.status === 'FAILED' && task.errorMessage"
                type="error"
                :closable="false"
                style="margin-top: 12px"
              >
                {{ task.errorMessage }}
              </el-alert>
            </el-card>
          </el-col>

          <el-col :span="16">

            <!-- 视频播放器 -->
            <el-card shadow="never" class="video-card" v-if="task.videoUrl">
              <template #header>
                <span>视频内容</span>
              </template>
              <video :src="task.videoUrl" controls class="video-player" :poster="task.coverUrl">
                您的浏览器不支持视频播放
              </video>
            </el-card>

            <!-- 违规检测 -->
            <el-card shadow="never" v-if="task.violations?.length">
              <template #header>
                <div class="card-header">
                  <span>违规检测</span>
                  <el-tag type="danger" size="small">
                    {{ task.violations.filter(v => v.detected).length }} 项违规
                  </el-tag>
                </div>
              </template>
              <div class="violations-toolbar">
                <el-switch v-model="onlyDetected" active-text="仅显示检测到" />
              </div>
              <el-table :data="displayViolations" style="width: 100%">
                <el-table-column prop="type" label="违规类型" width="150" />
                <el-table-column label="检测结果" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.detected ? 'danger' : 'success'" size="small">
                      {{ row.detected ? '检测到' : '未检测到' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="置信度" width="120">
                  <template #default="{ row }">
                    <el-progress 
                      :percentage="row.confidence * 100" 
                      :color="row.confidence > 0.8 ? '#f56c6c' : '#e6a23c'"
                      :stroke-width="10"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="时间" width="140">
                  <template #default="{ row }">
                    <span v-if="row.startSec != null && row.endSec != null">
                      {{ formatTimeRange(row.startSec, row.endSec) }}
                    </span>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column label="证据描述">
                  <template #default="{ row }">
                    <div class="evidence-text">{{ row.evidence || '-' }}</div>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>

            <el-card shadow="never" v-if="structuredPolicyResult" class="structured-card">
              <template #header>
                <div class="card-header">
                  <span>结构化输出</span>
                </div>
              </template>

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
            </el-card>

            <el-card shadow="never" v-if="task.resultJson" class="raw-card">
              <template #header>
                <div class="card-header">
                  <span>原始输出</span>
                  <el-button text @click="rawExpanded = !rawExpanded">
                    {{ rawExpanded ? '收起' : '展开' }}
                  </el-button>
                </div>
              </template>
              <pre v-if="rawExpanded" class="raw-json">{{ task.resultJson }}</pre>
            </el-card>
          </el-col>

          <!-- 右侧 -->
          <el-col :span="8">
            <!-- 摘要信息 -->
            <el-card shadow="never" class="summary-card" v-if="task.summary">
              <template #header>
                <span>分析摘要</span>
              </template>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="违规次数">
                  <el-tag type="danger">{{ task.summary.totalViolations }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="高置信度">
                  {{ task.summary.highConfidenceCount }}
                </el-descriptions-item>
                <el-descriptions-item label="主要违规">
                  {{ task.summary.primaryViolation || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="视频时长">
                  {{ task.summary.videoDurationSec != null ? `${task.summary.videoDurationSec}s` : '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="总体置信度">
                  {{ task.summary.overallConfidence != null ? task.summary.overallConfidence.toFixed(2) : (task.overallConfidence != null ? task.overallConfidence.toFixed(2) : '-') }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>

            <el-card shadow="never" class="status-card">
              <template #header>
                <span>任务状态</span>
              </template>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="当前状态">
                  <el-tag :type="getStatusType(task.status)" size="small">
                    {{ getStatusText(task.status) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="违规命中">
                  <el-tag v-if="task.moderationResult" :type="getResultType(task.moderationResult)" size="small">
                    {{ getResultText(task.moderationResult) }}
                  </el-tag>
                  <span v-else>-</span>
                </el-descriptions-item>
                <el-descriptions-item label="总体置信度">
                  {{ task.overallConfidence != null ? task.overallConfidence.toFixed(2) : '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="最后更新时间">
                  {{ formatDate(task.completedAt || task.createdAt) }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
        </el-row>
      </template>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Refresh, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { videoApi } from '@/api/video'
import type { VideoAnalysisTask } from '@/types/video'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const onlyDetected = ref(false)
const rawExpanded = ref(false)
let pollTimer: number | null = null

const task = ref<VideoAnalysisTask>({
  taskId: '',
  callId: route.params.callId as string,
  contentId: '',
  videoUrl: '',
  status: 'PENDING'
})

// 状态映射
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


function getStatusType(status: string) { return statusMap[status]?.type || 'info' }
function getStatusText(status: string) { return statusMap[status]?.text || status }
function getResultType(result: string) { return resultMap[result]?.type || 'info' }
function getResultText(result: string) { return resultMap[result]?.text || result }

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

function formatTimeRange(startSec: number, endSec: number) {
  return `${formatTime(startSec)} - ${formatTime(endSec)}`
}

function formatTime(seconds: number) {
  const s = Math.max(0, Math.floor(seconds))
  const mm = String(Math.floor(s / 60)).padStart(2, '0')
  const ss = String(s % 60).padStart(2, '0')
  return `${mm}:${ss}`
}

const displayViolations = computed(() => {
  const list = task.value.violations || []
  return onlyDetected.value ? list.filter(v => v.detected) : list
})
const structuredPolicyResult = computed(() => extractStructuredPolicyResult(task.value.resultJson))
const dedupedSummary = computed(() => buildDedupedSummary(structuredPolicyResult.value))

function extractStructuredPolicyResult(rawJson?: string) {
  const root = tryParseJsonObject(rawJson)
  if (!root) return null
  const candidates = collectStructuredCandidates(root)
  for (const candidate of candidates) {
    if (!isStructuredPolicyPayload(candidate)) continue
    return {
      sections: Array.isArray(candidate.sections) ? candidate.sections : [],
      summary: candidate.summary
    }
  }
  return null
}

function collectStructuredCandidates(root: any) {
  const candidates: any[] = []
  if (!isObjectRecord(root)) return candidates
  candidates.push(root)
  candidates.push(root.finalResult)
  candidates.push(root.output)
  candidates.push(root.result)
  candidates.push(root.analysisResult)
  for (const value of Object.values(root)) {
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

function prettifyKey(key: string) {
  return key
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/^./, char => char.toUpperCase())
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
  return `${formatTime(start)} - ${formatTime(end)}`
}

function formatStructuredText(value: any) {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'string') return value.trim() || '-'
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
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
  if (typeof value === 'string') return value.trim()
  if (value === null || value === undefined) return ''
  return String(value).trim()
}

function startPolling() {
  if (pollTimer != null) return
  pollTimer = window.setInterval(async () => {
    const status = task.value.status
    if (status !== 'PENDING' && status !== 'PROCESSING') {
      stopPolling()
      return
    }
    await loadData(true)
  }, 2000)
}

function stopPolling() {
  if (pollTimer != null) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
}

async function loadData(silent = false) {
  if (!silent) loading.value = true
  try {
    const result = await videoApi.getResult(task.value.callId)
    if (result) {
      task.value = result
      if (result.status === 'PENDING' || result.status === 'PROCESSING') {
        startPolling()
      } else {
        stopPolling()
      }
    }
  } catch (error) {
    if (!silent) ElMessage.error('加载失败')
  } finally {
    if (!silent) loading.value = false
  }
}

async function handleReAnalyze() {
  try {
    await ElMessageBox.confirm('确认重新分析该任务？', '重新分析', {
      type: 'warning',
      confirmButtonText: '重新分析',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  loading.value = true
  try {
    await videoApi.reAnalyze(task.value.callId)
    ElMessage.success('已触发重新分析')
    await loadData(true)
    startPolling()
  } catch (e) {
    ElMessage.error('重新分析失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.task-detail-page {
  padding: 0;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-card,
.video-card {
  margin-bottom: 20px;
}

.video-player {
  width: 100%;
  border-radius: 8px;
}

.violations-toolbar {
  margin-bottom: 12px;
  display: flex;
  justify-content: flex-end;
}

.evidence-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.4;
}

.raw-card {
  margin-top: 20px;
}

.structured-card {
  margin-top: 20px;
}

.summary-card {
  margin-bottom: 20px;
}

.status-card {
  margin-bottom: 20px;
}

.raw-json {
  margin: 0;
  padding: 12px;
  background: #0b1220;
  color: #e5e7eb;
  border-radius: 8px;
  overflow: auto;
  max-height: 420px;
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

.structured-text {
  margin-top: 8px;
  font-size: 13px;
  color: #0f172a;
  white-space: pre-wrap;
  line-height: 1.7;
}

.structured-text.emphasis {
  padding: 10px 12px;
  border-radius: 10px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
}

.timeline-table :deep(.cell) {
  white-space: pre-wrap;
  word-break: keep-all;
  line-break: auto;
  line-height: 1.65;
}

.timeline-text {
  white-space: pre-wrap;
  word-break: keep-all;
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
</style>
