<template>
  <div class="event-detail-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-page-header @back="router.back()">
        <template #content>
          <span class="page-title">事件详情</span>
        </template>
        <template #extra>
          <el-button @click="loadData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </template>
      </el-page-header>
    </div>

    <el-skeleton :loading="loading" animated>
      <template #default>
        <el-row :gutter="20">
          <!-- 左侧 -->
          <el-col :span="16">
            <!-- 事件信息 -->
            <el-card shadow="never" class="info-card">
              <template #header>
                <div class="card-header">
                  <span>事件信息</span>
                  <el-tag :type="getStatusType(event.status)" size="large">
                    {{ getStatusText(event.status) }}
                  </el-tag>
                </div>
              </template>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="事件ID">{{ event.eventId }}</el-descriptions-item>
                <el-descriptions-item label="用户ID">{{ event.userId }}</el-descriptions-item>
                <el-descriptions-item label="Call ID">{{ event.callId }}</el-descriptions-item>
                <el-descriptions-item label="违规类型">
                  <el-tag type="danger">{{ event.violationName }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="置信度">
                  <el-progress 
                    :percentage="event.confidence * 100" 
                    :color="event.confidence > 0.85 ? '#f56c6c' : '#e6a23c'"
                    style="width: 150px"
                  />
                </el-descriptions-item>
                <el-descriptions-item label="发生时间">{{ formatDate(event.createdAt) }}</el-descriptions-item>
                <el-descriptions-item label="视频时段" :span="2">
                  {{ event.startSec }}s - {{ event.endSec }}s (共 {{ event.videoDuration }}s)
                </el-descriptions-item>
                <el-descriptions-item label="证据描述" :span="2">
                  {{ event.evidence }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>

            <!-- 视频片段 -->
            <el-card shadow="never" v-if="event.videoUrl">
              <template #header>
                <span>视频片段</span>
              </template>
              <video :src="event.videoUrl" controls class="video-player">
                您的浏览器不支持视频播放
              </video>
            </el-card>
          </el-col>

          <!-- 右侧 -->
          <el-col :span="8">
            <!-- 处理操作 -->
            <el-card shadow="never">
              <template #header>
                <span>处理操作</span>
              </template>
              <el-space direction="vertical" style="width: 100%">
                <el-button type="danger" style="width: 100%" @click="handleConfirm">
                  <el-icon><CircleCheck /></el-icon>
                  确认违规
                </el-button>
                <el-button type="success" style="width: 100%" @click="handleDismiss">
                  <el-icon><CircleClose /></el-icon>
                  驳回标记
                </el-button>
                <el-divider />
                <el-button style="width: 100%" @click="handleExport">
                  <el-icon><Download /></el-icon>
                  导出报告
                </el-button>
                <el-button style="width: 100%" @click="handleCopyLink">
                  <el-icon><Link /></el-icon>
                  复制链接
                </el-button>
              </el-space>
            </el-card>

            <!-- 用户画像 -->
            <el-card shadow="never" class="profile-card">
              <template #header>
                <span>用户画像</span>
              </template>
              <div class="profile-link" @click="router.push(`/profile/${event.userId}`)">
                <el-avatar :size="48">U</el-avatar>
                <div class="profile-info">
                  <div class="user-id">用户 {{ event.userId }}</div>
                  <el-text type="info">点击查看画像详情</el-text>
                </div>
                <el-icon><ArrowRight /></el-icon>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </template>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Refresh, CircleCheck, CircleClose, Download, Link, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { violationTypeMap } from '@/types/profile'

const router = useRouter()
const route = useRoute()
const loading = ref(false)

const event = ref({
  eventId: route.params.eventId as string,
  userId: 12345,
  callId: 'call_001',
  violationType: 'SILENT_ALL_TIME',
  violationName: '全程不说话',
  confidence: 0.85,
  evidence: '检测到全程不说话行为，置信度较高',
  startSec: 10,
  endSec: 120,
  videoDuration: 180,
  videoUrl: 'https://example.com/video.mp4',
  status: 'PENDING',
  createdAt: new Date().toISOString()
})

const statusMap: Record<string, { type: string; text: string }> = {
  PENDING: { type: 'warning', text: '待审核' },
  CONFIRMED: { type: 'danger', text: '已确认' },
  DISMISSED: { type: 'success', text: '已驳回' }
}

function getStatusType(status: string) { return statusMap[status]?.type || 'info' }
function getStatusText(status: string) { return statusMap[status]?.text || status }

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function loadData() {
  loading.value = true
  try {
    // 模拟加载
  } finally {
    loading.value = false
  }
}

function handleConfirm() {
  ElMessage.success('已确认违规')
}

function handleDismiss() {
  ElMessage.success('已驳回标记')
}

function handleExport() {
  ElMessage.success('报告已导出')
}

function handleCopyLink() {
  navigator.clipboard.writeText(window.location.href)
  ElMessage.success('链接已复制')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.event-detail-page {
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

.info-card {
  margin-bottom: 20px;
}

.video-player {
  width: 100%;
  border-radius: 8px;
}

.profile-card {
  margin-top: 20px;
}

.profile-link {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.profile-link:hover {
  background-color: #f5f7fa;
}

.profile-info {
  flex: 1;
}

.user-id {
  font-weight: 500;
  margin-bottom: 4px;
}
</style>