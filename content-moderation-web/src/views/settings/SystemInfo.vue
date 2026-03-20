<template>
  <div class="system-info-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-wrapper">
        <h1 class="page-title">系统信息</h1>
        <p class="page-subtitle">查看系统运行状态和配置参数</p>
      </div>
      <el-button @click="loadData" class="refresh-btn" round>
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 服务状态卡片 -->
    <el-card shadow="hover" class="status-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <div class="title-icon gradient-success">
              <el-icon><Monitor /></el-icon>
            </div>
            <span>服务状态监控</span>
          </div>
          <el-tag type="success" effect="plain" size="small">
            <el-icon><CircleCheck /></el-icon>
            系统正常
          </el-tag>
        </div>
      </template>
      
      <el-row :gutter="24" class="status-row">
        <el-col :span="8">
          <div class="status-item">
            <div class="status-icon gradient-primary">
              <el-icon :size="32"><Connection /></el-icon>
            </div>
            <div class="status-info">
              <div class="status-label">API 服务</div>
              <div class="status-value success">
                <el-icon><CircleCheck /></el-icon>
                正常运行
              </div>
              <div class="status-meta">响应时间：23ms</div>
            </div>
          </div>
        </el-col>
        
        <el-col :span="8">
          <div class="status-item">
            <div class="status-icon gradient-success">
              <el-icon :size="32"><DataLine /></el-icon>
            </div>
            <div class="status-info">
              <div class="status-label">数据库</div>
              <div class="status-value success">
                <el-icon><CircleCheck /></el-icon>
                已连接
              </div>
              <div class="status-meta">PostgreSQL 15.14</div>
            </div>
          </div>
        </el-col>
        
        <el-col :span="8">
          <div class="status-item">
            <div class="status-icon gradient-warning">
              <el-icon :size="32"><Coin /></el-icon>
            </div>
            <div class="status-info">
              <div class="status-label">缓存服务</div>
              <div class="status-value success">
                <el-icon><CircleCheck /></el-icon>
                正常运行
              </div>
              <div class="status-meta">命中率：98.5%</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 系统参数卡片 -->
    <el-card shadow="hover" class="params-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <div class="title-icon gradient-primary">
              <el-icon><Setting /></el-icon>
            </div>
            <span>系统参数</span>
          </div>
        </div>
      </template>
      
      <el-descriptions :column="2" border class="system-descriptions">
        <el-descriptions-item label="系统名称">
          <span class="desc-value">内容风控系统</span>
        </el-descriptions-item>
        <el-descriptions-item label="版本号">
          <el-tag type="primary" size="small" effect="plain">v1.0.0-SNAPSHOT</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="运行环境">
          <el-tag type="success" size="small" effect="plain">生产环境</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="启动时间">
          <span class="desc-value">2026-03-15 16:00:00</span>
        </el-descriptions-item>
        <el-descriptions-item label="运行时长">
          <span class="desc-value">2 天 14 小时 32 分钟</span>
        </el-descriptions-item>
        <el-descriptions-item label="JDK 版本">
          <span class="desc-value">17.0.16</span>
        </el-descriptions-item>
        <el-descriptions-item label="Spring Boot">
          <span class="desc-value">3.2.0</span>
        </el-descriptions-item>
        <el-descriptions-item label="MyBatis-Plus">
          <span class="desc-value">3.5.6</span>
        </el-descriptions-item>
        <el-descriptions-item label="数据库类型">
          <span class="desc-value">PostgreSQL 15.14</span>
        </el-descriptions-item>
        <el-descriptions-item label="数据库地址">
          <span class="desc-value">47.99.69.21:9877</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 资源使用卡片 -->
    <el-row :gutter="24" class="resource-row">
      <el-col :span="8">
        <el-card shadow="hover" class="resource-card">
          <template #header>
            <div class="resource-header">
              <span>CPU 使用率</span>
              <el-tag type="success" size="small" effect="plain">正常</el-tag>
            </div>
          </template>
          <div class="resource-content">
            <el-progress
              type="dashboard"
              :percentage="23.5"
              :color="customColors"
              :width="160"
            />
            <div class="resource-detail">
              <span>4 核心 · 8 线程</span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card shadow="hover" class="resource-card">
          <template #header>
            <div class="resource-header">
              <span>内存使用率</span>
              <el-tag type="warning" size="small" effect="plain">适中</el-tag>
            </div>
          </template>
          <div class="resource-content">
            <el-progress
              type="dashboard"
              :percentage="67.2"
              :color="customColors"
              :width="160"
            />
            <div class="resource-detail">
              <span>8.2GB / 16GB</span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card shadow="hover" class="resource-card">
          <template #header>
            <div class="resource-header">
              <span>磁盘使用率</span>
              <el-tag type="success" size="small" effect="plain">充足</el-tag>
            </div>
          </template>
          <div class="resource-content">
            <el-progress
              type="dashboard"
              :percentage="42.8"
              :color="customColors"
              :width="160"
            />
            <div class="resource-detail">
              <span>214GB / 500GB</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { CircleCheck, Refresh, Monitor, Connection, DataLine, Coin, Setting } from '@element-plus/icons-vue'

