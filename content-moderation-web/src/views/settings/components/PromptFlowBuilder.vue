<template>
  <div class="flow-wrap">
    <div class="endpoint start">Start</div>
    <div class="line"></div>

    <DragContext :items="modules" key-field="id" @reorder="onReorder">
      <template #default="{ item }">
        <FlowNode
          :module="item as PromptFlowModule"
          :expanded="!!expanded[(item as PromptFlowModule).id]"
          @toggleExpand="toggleExpand((item as PromptFlowModule).id)"
          @toggleEnabled="(enabled) => onToggleEnabled(item as PromptFlowModule, enabled)"
          @remove="onRemove(item as PromptFlowModule)"
          @edit="onEdit(item as PromptFlowModule)"
          @contentChange="(content) => onContentChange(item as PromptFlowModule, content)"
          @contentSave="onContentSave(item as PromptFlowModule)"
          @titleChange="(title) => onTitleChange(item as PromptFlowModule, title)"
          @titleSave="onTitleSave(item as PromptFlowModule)"
        />
      </template>
    </DragContext>

    <div class="add-row">
      <el-button type="primary" plain @click="emit('add')">+ Add</el-button>
    </div>
    <div class="line"></div>
    <div class="endpoint end">End</div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import DragContext from './DragContext.vue'
import FlowNode from './FlowNode.vue'

export type PromptFlowModule = {
  id: string
  name: string
  type: 'BASE' | 'RULE' | 'JSON' | 'TONE'
  enabled: boolean
  content: string
  order: number
}

defineProps<{
  modules: PromptFlowModule[]
}>()

const emit = defineEmits<{
  reorder: [items: PromptFlowModule[]]
  toggleEnabled: [item: PromptFlowModule, enabled: boolean]
  remove: [item: PromptFlowModule]
  edit: [item: PromptFlowModule]
  contentChange: [item: PromptFlowModule, content: string]
  contentSave: [item: PromptFlowModule]
  titleChange: [item: PromptFlowModule, title: string]
  titleSave: [item: PromptFlowModule]
  add: []
}>()

const expanded = reactive<Record<string, boolean>>({})

const toggleExpand = (id: string) => {
  expanded[id] = !expanded[id]
}

const onReorder = (items: Record<string, any>[]) => {
  emit('reorder', items as PromptFlowModule[])
}

const onToggleEnabled = (item: PromptFlowModule, enabled: boolean) => {
  emit('toggleEnabled', item, enabled)
}

const onRemove = (item: PromptFlowModule) => {
  emit('remove', item)
}

const onEdit = (item: PromptFlowModule) => {
  emit('edit', item)
}

const onContentChange = (item: PromptFlowModule, content: string) => {
  emit('contentChange', item, content)
}

const onContentSave = (item: PromptFlowModule) => {
  emit('contentSave', item)
}

const onTitleChange = (item: PromptFlowModule, title: string) => {
  emit('titleChange', item, title)
}

const onTitleSave = (item: PromptFlowModule) => {
  emit('titleSave', item)
}
</script>

<style scoped>
.flow-wrap {
  padding: 8px 4px 10px;
}

.endpoint {
  width: 132px;
  margin: 0 auto;
  border-radius: 999px;
  text-align: center;
  padding: 6px 10px;
  font-weight: 650;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.endpoint.start {
  background: #ecfdf3;
  color: #15803d;
  border: 0.5px solid #bbf7d0;
}

.endpoint.end {
  background: #f8fafc;
  color: #334155;
  border: 0.5px solid #d3deec;
}

.line {
  width: 2px;
  height: 14px;
  margin: 8px auto;
  background: linear-gradient(180deg, #bfdbfe, #94a3b8);
  border-radius: 999px;
}

.add-row {
  margin-top: 12px;
  display: flex;
  justify-content: center;
}
</style>
