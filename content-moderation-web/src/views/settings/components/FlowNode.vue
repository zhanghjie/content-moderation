<template>
  <div class="flow-node" :class="{ disabled: !module.enabled }">
    <div class="node-header">
      <div class="head-left">
        <span class="drag-icon">⋮⋮</span>
        <el-input
          :model-value="module.name"
          @update:model-value="(v) => emit('titleChange', String(v || ''))"
          @blur="emit('titleSave')"
          class="title-input"
        />
        <el-tag size="small" effect="plain" :type="tagType(module.type)">
          {{ module.type }}
        </el-tag>
      </div>
      <div class="head-right">
        <el-switch :model-value="module.enabled" @change="(v) => emit('toggleEnabled', !!v)" />
        <el-button link type="primary" @click="emit('toggleExpand')">{{ expanded ? '收起' : '展开' }}</el-button>
        <el-button link type="primary" @click="emit('edit')">编辑</el-button>
        <el-button link type="danger" @click="emit('remove')">删除</el-button>
      </div>
    </div>
    <div class="node-meta">
      <span class="meta-code">{{ module.id }}</span>
      <span class="meta-order">#{{ module.order }}</span>
    </div>
    <div v-if="expanded" class="node-body">
      <el-input
        :model-value="module.content"
        @update:model-value="(v) => emit('contentChange', String(v || ''))"
        @blur="emit('contentSave')"
        type="textarea"
        :rows="8"
        placeholder="请输入 Prompt 内容"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PromptFlowModule } from './PromptFlowBuilder.vue'

defineProps<{
  module: PromptFlowModule
  expanded: boolean
}>()

const emit = defineEmits<{
  toggleExpand: []
  toggleEnabled: [enabled: boolean]
  remove: []
  edit: []
  contentChange: [content: string]
  contentSave: []
  titleChange: [title: string]
  titleSave: []
}>()

const tagType = (type: PromptFlowModule['type']) => {
  if (type === 'BASE') return 'danger'
  if (type === 'JSON') return 'warning'
  if (type === 'TONE') return 'success'
  return 'primary'
}
</script>

<style scoped>
.flow-node {
  border-radius: 16px;
  background: #ffffff;
  border: 0.5px solid #dce3ef;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.05);
  transition: box-shadow 0.2s ease, border-color 0.2s ease, transform 0.2s ease;
}

.flow-node:hover {
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.1);
  border-color: #b9cbef;
  transform: translateY(-1px);
}

.flow-node.disabled {
  opacity: 0.65;
}

.node-header {
  padding: 12px 14px 8px;
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.head-left {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.drag-icon {
  color: #7b8aa6;
  line-height: 1;
  font-size: 12px;
  letter-spacing: -1px;
}

.title-input {
  flex: 1;
}

.title-input :deep(.el-input__wrapper) {
  box-shadow: none;
  border-radius: 8px;
  border: 0.5px solid #d8e1ee;
}

.head-right {
  display: flex;
  align-items: center;
  gap: 0;
}

.node-meta {
  margin: 0 14px;
  padding: 0 0 8px;
  display: flex;
  justify-content: space-between;
  color: #64748b;
  font-size: 11px;
}

.meta-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
}

.node-body {
  margin: 0 10px 10px;
  padding-top: 8px;
  border-top: 0.5px dashed #d6deeb;
}
</style>
