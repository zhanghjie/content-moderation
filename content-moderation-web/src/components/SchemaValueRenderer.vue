<template>
  <div class="schema-renderer">
    <div v-if="label || description || title" class="schema-header">
      <div class="schema-title">{{ title || label || '结果' }}</div>
      <div v-if="description" class="schema-description">{{ description }}</div>
    </div>

    <el-empty v-if="isEmpty" :description="emptyText" :image-size="72" />

    <template v-else>
      <template v-if="isPrimitive">
        <el-tag v-if="schema?.enum && schema.enum.includes(normalizedValue)" effect="plain">
          {{ displayPrimitive }}
        </el-tag>
        <span v-else class="primitive-value">{{ displayPrimitive }}</span>
      </template>

      <template v-else-if="isArray">
        <el-table v-if="tableColumns.length" :data="arrayRows" size="small" border style="width: 100%">
          <el-table-column
            v-for="column in tableColumns"
            :key="column.key"
            :prop="column.key"
            :label="column.label"
            :min-width="column.minWidth"
          >
            <template #default="{ row }">
              <SchemaValueRenderer
                :value="row[column.key]"
                :schema="column.schema"
                :label="''"
                :depth="depth + 1"
                empty-text="-"
              />
            </template>
          </el-table-column>
        </el-table>

        <div v-else class="array-stack">
          <div v-for="(item, index) in arrayRows" :key="index" class="array-item">
            <div class="array-item-header">
              <span>第 {{ index + 1 }} 项</span>
            </div>
            <SchemaValueRenderer
              :value="item"
              :schema="itemSchema"
              :label="''"
              :depth="depth + 1"
              empty-text="-"
            />
          </div>
        </div>
      </template>

      <template v-else-if="isObject">
        <el-descriptions v-if="objectFields.length" :column="columnCount" border>
          <el-descriptions-item v-for="field in objectFields" :key="field.key" :label="field.label">
            <SchemaValueRenderer
              :value="field.value"
              :schema="field.schema"
              :label="''"
              :depth="depth + 1"
              empty-text="-"
            />
          </el-descriptions-item>
        </el-descriptions>
        <pre v-else class="json-block">{{ formatJson(normalizedValue) }}</pre>
      </template>

      <pre v-else class="json-block">{{ formatJson(normalizedValue) }}</pre>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

defineOptions({
  name: 'SchemaValueRenderer'
})

type SchemaValue = Record<string, any> | Array<any> | string | number | boolean | null | undefined

const props = withDefaults(defineProps<{
  value: SchemaValue
  schema?: Record<string, any> | null
  label?: string
  title?: string
  description?: string
  emptyText?: string
  depth?: number
}>(), {
  schema: null,
  label: '',
  title: '',
  description: '',
  emptyText: '-',
  depth: 0
})

const normalizedValue = computed(() => props.value)
const schemaType = computed(() => String(props.schema?.type || inferType(normalizedValue.value)).toLowerCase())
const isEmpty = computed(() => normalizedValue.value === null || normalizedValue.value === undefined || (typeof normalizedValue.value === 'string' && normalizedValue.value.trim() === '') || (Array.isArray(normalizedValue.value) && normalizedValue.value.length === 0) || (isObjectValue(normalizedValue.value) && Object.keys(normalizedValue.value as Record<string, any>).length === 0))
const isPrimitive = computed(() => ['string', 'number', 'integer', 'boolean'].includes(schemaType.value))
const isArray = computed(() => schemaType.value === 'array' || Array.isArray(normalizedValue.value))
const isObject = computed(() => schemaType.value === 'object' || isObjectValue(normalizedValue.value))
const itemSchema = computed(() => props.schema?.items || null)
const arrayRows = computed(() => Array.isArray(normalizedValue.value) ? normalizedValue.value : [])
const objectValue = computed<Record<string, any>>(() => isObjectValue(normalizedValue.value) ? normalizedValue.value as Record<string, any> : {})
const objectFields = computed(() => {
  const schemaProps = props.schema?.properties && typeof props.schema.properties === 'object'
    ? Object.entries(props.schema.properties)
    : []
  const orderedKeys = schemaProps.map(([key]) => key)
  const extraKeys = Object.keys(objectValue.value).filter(key => !orderedKeys.includes(key))
  const keys = [...orderedKeys, ...extraKeys]
  return keys.map((key) => {
    const childSchema = schemaProps.find(([schemaKey]) => schemaKey === key)?.[1] as Record<string, any> | undefined
    return {
      key,
      label: childSchema?.title || prettifyKey(key),
      value: objectValue.value[key],
      schema: childSchema || null
    }
  })
})
const columnCount = computed(() => {
  const size = objectFields.value.length
  if (size <= 1) return 1
  if (size <= 4) return 2
  return 3
})
const tableColumns = computed(() => {
  if (!arrayRows.value.length) return [] as Array<{ key: string; label: string; minWidth: number; schema: Record<string, any> | null }>
  const firstObject = arrayRows.value.find(item => isObjectValue(item)) as Record<string, any> | undefined
  const schemaProps = itemSchema.value?.properties && typeof itemSchema.value.properties === 'object'
    ? Object.entries(itemSchema.value.properties)
    : []
  const keys = schemaProps.length
    ? schemaProps.map(([key]) => key)
    : firstObject
      ? Object.keys(firstObject)
      : []

  if (!keys.length || !arrayRows.value.every(item => isObjectValue(item))) {
    return []
  }

  return keys.map((key) => {
    const childSchema = schemaProps.find(([schemaKey]) => schemaKey === key)?.[1] as Record<string, any> | undefined
    return {
      key,
      label: childSchema?.title || prettifyKey(key),
      minWidth: key.length <= 8 ? 120 : 160,
      schema: childSchema || null
    }
  })
})
const title = computed(() => props.title || props.schema?.title || '')
const description = computed(() => props.description || props.schema?.description || '')
const label = computed(() => props.label || '')
const displayPrimitive = computed(() => formatPrimitive(normalizedValue.value))

function isObjectValue(value: SchemaValue): value is Record<string, any> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function inferType(value: SchemaValue): string {
  if (Array.isArray(value)) return 'array'
  if (value === null || value === undefined) return 'null'
  if (typeof value === 'boolean') return 'boolean'
  if (typeof value === 'number') return Number.isInteger(value) ? 'integer' : 'number'
  if (typeof value === 'string') return 'string'
  return 'object'
}

function prettifyKey(key: string) {
  return key
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/^./, char => char.toUpperCase())
}

function formatPrimitive(value: SchemaValue) {
  if (value === null || value === undefined) return props.emptyText
  if (typeof value === 'boolean') return value ? 'TRUE' : 'FALSE'
  if (typeof value === 'number') return String(value)
  return String(value)
}

function formatJson(value: any) {
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}
</script>

<style scoped>
.schema-renderer {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.schema-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.schema-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.schema-description {
  font-size: 12px;
  color: #64748b;
  white-space: pre-wrap;
}

.primitive-value {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  color: #0f172a;
  font-size: 13px;
  word-break: break-word;
  white-space: pre-wrap;
}

.array-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.array-item {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px;
  background: #f8fafc;
}

.array-item-header {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.json-block {
  margin: 0;
  padding: 12px;
  border-radius: 10px;
  background: #0f172a;
  color: #e2e8f0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  line-height: 1.6;
}
</style>
