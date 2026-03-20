<template>
  <div class="drag-context">
    <div
      v-for="(item, index) in items"
      :key="item[keyField]"
      class="drag-item"
      :class="{
        dragging: draggingIndex === index,
        over: overIndex === index && draggingIndex !== index
      }"
      draggable
      @dragstart="onDragStart(index)"
      @dragend="onDragEnd"
      @dragover.prevent="onDragOver(index)"
      @drop.prevent="onDrop(index)"
    >
      <slot :item="item" :index="index" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  items: Record<string, any>[]
  keyField?: string
}>()

const emit = defineEmits<{
  reorder: [items: Record<string, any>[]]
}>()

const keyField = props.keyField || 'id'
const draggingIndex = ref<number | null>(null)
const overIndex = ref<number | null>(null)

const onDragStart = (index: number) => {
  draggingIndex.value = index
  overIndex.value = index
}

const onDragOver = (index: number) => {
  overIndex.value = index
}

const onDrop = (dropIndex: number) => {
  const from = draggingIndex.value
  if (from === null || from === dropIndex) return
  const next = props.items.slice()
  const [moved] = next.splice(from, 1)
  next.splice(dropIndex, 0, moved)
  emit('reorder', next)
}

const onDragEnd = () => {
  draggingIndex.value = null
  overIndex.value = null
}
</script>

<style scoped>
.drag-context {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.drag-item {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.drag-item.over {
  transform: translateY(2px);
}

.drag-item.dragging {
  opacity: 0.88;
}
</style>

