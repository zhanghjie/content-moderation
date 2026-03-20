<template>
  <div class="tag-repository-page">
    <div class="page-header">
      <div class="page-title-wrapper">
        <h1 class="page-title">治理级标签</h1>
        <p class="page-subtitle">聚合展示 AI 生成标签，支持分类检索与置信度过滤</p>
      </div>
      <el-button type="primary" :loading="loading" @click="loadData">
        刷新标签
      </el-button>
    </div>

    <el-card shadow="hover" class="filter-card">
      <el-form :model="filters" inline>
        <el-form-item label="标签分类">
          <el-select v-model="filters.category" clearable placeholder="全部分类" class="filter-select">
            <el-option v-for="category in categories" :key="category" :label="category" :value="category" />
          </el-select>
        </el-form-item>
        <el-form-item label="最低置信度">
          <el-slider v-model="filters.minConfidence" :min="0" :max="100" :step="5" show-input class="filter-slider" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16" class="summary-row">
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <div class="summary-item">
            <span class="summary-label">标签总数</span>
            <span class="summary-value">{{ filteredTags.length }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <div class="summary-item">
            <span class="summary-label">覆盖用户</span>
            <span class="summary-value">{{ coveredUsers }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <div class="summary-item">
            <span class="summary-label">平均置信度</span>
            <span class="summary-value">{{ avgConfidence }}%</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover">
      <el-table :data="paginatedTags" v-loading="loading" style="width: 100%">
        <el-table-column prop="name" label="标签名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="category" label="标签分类" min-width="120" />
        <el-table-column prop="userId" label="用户ID" min-width="100" />
        <el-table-column label="置信度" min-width="120">
          <template #default="{ row }">
            <el-progress :percentage="Math.round(row.confidence * 100)" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" min-width="120" />
        <el-table-column prop="createdAt" label="生成时间" min-width="160" />
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="filteredTags.length"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { profileApi } from '@/api/profile'
import type { UserTag } from '@/types/profile'

type TagRow = UserTag & { userId: number }

const loading = ref(false)
const tags = ref<TagRow[]>([])
const categories = ref<string[]>([])

const filters = reactive({
  category: '',
  minConfidence: 60
})

const pagination = reactive({
  page: 1,
  pageSize: 10
})

const filteredTags = computed(() => {
  return tags.value.filter(item => {
    const matchCategory = filters.category ? item.category === filters.category : true
    const matchConfidence = item.confidence * 100 >= filters.minConfidence
    return matchCategory && matchConfidence
  })
})

const paginatedTags = computed(() => {
  const start = (pagination.page - 1) * pagination.pageSize
  return filteredTags.value.slice(start, start + pagination.pageSize)
})

const coveredUsers = computed(() => {
  return new Set(filteredTags.value.map(item => item.userId)).size
})

const avgConfidence = computed(() => {
  if (filteredTags.value.length === 0) return 0
  const total = filteredTags.value.reduce((sum, item) => sum + item.confidence * 100, 0)
  return Math.round(total / filteredTags.value.length)
})

async function loadData() {
  loading.value = true
  try {
    const res = await profileApi.getList({ page: 1, pageSize: 200 })
    const merged: TagRow[] = []
    res.list.forEach(user => {
      user.tags.forEach(tag => {
        merged.push({ ...tag, userId: user.userId })
      })
    })
    tags.value = merged.sort((a, b) => b.confidence - a.confidence)
    categories.value = Array.from(new Set(merged.map(item => item.category)))
    pagination.page = 1
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.tag-repository-page {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  margin: 0;
  font-size: 28px;
}

.page-subtitle {
  margin: 4px 0 0;
  color: var(--text-secondary);
}

.filter-card {
  margin-bottom: 16px;
}

.filter-select {
  width: 180px;
}

.filter-slider {
  width: 280px;
}

.summary-row {
  margin-bottom: 16px;
}

.summary-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary-label {
  color: var(--text-secondary);
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .filter-slider {
    width: 220px;
  }
}
</style>
