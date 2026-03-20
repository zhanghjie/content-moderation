<template>
  <router-view />
</template>

<script setup lang="ts">
</script>

<style>
/* 
  全局样式已在 main.ts 中导入
  这里只保留 Element Plus 组件的全局样式覆盖
*/

/* Element Plus 全局样式覆盖 */
.el-card {
  border-radius: var(--radius-xl);
  border: none;
  background-color: var(--bg-card);
  transition: box-shadow var(--transition-base), transform var(--transition-base);
}

.el-card:hover {
  box-shadow: var(--shadow-xl);
  transform: translateY(-2px);
}

.el-card__header {
  background: transparent;
  border-bottom: 1px solid var(--border-light);
  padding: var(--spacing-lg) var(--spacing-xl);
}

.el-menu {
  border-right: none !important;
}

.el-table {
  font-size: var(--text-sm);
}

.el-table th {
  font-weight: var(--font-semibold);
  color: var(--text-secondary);
  background-color: var(--gray-50) !important;
}

.el-button--primary {
  --el-button-bg-color: var(--primary-600);
  --el-button-border-color: var(--primary-600);
  --el-button-hover-bg-color: var(--primary-700);
  --el-button-hover-border-color: var(--primary-700);
}

.el-tag {
  border-radius: var(--radius-full);
  font-weight: var(--font-medium);
}

.el-select, .el-input, .el-input-number {
  width: 100%;
}

/* 页面过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