const customColors = [
  { color: '#10b981', percentage: 60 },
  { color: '#f59e0b', percentage: 80 },
  { color: '#ef4444', percentage: 100 }
]

const loadData = () => {
  // 加载数据逻辑
}
</script>

<style scoped>
.system-info-page {
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

/* ========== 状态卡片 ========== */
.status-card {
  margin-bottom: var(--spacing-2xl);
  border-radius: var(--radius-xl);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  font-weight: var(--font-semibold);
  font-size: var(--text-lg);
  color: var(--text-primary);
}

.title-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: var(--shadow-sm);
}

.gradient-primary { background: var(--gradient-primary); }
.gradient-success { background: var(--gradient-success); }
.gradient-warning { background: var(--gradient-warning); }
.gradient-info { background: var(--gradient-info); }

.status-row {
  padding: var(--spacing-lg) 0;
}

.status-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-lg);
  border-radius: var(--radius-lg);
  background: var(--gray-50);
  transition: all var(--transition-base);
}

.status-item:hover {
  background: var(--primary-50);
  transform: translateX(4px);
}

.status-icon {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: var(--shadow-md);
}

.status-info {
  flex: 1;
}

.status-label {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  font-weight: var(--font-medium);
  margin-bottom: var(--spacing-xs);
}

.status-value {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-xs);
}

.status-value.success {
  color: var(--success);
}

.status-meta {
  font-size: var(--text-xs);
  color: var(--text-secondary);
}

/* ========== 参数卡片 ========== */
.params-card {
  margin-bottom: var(--spacing-2xl);
  border-radius: var(--radius-xl);
}

.system-descriptions {
  --el-descriptions-border-color: var(--border-light);
  --el-descriptions-label-bg-color: var(--gray-50);
}

:deep(.el-descriptions__label) {
  font-weight: var(--font-medium);
  color: var(--text-secondary);
  width: 140px;
}

:deep(.el-descriptions__content) {
  font-weight: var(--font-medium);
  color: var(--text-primary);
}

.desc-value {
  font-weight: var(--font-medium);
}

/* ========== 资源卡片 ========== */
.resource-row {
  margin-bottom: var(--spacing-2xl);
}

.resource-card {
  border-radius: var(--radius-xl);
  transition: all var(--transition-base);
}

.resource-card:hover {
  box-shadow: var(--shadow-xl);
}

.resource-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: var(--font-semibold);
  color: var(--text-primary);
}

.resource-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-lg) 0;
}

.resource-detail {
  margin-top: var(--spacing-md);
  font-size: var(--text-sm);
  color: var(--text-secondary);
  font-weight: var(--font-medium);
}

/* ========== 响应式适配 ========== */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-lg);
  }
  
  .status-item {
    flex-direction: column;
    text-align: center;
  }
  
  .resource-row {
    flex-direction: column;
  }
}
</style>
