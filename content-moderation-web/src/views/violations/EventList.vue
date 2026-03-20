<template>
  <div class="event-list-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>违规事件</h2>
      <el-button @click="loadData">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 搜索表单 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="违规类型">
          <el-select v-model="searchForm.violationType" placeholder="请选择" clearable style="width: 150px">
            <el-option
              v-for="(name, type) in violationTypeMap"
              :key="type"
              :label="name"
              :value="type"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="置信度">
          <el-select v-model="searchForm.confidence" placeholder="请选择" clearable style="width: 120px">
            <el-option label="高 (>90%)" value="high" />
            <el-option label="中 (70-90%)" value="medium" />
            <el-option label="低 (<70%)" value="low" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 120px">
            <el-option label="待审核" value="PENDING" />
            <el-option label="已确认" value="CONFIRMED" />
            <el-option label="已驳回" value="DISMISSED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="eventId" label="事件ID" width="180" show-overflow-tooltip />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="callId" label="Call ID" width="180" show-overflow-tooltip />
        <el-table-column prop="violationName" label="违规类型" width="120" />
        <el-table-column label="置信度" width="120">
          <template #default="{ row }">
            <el-progress 
              :percentage="row.confidence * 100" 
              :color="row.confidence > 0.85 ? '#f56c6c' : '#e6a23c'"
              :stroke-width="10"
            />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="router.push(`/violations/${row.eventId}`)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { violationTypeMap } from '@/types/profile'

const router = useRouter()
const loading = ref(false)

const searchForm = reactive({
  violationType: '',
  confidence: '',
  status: ''
})

const tableData = ref<any[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
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

// 模拟数据
function generateMockData() {
  const types = Object.keys(violationTypeMap)
  const data = []
  for (let i = 0; i < 20; i++) {
    const type = types[Math.floor(Math.random() * types.length)]
    data.push({
      eventId: `evt_${Date.now()}_${i}`,
      userId: 10000 + Math.floor(Math.random() * 100),
      callId: `call_${Date.now()}_${i}`,
      violationType: type,
      violationName: violationTypeMap[type as keyof typeof violationTypeMap],
      confidence: 0.6 + Math.random() * 0.38,
      status: ['PENDING', 'CONFIRMED', 'DISMISSED'][Math.floor(Math.random() * 3)],
      createdAt: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString()
    })
  }
  return data
}

async function loadData() {
  loading.value = true
  try {
    // 模拟数据
    const allData = generateMockData()
    tableData.value = allData
    pagination.total = allData.length
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  searchForm.violationType = ''
  searchForm.confidence = ''
  searchForm.status = ''
  pagination.page = 1
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.event-list-page {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.search-card {
  margin-bottom: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>