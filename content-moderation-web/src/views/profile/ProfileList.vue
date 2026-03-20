<template>
  <div class="profile-list-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-wrapper">
        <h1 class="page-title">用户画像</h1>
        <p class="page-subtitle">基于视频分析数据的用户风险评估与管理</p>
      </div>
      <el-button @click="loadData" class="refresh-btn" round>
        <el-icon><Refresh /></el-icon>
        刷新数据
      </el-button>
    </div>

    <!-- 搜索表单 -->
    <el-card shadow="hover" class="search-card">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="用户类型">
          <el-select v-model="searchForm.userType" placeholder="全部" clearable class="search-select">
            <el-option label="主播" value="STREAMER" />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="searchForm.riskLevel" placeholder="全部" clearable class="search-select">
            <el-option label="高风险" value="HIGH" />
            <el-option label="中风险" value="MEDIUM" />
            <el-option label="低风险" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="信任分">
          <div class="score-range">
            <el-input-number
              v-model="searchForm.minScore"
              :min="0"
              :max="100"
              :controls="false"
              placeholder="最低"
              class="score-input"
            />
            <span class="range-separator">~</span>
            <el-input-number
              v-model="searchForm.maxScore"
              :min="0"
              :max="100"
              :controls="false"
              placeholder="最高"
              class="score-input"
            />
          </div>
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button @click="handleReset" class="reset-btn">重置</el-button>
          <el-button type="primary" @click="handleSearch" class="search-btn">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 统计卡片 - 响应式布局 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :xs="24" :sm="12" :md="12" :lg="6">
        <el-card shadow="hover" class="stat-card stat-card-total">
          <div class="stat-content">
            <div class="stat-header">
              <div class="stat-icon-wrapper gradient-primary">
                <el-icon :size="24"><User /></el-icon>
              </div>
            </div>
            <div class="stat-body">
              <div class="stat-value">{{ statistics.total }}</div>
              <div class="stat-label">总用户数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="12" :lg="6">
        <el-card shadow="hover" class="stat-card stat-card-high">
          <div class="stat-content">
            <div class="stat-header">
              <div class="stat-icon-wrapper gradient-danger">
                <el-icon :size="24"><Warning /></el-icon>
              </div>
              <el-tag type="danger" effect="plain" size="small" class="stat-badge">{{ statistics.highRisk }}</el-tag>
            </div>
            <div class="stat-body">
              <div class="stat-value danger">{{ statistics.highRisk }}</div>
              <div class="stat-label">高风险</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="12" :lg="6">
        <el-card shadow="hover" class="stat-card stat-card-medium">
          <div class="stat-content">
            <div class="stat-header">
              <div class="stat-icon-wrapper gradient-warning">
                <el-icon :size="24"><InfoFilled /></el-icon>
              </div>
              <el-tag type="warning" effect="plain" size="small" class="stat-badge">{{ statistics.mediumRisk }}</el-tag>
            </div>
            <div class="stat-body">
              <div class="stat-value warning">{{ statistics.mediumRisk }}</div>
              <div class="stat-label">中风险</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="12" :lg="6">
        <el-card shadow="hover" class="stat-card stat-card-low">
          <div class="stat-content">
            <div class="stat-header">
              <div class="stat-icon-wrapper gradient-success">
                <el-icon :size="24"><CircleCheck /></el-icon>
              </div>
              <el-tag type="success" effect="plain" size="small" class="stat-badge">{{ statistics.lowRisk }}</el-tag>
            </div>
            <div class="stat-body">
              <div class="stat-value success">{{ statistics.lowRisk }}</div>
              <div class="stat-label">低风险</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <el-card shadow="hover" class="table-card">
      <el-table
        :data="tableData"
        v-loading="loading"
        style="width: 100%"
        :header-cell-style="{ background: 'var(--gray-50)', color: 'var(--text-secondary)', fontWeight: '600' }"
        :row-style="{ transition: 'all 0.2s' }"
      >
        <!-- 用户信息 -->
        <el-table-column label="用户" min-width="220">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar
                :size="44"
                :style="{ background: getRiskColor(row.riskLevel) }"
                class="user-avatar"
              >
                {{ row.userId.toString().slice(-2) }}
              </el-avatar>
              <div class="user-info">
                <div class="nickname">{{ row.nickname }}</div>
                <div class="user-meta">
                  <span class="user-id">ID: {{ row.userId }}</span>
                  <el-tag
                    :type="row.userType === 'STREAMER' ? 'primary' : 'info'"
                    size="small"
                    effect="plain"
                    round
                    class="user-type-tag"
                  >
                    {{ row.userType === 'STREAMER' ? '主播' : '用户' }}
                  </el-tag>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 风险等级 -->
        <el-table-column label="风险等级" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getRiskTagType(row.riskLevel)" effect="plain" size="small" round>
              {{ getRiskText(row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 信任分 -->
        <el-table-column label="信任分" width="180">
          <template #default="{ row }">
            <div class="score-cell">
              <el-progress
                :percentage="row.trustScore"
                :color="getScoreColor(row.trustScore)"
                :stroke-width="14"
                :text-inside="true"
                class="trust-progress"
              />
            </div>
          </template>
        </el-table-column>

        <!-- 趋势 -->
        <el-table-column label="趋势" width="90" align="center">
          <template #default="{ row }">
            <div class="trend-cell" :class="row.riskTrend?.toLowerCase()">
              <el-icon v-if="row.riskTrend === 'IMPROVING'" class="trend-icon"><Top /></el-icon>
              <el-icon v-else-if="row.riskTrend === 'WORSENING'" class="trend-icon"><Bottom /></el-icon>
              <el-icon v-else class="trend-icon"><Minus /></el-icon>
              <span class="trend-text">{{ getTrendText(row.riskTrend) }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 标签 -->
        <el-table-column label="标签" min-width="220">
          <template #default="{ row }">
            <div class="tags-cell">
              <template v-if="row.tags && row.tags.length > 0">
                <el-tag
                  v-for="tag in row.tags.slice(0, 3)"
                  :key="tag.code"
                  size="small"
                  effect="plain"
                  round
                  class="user-tag"
                >
                  {{ tag.name }}
                </el-tag>
                <el-tag
                  v-if="row.tags.length > 3"
                  size="small"
                  type="info"
                  effect="plain"
                  round
                  class="more-tag"
                >
                  +{{ row.tags.length - 3 }}
                </el-tag>
              </template>
              <span v-else class="no-data">暂无标签</span>
            </div>
          </template>
        </el-table-column>

        <!-- 违规次数 -->
        <el-table-column label="违规次数" width="90" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="row.totalViolations > 0"
              :type="row.totalViolations > 5 ? 'danger' : 'warning'"
              effect="plain"
              size="small"
              round
            >
              {{ row.totalViolations }}
            </el-tag>
            <span v-else class="zero-violation">0</span>
          </template>
        </el-table-column>

        <!-- 最后活跃 -->
        <el-table-column label="最后活跃" width="110">
          <template #default="{ row }">
            <span class="date-text">{{ formatDate(row.lastActiveAt) }}</span>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="操作" width="90" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link class="action-btn" @click="handleViewDetail(row)">
              详情 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
          class="custom-pagination"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { 
  User, Warning, InfoFilled, CircleCheck, Refresh, 
  Top, Bottom, Minus, Search, ArrowRight 
} from '@element-plus/icons-vue'
import { profileApi } from '@/api/profile'
import type { ProfileListItem, RiskLevel, UserType } from '@/types/profile'

const router = useRouter()
const loading = ref(false)

// 搜索表单
const searchForm = reactive({
  userType: null as UserType | null,
  riskLevel: null as RiskLevel | null,
  minScore: null as number | null,
  maxScore: null as number | null
})

// 统计数据
const statistics = reactive({
  total: 0,
  highRisk: 0,
  mediumRisk: 0,
  lowRisk: 0
})

// 表格数据
const tableData = ref<ProfileListItem[]>([])

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 风险颜色
const riskColors: Record<string, string> = {
  HIGH: '#ef4444',
  MEDIUM: '#f59e0b',
  LOW: '#10b981'
}

function getRiskColor(level: string) {
  return riskColors[level] || '#9ca3af'
}

function getRiskTagType(level: string): 'danger' | 'warning' | 'success' {
  const map: Record<string, 'danger' | 'warning' | 'success'> = {
    HIGH: 'danger',
    MEDIUM: 'warning',
    LOW: 'success'
  }
  return map[level] || 'warning'
}

function getRiskText(level: string) {
  const map: Record<string, string> = {
    HIGH: '高风险',
    MEDIUM: '中风险',
    LOW: '低风险'
  }
  return map[level] || level
}

function getScoreColor(score: number): string {
  if (score >= 70) return '#10b981'
  if (score >= 40) return '#f59e0b'
  return '#ef4444'
}

function getTrendText(trend?: string): string {
  const map: Record<string, string> = {
    IMPROVING: '改善',
    WORSENING: '恶化',
    STABLE: '稳定'
  }
  return map[trend || ''] || '-'
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

async function loadData() {
  loading.value = true
  try {
    const result = await profileApi.getList({
      userType: searchForm.userType || undefined,
      riskLevel: searchForm.riskLevel || undefined,
      minScore: searchForm.minScore || undefined,
      maxScore: searchForm.maxScore || undefined,
      page: pagination.page,
      pageSize: pagination.pageSize
    })

    tableData.value = result.list
    pagination.total = result.total

    statistics.total = result.total
    statistics.highRisk = result.list.filter(p => p.riskLevel === 'HIGH').length
    statistics.mediumRisk = result.list.filter(p => p.riskLevel === 'MEDIUM').length
    statistics.lowRisk = result.list.filter(p => p.riskLevel === 'LOW').length
  } catch (error) {
    console.error('加载失败:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  searchForm.userType = null
  searchForm.riskLevel = null
  searchForm.minScore = null
  searchForm.maxScore = null
  pagination.page = 1
  loadData()
}

function handleViewDetail(row: ProfileListItem) {
  router.push(`/profile/${row.userId}`)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.profile-list-page {
  padding: 0;
}

/* ========== 页面标题 ========== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-2xl);
  padding: var(--spacing-xl) 0;
}

.page-title-wrapper {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.page-title {
  font-size: var(--text-3xl);
  font-weight: var(--font-bold);
  color: var(--text-primary);
  margin: 0;
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin: 0;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-xl);
  font-weight: var(--font-medium);
  transition: all var(--transition-base);
}

/* ========== 搜索卡片 ========== */
.search-card {
  margin-bottom: var(--spacing-2xl);
  border-radius: var(--radius-xl);
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: var(--spacing-lg);
}

.search-select {
  width: 140px;
}

.score-range {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.score-input {
  width: 90px;
}

.range-separator {
  color: var(--text-secondary);
  font-size: var(--text-sm);
}

.search-actions {
  margin-left: auto;
}

.reset-btn {
  margin-right: var(--spacing-sm);
}

.search-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-lg);
  font-weight: var(--font-medium);
}

/* ========== 统计卡片 ========== */
.stat-row {
  margin-bottom: var(--spacing-2xl);
}

.stat-card {
  border-radius: var(--radius-xl);
  overflow: hidden;
  transition: all var(--transition-slow);
  height: 120px;
  position: relative;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  opacity: 0;
  transition: opacity var(--transition-base);
}

.stat-card-total::before { background: var(--gradient-primary); }
.stat-card-high::before { background: var(--gradient-danger); }
.stat-card-medium::before { background: var(--gradient-warning); }
.stat-card-low::before { background: var(--gradient-success); }

.stat-card:hover::before {
  opacity: 1;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-xl);
}

.stat-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-md);
}

.stat-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: var(--shadow-md);
}

.gradient-primary { background: var(--gradient-primary); }
.gradient-success { background: var(--gradient-success); }
.gradient-warning { background: var(--gradient-warning); }
.gradient-danger { background: var(--gradient-danger); }

.stat-badge {
  font-weight: var(--font-semibold);
}

.stat-body {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 32px;
  font-weight: var(--font-bold);
  color: var(--text-primary);
  line-height: 1;
  letter-spacing: -0.5px;
}

.stat-value.danger { color: var(--danger); }
.stat-value.warning { color: var(--warning); }
.stat-value.success { color: var(--success); }

.stat-label {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin-top: var(--spacing-xs);
  font-weight: var(--font-medium);
}

/* ========== 表格卡片 ========== */
.table-card {
  border-radius: var(--radius-xl);
  overflow: hidden;
  transition: all var(--transition-base);
}

.table-card:hover {
  box-shadow: var(--shadow-xl);
}

/* 用户单元格 */
.user-cell {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.user-avatar {
  flex-shrink: 0;
  font-weight: var(--font-semibold);
  transition: transform var(--transition-fast);
}

.user-cell:hover .user-avatar {
  transform: scale(1.1);
}

.user-info {
  flex: 1;
  min-width: 0;
}

.nickname {
  font-weight: var(--font-semibold);
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--text-xs);
  color: var(--text-secondary);
  margin-top: 4px;
}

.user-type-tag {
  font-weight: var(--font-medium);
}

/* 分数单元格 */
.score-cell {
  padding-right: var(--spacing-sm);
}

.trust-progress {
  border-radius: var(--radius-full);
}

/* 趋势单元格 */
.trend-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.trend-icon {
  font-size: 18px;
}

.trend-cell.improving { color: var(--success); }
.trend-cell.stable { color: var(--text-secondary); }
.trend-cell.worsening { color: var(--danger); }

.trend-text {
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
}

/* 标签单元格 */
.tags-cell {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
}

.user-tag {
  font-weight: var(--font-medium);
}

.no-data {
  color: var(--text-placeholder);
  font-size: var(--text-sm);
}

/* 违规次数 */
.zero-violation {
  color: var(--success);
  font-weight: var(--font-semibold);
}

/* 日期 */
.date-text {
  color: var(--text-secondary);
  font-size: var(--text-sm);
}

/* 操作按钮 */
.action-btn {
  display: flex;
  align-items: center;
  gap: 2px;
  font-weight: var(--font-medium);
  transition: all var(--transition-fast);
}

.action-btn:hover {
  transform: translateX(4px);
}

/* ========== 分页 ========== */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--spacing-xl);
  padding-top: var(--spacing-xl);
  border-top: 1px solid var(--border-light);
}

.custom-pagination {
  --el-pagination-bg-color: var(--bg-card);
  --el-pagination-text-color: var(--text-regular);
  --el-pagination-button-disabled-bg-color: var(--gray-100);
  --el-pagination-button-disabled-color: var(--text-placeholder);
  --el-pagination-hover-color: var(--primary-600);
}

/* ========== 响应式适配 ========== */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-lg);
  }
  
  .search-form {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-select {
    width: 100%;
  }
  
  .search-actions {
    margin-left: 0;
  }
}
</style>
