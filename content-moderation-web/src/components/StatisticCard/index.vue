<template>
  <el-card class="statistic-card" shadow="hover">
    <div class="statistic-header">
      <span class="statistic-title">{{ title }}</span>
      <el-icon :color="iconColor" :size="24">
        <component :is="icon" />
      </el-icon>
    </div>
    <div class="statistic-content">
      <el-statistic :value="value">
        <template v-if="prefix" #prefix>{{ prefix }}</template>
        <template v-if="suffix" #suffix>{{ suffix }}</template>
      </el-statistic>
      <div v-if="trend !== undefined" class="statistic-trend" :class="{ 'up': trend > 0, 'down': trend < 0 }">
        <el-icon v-if="trend > 0"><Top /></el-icon>
        <el-icon v-if="trend < 0"><Bottom /></el-icon>
        <span>{{ Math.abs(trend) }}%</span>
      </div>
    </div>
    <div v-if="footer" class="statistic-footer">{{ footer }}</div>
  </el-card>
</template>

<script setup lang="ts">
import { Top, Bottom } from '@element-plus/icons-vue'

defineProps<{
  title: string
  value: number | string
  icon?: any
  iconColor?: string
  prefix?: string
  suffix?: string
  trend?: number
  footer?: string
}>()
</script>

<style scoped>
.statistic-card {
  height: 100%;
}

.statistic-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.statistic-title {
  font-size: 14px;
  color: #666;
}

.statistic-content {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.statistic-trend {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.statistic-trend.up {
  color: #f56c6c;
}

.statistic-trend.down {
  color: #67c23a;
}

.statistic-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
  font-size: 12px;
  color: #909399;
}
</style>