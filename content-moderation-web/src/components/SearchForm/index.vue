<template>
  <n-card class="search-form" :bordered="false">
    <n-form :model="formValue" :label-width="80" size="medium">
      <n-grid :cols="24" :x-gap="16" :y-gap="16">
        <slot name="items"></slot>
        
        <n-grid-item :span="24">
          <n-space justify="end">
            <n-button @click="handleReset">重置</n-button>
            <n-button type="primary" @click="handleSearch">搜索</n-button>
          </n-space>
        </n-grid-item>
      </n-grid>
    </n-form>
  </n-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  initialValues?: Record<string, any>
}>()

const emit = defineEmits<{
  (e: 'search', values: Record<string, any>): void
  (e: 'reset', values: Record<string, any>): void
}>()

const formValue = ref<Record<string, any>>({
  ...props.initialValues
})

const handleSearch = () => {
  emit('search', { ...formValue.value })
}

const handleReset = () => {
  formValue.value = { ...props.initialValues }
  emit('reset', { ...formValue.value })
}
</script>

<style scoped>
.search-form {
  margin-bottom: 16px;
}
</style>
