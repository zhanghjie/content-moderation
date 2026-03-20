<template>
  <div class="profile-detail-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-page-header @back="router.back()">
        <template #content>
          <span class="page-title">服务质量画像</span>
        </template>
        <template #extra>
          <el-space>
            <el-button type="primary" plain size="small" :disabled="!profile" @click="handleExport">
              <el-icon><Download /></el-icon> 导出画像报告
            </el-button>
            <el-tag v-if="profile" :type="userTypeTagType" effect="plain">
              {{ userTypeText }}
            </el-tag>
          </el-space>
        </template>
      </el-page-header>
    </div>

    <el-skeleton :loading="loading" animated :rows="10">
      <template #default>
        <template v-if="profile">
          <!-- 用户概览卡片 -->
          <el-card shadow="never" class="overview-card">
            <div class="overview-content">
              <div class="user-info">
                <el-avatar 
                  :size="80" 
                  :style="{ backgroundColor: riskColor }"
                >
                  {{ profile.userId.toString().slice(-2) }}
                </el-avatar>
                <div class="user-meta">
                  <h2>{{ profile.nickname }}</h2>
                  <div class="meta-row">
                    <el-tag :type="riskTagType" effect="dark" size="large">
                      {{ riskText }}
                    </el-tag>
                    <span class="trust-score">信任分: <strong>{{ profile.riskProfile.trustScore }}</strong></span>
                  </div>
                  <div class="meta-row sub">
                    <span>用户ID: {{ profile.userId }}</span>
                    <span>首次活跃: {{ formatDate(profile.basicInfo.firstCallAt) }}</span>
                    <span>总通话: {{ profile.basicInfo.totalCalls }} 次</span>
                  </div>
                </div>
              </div>
              <div class="score-ring">
                <el-progress 
                  type="dashboard" 
                  :percentage="profile.riskProfile.trustScore" 
                  :color="riskColor"
                  :width="120"
                >
                  <template #default="{ percentage }">
                    <span class="percentage-value">{{ percentage }}</span>
                    <span class="percentage-label">信任分</span>
                  </template>
                </el-progress>
              </div>
            </div>
          </el-card>

          <!-- P1: 动态雷达图 + AI标签 -->
          <el-row :gutter="20" class="main-row">
            <!-- 动态雷达图 -->
            <el-col :xs="24" :md="12">
              <el-card shadow="never" class="radar-card">
                <template #header>
                  <div class="card-header">
                    <span class="card-title">
                      <el-icon><DataAnalysis /></el-icon>
                      画像维度
                    </span>
                    <el-tag type="info" effect="plain" size="small">
                      AI 分析 · {{ profile.dimensions.length }} 个维度
                    </el-tag>
                  </div>
                </template>
                
                <!-- 雷达图 -->
                <OrganicRadarChart :dimensions="profile.dimensions" :height="500" class="radar-chart" />
                
                <!-- 维度详情列表 -->
                <el-divider content-position="left">
                  <span class="divider-text">维度详情</span>
                </el-divider>
                <div class="dimension-list">
                  <div 
                    v-for="dim in profile.dimensions" 
                    :key="dim.code" 
                    class="dimension-item"
                  >
                    <div class="dim-header">
                      <span class="dim-name">{{ dim.name }}</span>
                      <span class="dim-score" :style="{ color: getScoreColor(dim.score) }">
                        {{ dim.score }}
                      </span>
                    </div>
                    <el-progress 
                      :percentage="dim.score" 
                      :color="getScoreColor(dim.score)"
                      :stroke-width="8"
                      :show-text="false"
                    />
                    <div class="dim-meta">
                      <span class="dim-desc">{{ dim.description }}</span>
                      <el-tag size="small" effect="plain" class="dim-source">
                        {{ formatSource(dim.source) }}
                      </el-tag>
                    </div>
                  </div>
                </div>
              </el-card>
            </el-col>
            
            <!-- AI标签展示 -->
            <el-col :xs="24" :md="12">
              <el-card shadow="never" class="tags-card">
                <template #header>
                  <div class="card-header">
                    <span class="card-title">
                      <el-icon><PriceTag /></el-icon>
                      AI 标签
                    </span>
                    <el-tag type="info" effect="plain" size="small">
                      共 {{ profile.tags.length }} 个标签
                    </el-tag>
                  </div>
                </template>
                
                <!-- 按类别分组展示 -->
                <div class="tags-container">
                  <div 
                    v-for="category in tagCategories" 
                    :key="category" 
                    class="tag-group"
                  >
                    <div class="category-header">
                      <span class="category-name">{{ category }}</span>
                      <span class="category-count">{{ getTagsByCategory(category).length }}</span>
                    </div>
                    <div class="tag-list">
                      <el-tooltip 
                        v-for="tag in getTagsByCategory(category)" 
                        :key="tag.code"
                        placement="top"
                      >
                        <template #content>
                          <div class="tag-tooltip">
                            <div>置信度: <strong>{{ (tag.confidence * 100).toFixed(0) }}%</strong></div>
                            <div v-if="tag.evidence">依据: {{ tag.evidence }}</div>
                            <div>来源: {{ tag.source }}</div>
                          </div>
                        </template>
                        <el-tag 
                          :type="getTagType(tag.category)" 
                          effect="plain"
                          class="tag-item"
                        >
                          <el-icon v-if="tag.confidence > 0.9" class="tag-icon"><CircleCheck /></el-icon>
                          {{ tag.name }}
                        </el-tag>
                      </el-tooltip>
                    </div>
                  </div>
                  
                  <!-- 无标签提示 -->
                  <el-empty 
                    v-if="profile.tags.length === 0" 
                    description="暂无标签数据" 
                    :image-size="60"
                  />
                </div>
              </el-card>
            </el-col>
          </el-row>

          <!-- P2: 多源证据链可视化 -->
          <el-card shadow="never" class="evidence-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">
                  <el-icon><Connection /></el-icon>
                  多源证据融合链 (DWS层)
                </span>
              </div>
            </template>
            <div class="evidence-timeline">
              <el-timeline>
                <el-timeline-item timestamp="语音解析 (ASR)" placement="top" type="primary">
                  <el-card shadow="hover" class="evidence-sub-card">
                    <p class="evidence-text">“...没关系，我在这陪着你，你可以慢慢说...”</p>
                    <div class="evidence-meta">
                      <el-tag size="small" type="info">情绪评分: 92 (温暖)</el-tag>
                      <el-tag size="small" type="success">置信度: 0.98</el-tag>
                    </div>
                  </el-card>
                </el-timeline-item>
                <el-timeline-item timestamp="图像识别 (CV)" placement="top" type="warning">
                  <el-card shadow="hover" class="evidence-sub-card">
                    <p class="evidence-text">画面检测到主播面带微笑，出镜率 100%，环境明亮。</p>
                    <div class="evidence-meta">
                      <el-tag size="small" type="warning">表情: 愉悦</el-tag>
                      <el-tag size="small" type="success">置信度: 0.95</el-tag>
                    </div>
                  </el-card>
                </el-timeline-item>
                <el-timeline-item timestamp="语义建模 (LLM)" placement="top" type="success">
                  <el-card shadow="hover" class="evidence-sub-card">
                    <p class="evidence-text">综合语音与图像，判定服务风格为“情感陪伴型”。主播表现出极高的热情度和专业度。</p>
                    <div class="evidence-meta">
                      <el-tag size="small" type="danger">风险评估: 极低</el-tag>
                      <el-tag size="small" type="success">结论一致性: 100%</el-tag>
                    </div>
                  </el-card>
                </el-timeline-item>
              </el-timeline>
            </div>
          </el-card>

          <!-- 风险画像 -->
          <el-card shadow="never" class="risk-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">风险画像</span>
              </div>
            </template>
            
            <el-row :gutter="24">
              <!-- 左侧：风险指标 -->
              <el-col :xs="24" :md="8">
                <div class="risk-indicators">
                  <div class="indicator-item">
                    <div class="indicator-label">风险等级</div>
                    <div class="indicator-value">
                      <el-tag :type="riskTagType" effect="dark" size="large">
                        {{ riskText }}
                      </el-tag>
                    </div>
                  </div>
                  <div class="indicator-item">
                    <div class="indicator-label">风险趋势</div>
                    <div class="indicator-value">
                      <el-tag :type="trendTagType" effect="plain">
                        <el-icon style="margin-right: 4px">
                          <component :is="trendIcon" />
                        </el-icon>
                        {{ trendText }}
                      </el-tag>
                    </div>
                  </div>
                  <div class="indicator-item">
                    <div class="indicator-label">信任分</div>
                    <div class="indicator-value">
                      <span class="score-large" :style="{ color: riskColor }">
                        {{ profile.riskProfile.trustScore }}
                      </span>
                      <span class="score-max">/ 100</span>
                    </div>
                  </div>
                </div>
              </el-col>
              
              <!-- 右侧：风险说明 -->
              <el-col :xs="24" :md="16">
                <div class="risk-description">
                  <div class="desc-title">风险评估说明</div>
                  <div class="desc-content">
                    <template v-if="profile.riskProfile.riskLevel === 'HIGH'">
                      <el-alert type="error" :closable="false" show-icon>
                        <template #title>
                          该用户存在较高风险，建议重点关注
                        </template>
                        <template #default>
                          根据历史分析数据，该用户存在多次违规记录，信任分较低，建议加强审核力度。
                        </template>
                      </el-alert>
                    </template>
                    <template v-else-if="profile.riskProfile.riskLevel === 'MEDIUM'">
                      <el-alert type="warning" :closable="false" show-icon>
                        <template #title>
                          该用户存在一定风险，建议适度关注
                        </template>
                        <template #default>
                          根据历史分析数据，该用户有少量违规记录，建议保持正常审核流程。
                        </template>
                      </el-alert>
                    </template>
                    <template v-else>
                      <el-alert type="success" :closable="false" show-icon>
                        <template #title>
                          该用户风险较低，表现良好
                        </template>
                        <template #default>
                          根据历史分析数据，该用户合规性良好，无或极少违规记录。
                        </template>
                      </el-alert>
                    </template>
                  </div>
                </div>
              </el-col>
            </el-row>
          </el-card>

          <!-- 基础信息 -->
          <el-card shadow="never" class="info-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">基础信息</span>
              </div>
            </template>
            
            <el-descriptions :column="3" border>
              <el-descriptions-item label="用户ID">
                {{ profile.userId }}
              </el-descriptions-item>
              <el-descriptions-item label="用户类型">
                <el-tag :type="profile.userType === 'STREAMER' ? '' : 'info'" effect="plain">
                  {{ profile.userType === 'STREAMER' ? '主播' : '普通用户' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="昵称">
                {{ profile.nickname || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="首次活跃">
                {{ formatDateTime(profile.basicInfo.firstCallAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="最后活跃">
                {{ formatDateTime(profile.basicInfo.lastCallAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="注册时间">
                {{ formatDateTime(profile.basicInfo.registerAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="总通话次数">
                <strong>{{ profile.basicInfo.totalCalls }}</strong> 次
              </el-descriptions-item>
              <el-descriptions-item label="总通话时长">
                {{ formatDuration(profile.basicInfo.totalCallDuration) }}
              </el-descriptions-item>
              <el-descriptions-item label="平均通话时长">
                {{ formatDuration(profile.behaviorStats.avgCallDuration) }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 统计数据 -->
          <el-row :gutter="16" class="stats-row">
            <el-col :xs="24" :md="12">
              <el-card shadow="never">
                <template #header>
                  <div class="card-header">
                    <span class="card-title">违规统计</span>
                  </div>
                </template>
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="总违规次数">
                    <el-tag 
                      :type="profile.violationStats.total > 0 ? 'danger' : 'success'" 
                      effect="dark"
                    >
                      {{ profile.violationStats.total }} 次
                    </el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="高置信度违规">
                    {{ profile.violationStats.highConfidence }} 次
                  </el-descriptions-item>
                  <el-descriptions-item label="平均置信度">
                    {{ (profile.violationStats.avgConfidence * 100).toFixed(0) }}%
                  </el-descriptions-item>
                  <el-descriptions-item label="近期趋势">
                    <el-tag :type="getTrendType(profile.violationStats.recentTrend)" effect="plain">
                      {{ getTrendText(profile.violationStats.recentTrend) }}
                    </el-tag>
                  </el-descriptions-item>
                </el-descriptions>
              </el-card>
            </el-col>
            
            <el-col :xs="24" :md="12">
              <el-card shadow="never">
                <template #header>
                  <div class="card-header">
                    <span class="card-title">行为统计</span>
                  </div>
                </template>
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="总通话">
                    {{ profile.basicInfo.totalCalls }} 次
                  </el-descriptions-item>
                  <el-descriptions-item label="平均时长">
                    {{ formatDuration(profile.behaviorStats.avgCallDuration) }}
                  </el-descriptions-item>
                  <el-descriptions-item label="违规率">
                    <el-tag 
                      :type="profile.behaviorStats.violationRate > 0.2 ? 'danger' : 'success'" 
                      effect="plain"
                    >
                      {{ (profile.behaviorStats.violationRate * 100).toFixed(1) }}%
                    </el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="活跃时段">
                    {{ profile.behaviorStats.peakHours.slice(0, 3).map(h => `${h}:00`).join(', ') }}
                  </el-descriptions-item>
                </el-descriptions>
              </el-card>
            </el-col>
          </el-row>
        </template>
        
        <!-- 空状态 -->
        <el-empty v-else description="用户画像不存在" />
      </template>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Top, Bottom, Minus, DataAnalysis, PriceTag, CircleCheck, Download, Connection } from '@element-plus/icons-vue'
import { profileApi } from '@/api/profile'
import type { ProfileDetail, UserTag } from '@/types/profile'
import OrganicRadarChart from '@/components/charts/OrganicRadarChart.vue'

const router = useRouter()
const route = useRoute()

const userId = computed(() => Number(route.params.userId))
const loading = ref(false)
const profile = ref<ProfileDetail | null>(null)

// 用户类型
const userTypeTagType = computed(() => 
  profile.value?.userType === 'STREAMER' ? '' : 'info'
)
const userTypeText = computed(() => 
  profile.value?.userType === 'STREAMER' ? '主播' : '普通用户'
)

// 风险配置
const riskConfig = {
  HIGH: { type: 'danger' as const, text: '高风险', color: '#f56c6c' },
  MEDIUM: { type: 'warning' as const, text: '中风险', color: '#e6a23c' },
  LOW: { type: 'success' as const, text: '低风险', color: '#67c23a' }
}

const riskTagType = computed(() => 
  profile.value ? riskConfig[profile.value.riskProfile.riskLevel].type : 'info'
)
const riskText = computed(() => 
  profile.value ? riskConfig[profile.value.riskProfile.riskLevel].text : ''
)
const riskColor = computed(() => 
  profile.value ? riskConfig[profile.value.riskProfile.riskLevel].color : '#909399'
)

// 趋势
const trendTagType = computed(() => {
  if (!profile.value) return 'info'
  const trend = profile.value.riskProfile.riskTrend
  if (trend === 'IMPROVING') return 'success'
  if (trend === 'WORSENING') return 'danger'
  return 'info'
})

const trendText = computed(() => {
  if (!profile.value) return ''
  const map: Record<string, string> = {
    IMPROVING: '改善中',
    STABLE: '稳定',
    WORSENING: '恶化中'
  }
  return map[profile.value.riskProfile.riskTrend] || ''
})

const trendIcon = computed(() => {
  if (!profile.value) return Minus
  const trend = profile.value.riskProfile.riskTrend
  if (trend === 'IMPROVING') return Top
  if (trend === 'WORSENING') return Bottom
  return Minus
})

// 标签类别
const tagCategories = computed(() => {
  if (!profile.value) return []
  const categories = new Set(profile.value.tags.map(t => t.category))
  return Array.from(categories)
})

function getTagsByCategory(category: string): UserTag[] {
  return profile.value?.tags.filter(t => t.category === category) || []
}

function getTagType(category: string): 'danger' | 'warning' | 'success' | 'info' | '' {
  const map: Record<string, 'danger' | 'warning' | 'success' | 'info' | ''> = {
    '风险特征': 'danger',
    '行为特征': 'warning',
    '内容风格': 'info',
    '互动方式': 'success',
    '专业能力': 'info',
    '消费特征': ''
  }
  return map[category] || ''
}

// 工具函数
function getScoreColor(score: number): string {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#409eff'
  if (score >= 40) return '#e6a23c'
  return '#f56c6c'
}

function formatSource(source: string): string {
  const map: Record<string, string> = {
    'VIDEO_ANALYSIS': '视频分析',
    'BEHAVIOR_ANALYSIS': '行为分析',
    'AI_ANALYSIS': 'AI分析'
  }
  return map[source] || source
}

function getTrendType(trend: string): 'danger' | 'warning' | 'success' {
  const map: Record<string, 'danger' | 'warning' | 'success'> = {
    INCREASING: 'danger',
    STABLE: 'warning',
    DECREASING: 'success'
  }
  return map[trend] || 'warning'
}

function getTrendText(trend: string): string {
  const map: Record<string, string> = {
    INCREASING: '上升',
    STABLE: '稳定',
    DECREASING: '下降'
  }
  return map[trend] || trend
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
}

function formatDateTime(dateStr: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function formatDuration(seconds: number): string {
  if (!seconds) return '-'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  
  if (hours > 0) {
    return `${hours}小时${minutes}分钟`
  }
  if (minutes > 0) {
    return `${minutes}分钟${secs}秒`
  }
  return `${secs}秒`
}

function handleExport() {
  if (!profile.value) return
  const content = JSON.stringify(profile.value, null, 2)
  const blob = new Blob([content], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `profile-${profile.value.userId}.json`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

// 加载数据
async function loadProfile() {
  loading.value = true
  try {
    profile.value = await profileApi.getDetail(userId.value)
  } catch (error) {
    console.error('加载失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.profile-detail-page {
  padding: 0;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

/* 概览卡片 */
.overview-card {
  margin-bottom: 20px;
}

.overview-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-meta h2 {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.meta-row.sub {
  color: #909399;
  font-size: 13px;
  gap: 16px;
}

.trust-score {
  color: #606266;
}

.trust-score strong {
  font-size: 18px;
  color: #303133;
}

.score-ring {
  text-align: center;
}

.percentage-value {
  display: block;
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.percentage-label {
  display: block;
  font-size: 12px;
  color: #909399;
}

/* 主行 */
.main-row {
  margin-bottom: 20px;
}

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

/* 雷达图卡片 */
.radar-card {
  height: 100%;
}

.radar-chart {
  width: 100%;
  min-height: 500px;
}

.divider-text {
  font-size: 13px;
  color: #909399;
}

.dimension-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.dimension-item {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.dimension-item:last-child {
  border-bottom: none;
}

.dim-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.dim-name {
  font-weight: 500;
  color: #303133;
}

.dim-score {
  font-size: 16px;
  font-weight: 600;
}

.dim-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}

.dim-desc {
  font-size: 12px;
  color: #909399;
}

.dim-source {
  font-size: 11px;
}

/* 标签卡片 */
.tags-card {
  height: 100%;
}

.tags-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 450px;
  overflow-y: auto;
}

.tag-group {
  padding: 8px 0;
}

.category-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #f0f0f0;
}

.category-name {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.category-count {
  font-size: 12px;
  color: #fff;
  background: #909399;
  padding: 1px 8px;
  border-radius: 10px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  cursor: pointer;
  transition: all 0.2s;
}

.tag-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.tag-icon {
  margin-right: 4px;
}

.tag-tooltip {
  font-size: 13px;
  line-height: 1.6;
}

/* 证据链卡片 */
.evidence-card {
  margin-bottom: 20px;
}

.evidence-timeline {
  padding: 20px 0;
}

.evidence-sub-card {
  border: 1px dashed var(--border-medium);
}

.evidence-text {
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 12px;
  line-height: 1.6;
}

.evidence-meta {
  display: flex;
  gap: 12px;
}

/* 风险卡片 */
.risk-card {
  margin-bottom: 20px;
}

.risk-indicators {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.indicator-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.indicator-label {
  font-size: 13px;
  color: #909399;
}

.indicator-value {
  display: flex;
  align-items: center;
}

.score-large {
  font-size: 32px;
  font-weight: 600;
}

.score-max {
  font-size: 14px;
  color: #909399;
  margin-left: 4px;
}

.risk-description {
  height: 100%;
}

.desc-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 12px;
}

/* 基础信息卡片 */
.info-card {
  margin-bottom: 20px;
}

/* 统计行 */
.stats-row {
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .overview-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .meta-row.sub {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }

  .main-row,
  .stats-row {
    margin-bottom: 12px;
  }

  .evidence-meta {
    flex-wrap: wrap;
    gap: 8px;
  }
}
</style>
