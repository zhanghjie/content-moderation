<template>
  <div class="dashboard-page">
    <div class="home-banner">
      <div class="home-banner-inner">
        <div class="home-banner-main">
          <div class="home-banner-kicker">
            <span class="home-banner-kicker-dot" />
            <span class="home-banner-kicker-text">SKILL OS</span>
            <span class="home-banner-kicker-sep" />
            <span class="home-banner-kicker-text">PRECISION PULSE</span>
          </div>
          <div class="home-banner-title-row">
            <div class="home-banner-title">Anything Is Skill</div>
            <div class="home-banner-badges">
              <span class="home-banner-badge is-primary">LIVE</span>
              <span class="home-banner-badge">DASHBOARD</span>
            </div>
          </div>
          <div class="home-banner-subtitle">把能力做成 Skill，让编排更简单</div>
        </div>
        <div class="home-banner-metric">
          <div class="home-banner-metric-label">SYSTEM STATUS</div>
          <div class="home-banner-metric-value">ONLINE</div>
        </div>
      </div>
    </div>
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-wrapper">
        <h1 class="page-title">数据概览</h1>
        <p class="page-subtitle">实时监控内容风控系统运行状态和风险趋势</p>
      </div>
      <el-button 
        @click="handleRefresh" 
        class="refresh-btn" 
        round
        :loading="dashboardStore.loading"
      >
        <el-icon><Refresh /></el-icon>
        刷新数据
      </el-button>
    </div>

    <!-- 核心能力状态 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="24" :sm="12" :md="12" :lg="6">
        <el-card shadow="hover" class="stat-card stat-card-planner">
          <div class="stat-content">
            <div class="stat-header">
              <div class="stat-icon-wrapper gradient-primary">
                <el-icon :size="24"><Operation /></el-icon>
              </div>
              <el-tag type="primary" effect="plain" size="small" class="stat-tag">Planner</el-tag>
            </div>
            <div class="stat-body">
              <div class="stat-value">98.5%</div>
              <div class="stat-label">智能规划成功率</div>
            </div>
            <div class="stat-footer">
              <span class="trend trend-up">
                <el-icon><Top /></el-icon> 2.1%
              </span>
              <span class="trend-text">较昨日</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="12" :lg="6">
        <el-card shadow="hover" class="stat-card stat-card-executor">
          <div class="stat-content">
            <div class="stat-header">
              <div class="stat-icon-wrapper gradient-info">
                <el-icon :size="24"><Cpu /></el-icon>
              </div>
              <el-tag type="info" effect="plain" size="small" class="stat-tag">Executor</el-tag>
            </div>
            <div class="stat-body">
              <div class="stat-value info">1.2s</div>
              <div class="stat-label">平均解析延迟</div>
            </div>
            <div class="stat-footer">
              <span class="trend trend-up success">
                <el-icon><Bottom /></el-icon> 150ms
              </span>
              <span class="trend-text">性能提升</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="12" :lg="6">
        <el-card shadow="hover" class="stat-card stat-card-critic">
          <div class="stat-content">
            <div class="stat-header">
              <div class="stat-icon-wrapper gradient-success">
                <el-icon :size="24"><CircleCheck /></el-icon>
              </div>
              <el-tag type="success" effect="plain" size="small" class="stat-tag">Critic</el-tag>
            </div>
            <div class="stat-body">
              <div class="stat-value success">94.2%</div>
              <div class="stat-label">结果校验通过率</div>
            </div>
            <div class="stat-footer">
              <span class="trend trend-up">
                <el-icon><Top /></el-icon> 0.8%
              </span>
              <span class="trend-text">模型演进中</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="12" :lg="6">
        <el-card shadow="hover" class="stat-card stat-card-tag">
          <div class="stat-content">
            <div class="stat-header">
              <div class="stat-icon-wrapper gradient-warning">
                <el-icon :size="24"><CollectionTag /></el-icon>
              </div>
              <el-tag type="warning" effect="plain" size="small" class="stat-tag">ADS</el-tag>
            </div>
            <div class="stat-body">
              <div class="stat-value warning">1,280</div>
              <div class="stat-label">治理级标签产出</div>
            </div>
            <div class="stat-footer">
              <span class="trend trend-up warning">
                <el-icon><Top /></el-icon> 12%
              </span>
              <span class="trend-text">覆盖率提升</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 智能分析流水线 -->
    <el-card shadow="hover" class="pipeline-card chart-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <div class="title-icon gradient-primary">
              <el-icon><Connection /></el-icon>
            </div>
            <span>智能分析流水线监控</span>
          </div>
          <el-tag type="success" effect="dark" size="small" round>
            <span class="pulse-dot"></span> 实时运行中
          </el-tag>
        </div>
      </template>
      <div class="pipeline-wrapper">
        <div class="pipeline-step">
          <div class="step-icon-box gradient-primary">
            <el-icon><VideoCamera /></el-icon>
          </div>
          <div class="step-info">
            <div class="step-name">1. 任务接入</div>
            <div class="step-desc">ODS 层数据沉淀</div>
          </div>
          <div class="step-connector">
            <div class="connector-line"></div>
            <div class="connector-arrow"></div>
          </div>
        </div>
        <div class="pipeline-step">
          <div class="step-icon-box gradient-info">
            <el-icon><Operation /></el-icon>
          </div>
          <div class="step-info">
            <div class="step-name">2. 智能规划</div>
            <div class="step-desc">Planner 任务拆解</div>
          </div>
          <div class="step-connector">
            <div class="connector-line"></div>
            <div class="connector-arrow"></div>
          </div>
        </div>
        <div class="pipeline-step">
          <div class="step-icon-box gradient-warning">
            <el-icon><Cpu /></el-icon>
          </div>
          <div class="step-info">
            <div class="step-name">3. 多模态分析</div>
            <div class="step-desc">Executor 执行解析</div>
          </div>
          <div class="step-connector">
            <div class="connector-line"></div>
            <div class="connector-arrow"></div>
          </div>
        </div>
        <div class="pipeline-step">
          <div class="step-icon-box gradient-success">
            <el-icon><Connection /></el-icon>
          </div>
          <div class="step-info">
            <div class="step-name">4. 证据融合</div>
            <div class="step-desc">DWS 层语义建模</div>
          </div>
          <div class="step-connector">
            <div class="connector-line"></div>
            <div class="connector-arrow"></div>
          </div>
        </div>
        <div class="pipeline-step">
          <div class="step-icon-box gradient-danger">
            <el-icon><CollectionTag /></el-icon>
          </div>
          <div class="step-info">
            <div class="step-name">5. 标签生成</div>
            <div class="step-desc">ADS 层成果输出</div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 图表区域 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :sm="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <div class="title-icon gradient-primary">
                  <el-icon><User /></el-icon>
                </div>
                <span>服务质量多维概览</span>
              </div>
            </div>
          </template>
          <OrganicRadarChart :dimensions="radarDimensions" :height="420" class="chart-container radar-overview" />
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <div class="title-icon gradient-warning">
                  <el-icon><Histogram /></el-icon>
                </div>
                <span>违规类型分布</span>
              </div>
            </div>
          </template>
          <div ref="barChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图 -->
    <el-card shadow="hover" class="trend-card chart-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <div class="title-icon gradient-info">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <span>风险趋势</span>
          </div>
          <el-radio-group v-model="trendDays" size="small" @change="refreshLineChart">
            <el-radio-button :label="7">7 天</el-radio-button>
            <el-radio-button :label="15">15 天</el-radio-button>
            <el-radio-button :label="30">30 天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="lineChartRef" class="chart-container chart-container-lg"></div>
    </el-card>

    <!-- 列表区域 -->
    <el-row :gutter="16" class="list-row">
      <el-col :xs="24" :sm="24" :md="12" :lg="12">
        <el-card shadow="hover" class="table-card">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <div class="title-icon gradient-danger">
                  <el-icon><UserFilled /></el-icon>
                </div>
                <span>高风险用户</span>
              </div>
              <el-button type="primary" link class="view-all-btn" @click="router.push('/profile')">
                查看全部 <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </template>
          <el-table 
            :data="dashboardStore.highRiskUsers" 
            style="width: 100%" 
            :header-cell-style="{ background: 'var(--gray-50)', color: 'var(--text-secondary)', fontWeight: '600' }"
          >
            <el-table-column prop="userId" label="用户 ID" width="100" show-overflow-tooltip />
            <el-table-column prop="trustScore" label="信任分" width="80">
              <template #default="{ row }">
                <el-tag type="danger" effect="plain" size="small" round>{{ row.trustScore }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="totalViolations" label="违规次数" width="80">
              <template #default="{ row }">
                <span class="violation-count">{{ row.totalViolations }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="100">
              <template #default="{ row }">
                <el-button type="primary" link class="action-link" @click="router.push(`/profile/${row.userId}`)">
                  查看画像 <el-icon><ArrowRight /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="12" :lg="12">
        <el-card shadow="hover" class="table-card">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <div class="title-icon gradient-warning">
                  <el-icon><WarningFilled /></el-icon>
                </div>
                <span>最新违规事件</span>
              </div>
              <el-button type="primary" link class="view-all-btn" @click="router.push('/violations')">
                查看全部 <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </template>
          <el-table 
            :data="dashboardStore.recentViolations" 
            style="width: 100%" 
            :header-cell-style="{ background: 'var(--gray-50)', color: 'var(--text-secondary)', fontWeight: '600' }"
          >
            <el-table-column prop="violationName" label="违规类型" min-width="120" show-overflow-tooltip />
            <el-table-column prop="confidence" label="置信度" width="90">
              <template #default="{ row }">
                <el-tag :type="row.confidence >= 0.9 ? 'danger' : 'warning'" effect="plain" size="small" round>
                  {{ (row.confidence * 100).toFixed(0) }}%
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" width="140">
              <template #default="{ row }">
                <span class="time-text">{{ formatTime(row.createdAt) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  User, CircleCheck, Refresh,
  Histogram, TrendCharts, UserFilled,
  WarningFilled, ArrowRight, Top, Bottom,
  Operation, Cpu, CollectionTag, Connection, VideoCamera
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { useDashboardStore } from '@/stores/dashboard'
import OrganicRadarChart from '@/components/charts/OrganicRadarChart.vue'
import type { ProfileDimension } from '@/types/profile'

const router = useRouter()
const dashboardStore = useDashboardStore()

const trendDays = ref(30)
let lineChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

const barChartRef = ref<HTMLElement | null>(null)
const lineChartRef = ref<HTMLElement | null>(null)
const radarDimensions: ProfileDimension[] = [
  { code: 'service_attitude', name: '服务态度', score: 85, source: 'AI_ANALYSIS' },
  { code: 'professional', name: '专业度', score: 92, source: 'AI_ANALYSIS' },
  { code: 'response_speed', name: '响应速度', score: 78, source: 'AI_ANALYSIS' },
  { code: 'compliance', name: '合规性', score: 71, source: 'AI_ANALYSIS' },
  { code: 'stability', name: '稳定性', score: 83, source: 'AI_ANALYSIS' },
  { code: 'content_quality', name: '内容质量', score: 88, source: 'AI_ANALYSIS' }
]

// 格式化时间
function formatTime(dateStr: string) {
  return new Date(dateStr).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 刷新数据
function handleRefresh() {
  dashboardStore.refreshAllData()
}

// 初始化图表
function initCharts() {
  initBarChart()
  initLineChart()
}

// 初始化柱状图
function initBarChart() {
  if (!barChartRef.value) return
  barChart = echarts.init(barChartRef.value)

  const option = {
    tooltip: { 
      trigger: 'axis', 
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e5e7eb',
      textStyle: { color: '#374151' }
    },
    grid: { left: '4%', right: '4%', bottom: '8%', top: '8%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: ['敏感言论', '广告推广', '不当内容', '虚假信息', '其他'], 
      axisLabel: { 
        rotate: 30,
        color: '#6b7280',
        fontSize: 12
      },
      axisLine: { lineStyle: { color: '#e5e7eb' } }
    },
    yAxis: { 
      type: 'value',
      axisLabel: { color: '#6b7280' },
      splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } }
    },
    series: [{
      data: [120, 89, 76, 54, 32],
      type: 'bar',
      barWidth: '50%',
      itemStyle: {
        borderRadius: [8, 8, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#0ea5e9' },
          { offset: 1, color: '#7dd3fc' }
        ])
      }
    }]
  }

  barChart.setOption(option)
}

// 初始化折线图
function initLineChart() {
  if (!lineChartRef.value) return
  if (lineChart) lineChart.dispose()
  lineChart = echarts.init(lineChartRef.value)

  const dates = Array.from({ length: trendDays.value }, (_, i) => {
    const date = new Date()
    date.setDate(date.getDate() - (trendDays.value - 1 - i))
    return date.toISOString().slice(5, 10)
  })

  const option = {
    tooltip: { 
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e5e7eb',
      textStyle: { color: '#374151' }
    },
    legend: { 
      data: ['高风险', '中风险', '低风险'],
      bottom: '0%',
      textStyle: { color: '#6b7280' }
    },
    grid: { left: '4%', right: '4%', bottom: '12%', top: '8%', containLabel: true },
    xAxis: { 
      type: 'category', 
      boundaryGap: false, 
      data: dates,
      axisLabel: { color: '#6b7280' },
      axisLine: { lineStyle: { color: '#e5e7eb' } }
    },
    yAxis: { 
      type: 'value',
      axisLabel: { color: '#6b7280' },
      splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } }
    },
    series: [
      { 
        name: '高风险', 
        type: 'line', 
        smooth: true,
        lineStyle: { width: 3, color: '#ef4444' },
        itemStyle: { color: '#ef4444' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(239,68,68,0.2)' },
            { offset: 1, color: 'rgba(239,68,68,0.02)' }
          ])
        },
        data: Array.from({ length: trendDays.value }, () => Math.floor(Math.random() * 50) + 200)
      },
      { 
        name: '中风险', 
        type: 'line', 
        smooth: true,
        lineStyle: { width: 3, color: '#f59e0b' },
        itemStyle: { color: '#f59e0b' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(245,158,11,0.2)' },
            { offset: 1, color: 'rgba(245,158,11,0.02)' }
          ])
        },
        data: Array.from({ length: trendDays.value }, () => Math.floor(Math.random() * 100) + 800)
      },
      { 
        name: '低风险', 
        type: 'line', 
        smooth: true,
        lineStyle: { width: 3, color: '#10b981' },
        itemStyle: { color: '#10b981' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(16,185,129,0.2)' },
            { offset: 1, color: 'rgba(16,185,129,0.02)' }
          ])
        },
        data: Array.from({ length: trendDays.value }, () => Math.floor(Math.random() * 200) + 11000)
      }
    ]
  }

  lineChart.setOption(option)
}

function refreshLineChart() {
  initLineChart()
}

// 窗口大小变化
function handleResize() {
  barChart?.resize()
  lineChart?.resize()
}

// 生命周期
onMounted(() => {
  dashboardStore.refreshAllData()
  initCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  lineChart?.dispose()
})
</script>

<style scoped>
/* 样式与之前相同，省略以保持简洁 */
@import './dashboard-styles.css';
</style>
