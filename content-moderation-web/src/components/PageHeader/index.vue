<template>
  <div class="page-header">
    <div class="page-header-main">
      <h2 class="page-header-title">{{ title }}</h2>
      <p v-if="subtitle" class="page-header-subtitle">{{ subtitle }}</p>
    </div>
    <div class="page-header-extra">
      <slot name="extra"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
}>()
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 8px;
}

.page-header-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1c1c1c;
}

.page-header-subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  color: #666;
}

.page-header-extra {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
