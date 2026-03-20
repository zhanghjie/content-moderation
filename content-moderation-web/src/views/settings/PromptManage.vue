<template>
  <div class="prompt-manage-page">
    <el-card shadow="never" class="header-card">
      <div class="header-row">
        <div>
          <h2 class="title">Prompt 节点编排</h2>
          <p class="subtitle">Start → 模块节点 → End，支持拖拽排序、启用禁用、展开编辑</p>
        </div>
        <el-space>
          <el-select v-model="analysisType" class="scene-select" @change="loadModules">
            <el-option v-for="scene in analysisTypeOptions" :key="scene.value" :label="scene.label" :value="scene.value" />
          </el-select>
          <el-button @click="loadModules" :loading="loading">刷新</el-button>
        </el-space>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="16">
        <el-card shadow="never" class="builder-card">
          <template #header>
            <div class="builder-header">
              <span>① 节点编排</span>
              <el-space>
                <el-tag effect="plain">{{ modules.length }} 个模块</el-tag>
                <el-tag :type="dslValidate?.valid ? 'success' : 'info'" effect="plain">
                  {{ dslValidate ? (dslValidate.valid ? '已通过校验' : '待修正') : '未校验' }}
                </el-tag>
                <el-button type="primary" :loading="validatingDsl" @click="validatePromptDsl">校验</el-button>
              </el-space>
            </div>
          </template>
          <PromptFlowBuilder
            :modules="modules"
            @add="openAddDialog"
            @reorder="onReorder"
            @toggleEnabled="onToggleEnabled"
            @remove="onRemove"
            @edit="onEdit"
            @contentChange="onContentChange"
            @contentSave="onContentSave"
            @titleChange="onTitleChange"
            @titleSave="onTitleSave"
          />
          <div v-if="dslValidate" class="validate-box">
            <el-alert :type="dslValidate.valid ? 'success' : 'warning'" :closable="false" :title="dslValidate.valid ? 'Prompt DSL 校验通过' : 'Prompt DSL 校验未通过'" />
            <el-scrollbar max-height="120px" class="error-scroll">
              <div v-if="!dslValidate.errors.length" class="error-line">无错误</div>
              <div v-for="(error, idx) in dslValidate.errors" :key="idx" class="error-line">{{ idx + 1 }}. {{ error }}</div>
            </el-scrollbar>
          </div>
        </el-card>

        <el-card shadow="never" class="builder-card">
          <template #header>
            <div class="builder-header">
              <span>② 可视化运行配置</span>
              <el-button type="primary" :loading="executingWorkflow" @click="executeWorkflowDsl">执行调试</el-button>
            </div>
          </template>
          <el-form label-width="96px" class="runner-form">
            <el-form-item label="Workflow ID">
              <el-input v-model="workflowId" />
            </el-form-item>
            <el-form-item label="输入字段名">
              <el-input v-model="workflowInputKey" />
            </el-form-item>
            <el-form-item label="输入参数(JSON)">
              <el-input v-model="workflowInputsText" type="textarea" :rows="4" />
            </el-form-item>
          </el-form>
          <el-collapse>
            <el-collapse-item title="高级模式（YAML 预览）" name="yaml">
              <el-form label-width="96px">
                <el-form-item label="Prompt DSL">
                  <el-input :model-value="promptDslText" type="textarea" :rows="10" readonly />
                </el-form-item>
                <el-form-item label="Workflow DSL">
                  <el-input :model-value="workflowDslText" type="textarea" :rows="10" readonly />
                </el-form-item>
              </el-form>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never" class="builder-card">
          <template #header>
            <div class="builder-header">
              <span>③ 运行结果</span>
              <el-tag v-if="workflowResult" :type="workflowResult.success ? 'success' : 'danger'">
                {{ workflowResult.success ? '成功' : '失败' }}
              </el-tag>
            </div>
          </template>
          <div class="metrics">
            <div class="metric-item">
              <span>总耗时</span>
              <strong>{{ workflowResult ? `${workflowResult.durationMs} ms` : '-' }}</strong>
            </div>
            <div class="metric-item">
              <span>节点数</span>
              <strong>{{ workflowResult ? workflowResult.nodeTraces.length : 0 }}</strong>
            </div>
          </div>
          <el-input v-model="workflowOutputText" type="textarea" :rows="10" readonly />
          <el-divider>节点追踪</el-divider>
          <el-table :data="workflowResult?.nodeTraces || []" size="small" height="220">
            <el-table-column prop="nodeId" label="节点" min-width="100" />
            <el-table-column prop="nodeType" label="类型" width="80" />
            <el-table-column prop="durationMs" label="耗时(ms)" width="96" />
          </el-table>
        </el-card>

        <el-card shadow="never" class="builder-card">
          <template #header>
            <div class="builder-header">
              <span>④ 兼容模块资产</span>
              <el-tag type="info" effect="plain">用于迁移</el-tag>
            </div>
          </template>
          <el-scrollbar max-height="300px">
            <div v-for="item in modules" :key="item.id" class="module-row">
              <el-tag size="small" :type="item.enabled ? 'success' : 'info'">{{ item.id }}</el-tag>
              <span class="module-title">{{ item.name }}</span>
            </div>
          </el-scrollbar>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="addDialogVisible" title="新增模块" width="560px">
      <el-form label-width="92px">
        <el-form-item label="模块名称">
          <el-input v-model="newModule.name" placeholder="例如：违规规则判断" />
        </el-form-item>
        <el-form-item label="模块类型">
          <el-select v-model="newModule.type" style="width: 100%">
            <el-option label="BASE" value="BASE" />
            <el-option label="RULE" value="RULE" />
            <el-option label="JSON" value="JSON" />
            <el-option label="TONE" value="TONE" />
          </el-select>
        </el-form-item>
        <el-form-item label="模块内容">
          <el-input v-model="newModule.content" type="textarea" :rows="8" placeholder="输入模块 Prompt 内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="onAddConfirm">确定新增</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="编辑模块" width="560px">
      <el-form label-width="92px">
        <el-form-item label="模块编码">
          <el-input v-model="editModule.id" readonly />
        </el-form-item>
        <el-form-item label="模块名称">
          <el-input v-model="editModule.name" placeholder="例如：违规规则判断" />
        </el-form-item>
        <el-form-item label="模块类型">
          <el-select v-model="editModule.type" style="width: 100%">
            <el-option label="BASE" value="BASE" />
            <el-option label="RULE" value="RULE" />
            <el-option label="JSON" value="JSON" />
            <el-option label="TONE" value="TONE" />
          </el-select>
        </el-form-item>
        <el-form-item label="模块内容">
          <el-input v-model="editModule.content" type="textarea" :rows="10" placeholder="输入模块 Prompt 内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editing" @click="onEditConfirm">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { promptApi, type PromptDslValidateRes, type PromptModuleItem, type WorkflowExecuteRes } from '@/api/prompts'
import PromptFlowBuilder, { type PromptFlowModule } from './components/PromptFlowBuilder.vue'

type FlowType = PromptFlowModule['type']

const analysisType = ref('HOST_VIOLATION')
const analysisTypeOptions = [
  { value: 'HOST_VIOLATION', label: '主播违规识别 Prompt' },
  { value: 'STANDARD', label: '通用审核 Prompt' }
]

const loading = ref(false)
const adding = ref(false)
const validatingDsl = ref(false)
const executingWorkflow = ref(false)
const modules = ref<PromptFlowModule[]>([])
const dslValidate = ref<PromptDslValidateRes | null>(null)
const workflowResult = ref<WorkflowExecuteRes | null>(null)
const workflowId = ref('host_violation_workflow')
const workflowInputKey = ref('text')
const workflowInputsText = ref('{"text":"这是一个用于测试 Workflow 执行能力的输入文本"}')
const addDialogVisible = ref(false)
const editDialogVisible = ref(false)
const newModule = ref<{ name: string; type: FlowType; content: string }>({
  name: '',
  type: 'RULE',
  content: ''
})
const editing = ref(false)
const editModule = ref<PromptFlowModule>({
  id: '',
  name: '',
  type: 'RULE',
  enabled: true,
  content: '',
  order: 1
})

const typeToCategory = (type: FlowType): PromptModuleItem['category'] => {
  if (type === 'BASE') return 'REQUIRED'
  if (type === 'TONE') return 'FREE'
  return 'PLUGGABLE'
}

const inferType = (item: PromptModuleItem): FlowType => {
  if (item.category === 'REQUIRED') return 'BASE'
  if (item.category === 'FREE') return 'TONE'
  const raw = `${item.code} ${item.title} ${item.content}`.toLowerCase()
  if (raw.includes('json')) return 'JSON'
  return 'RULE'
}

const toFlowModule = (item: PromptModuleItem): PromptFlowModule => ({
  id: item.code,
  name: item.title,
  type: inferType(item),
  enabled: item.enabled,
  content: item.content,
  order: item.sortOrder
})

const toSortedModules = (list: PromptFlowModule[]) =>
  list
    .map((item, index) => ({ ...item, order: index + 1 }))
    .sort((a, b) => a.order - b.order)

const enabledModules = computed(() => modules.value.filter(item => item.enabled).sort((a, b) => a.order - b.order))

const indentBlock = (text: string) => (text || '').split('\n').map(line => `    ${line}`).join('\n')

const promptDslListText = computed(() =>
  enabledModules.value
    .map(item => {
      const inputKey = workflowInputKey.value || 'text'
      return `id: ${item.id}_prompt
type: prompt
version: 1
model:
  name: gpt-4o
  temperature: 0.3
input_schema:
  ${inputKey}: string
template:
  system: |
${indentBlock('你是视频通话健康分违规识别助手')}
  instruction: |
${indentBlock(item.content || '')}
output_schema:
  result: string`
    })
    .join('\n---\n')
)

const promptDslText = computed(() => promptDslListText.value.split('\n---\n')[0] || '')

const workflowDslText = computed(() => {
  const inputKey = workflowInputKey.value || 'text'
  const nodes = enabledModules.value.map((item, idx) => {
    const inputExpr = idx === 0 ? `{{context.${inputKey}}}` : `{{${enabledModules.value[idx - 1].id}.output}}`
    return `  - id: ${item.id}
    type: llm
    prompt_ref: ${item.id}_prompt
    input_mapping:
      ${inputKey}: "${inputExpr}"
    output_key: out_${idx + 1}`
  })
  const lastNodeId = enabledModules.value.length ? enabledModules.value[enabledModules.value.length - 1].id : 'node_1'
  return `id: ${workflowId.value || 'dynamic_workflow'}
type: workflow
version: 1
context:
  inputs:
    ${inputKey}: string
nodes:
${nodes.join('\n')}
output:
  result: "{{${lastNodeId}.output}}"`
})

const workflowOutputText = computed(() => {
  if (!workflowResult.value) return ''
  return JSON.stringify(
    {
      success: workflowResult.value.success,
      output: workflowResult.value.output,
      errorMessage: workflowResult.value.errorMessage || ''
    },
    null,
    2
  )
})

const loadModules = async () => {
  loading.value = true
  try {
    const data = await promptApi.getModules(analysisType.value)
    modules.value = (data.modules || []).map(toFlowModule).sort((a, b) => a.order - b.order)
  } catch (e: any) {
    ElMessage.error(e?.message || '加载模块失败')
  } finally {
    loading.value = false
  }
}

const persistModule = async (item: PromptFlowModule) => {
  await promptApi.updateModule(analysisType.value, item.id, {
    title: item.name,
    content: item.content,
    enabled: item.enabled,
    category: typeToCategory(item.type),
    sortOrder: item.order
  })
}

const persistOrders = async () => {
  const tasks = modules.value.map(item =>
    promptApi.updateModule(analysisType.value, item.id, {
      sortOrder: item.order
    })
  )
  await Promise.all(tasks)
}

const onReorder = async (next: PromptFlowModule[]) => {
  modules.value = toSortedModules(next)
  try {
    await persistOrders()
    ElMessage.success('排序已更新')
  } catch (e: any) {
    ElMessage.error(e?.message || '排序更新失败')
    await loadModules()
  }
}

const onToggleEnabled = async (item: PromptFlowModule, enabled: boolean) => {
  item.enabled = enabled
  try {
    await persistModule(item)
    ElMessage.success(enabled ? '模块已启用' : '模块已禁用')
  } catch (e: any) {
    ElMessage.error(e?.message || '状态更新失败')
    item.enabled = !enabled
  }
}

const onRemove = async (item: PromptFlowModule) => {
  try {
    await ElMessageBox.confirm(`确认删除模块「${item.name}」？`, '删除确认', { type: 'warning' })
    await promptApi.deleteModule(analysisType.value, item.id)
    modules.value = toSortedModules(modules.value.filter(m => m.id !== item.id))
    await persistOrders()
    ElMessage.success('模块已删除')
  } catch (e: any) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '删除失败')
  }
}

const onEdit = (item: PromptFlowModule) => {
  editModule.value = { ...item }
  editDialogVisible.value = true
}

const onEditConfirm = async () => {
  if (!editModule.value.name.trim() || !editModule.value.content.trim()) {
    ElMessage.warning('请填写模块名称和内容')
    return
  }
  editing.value = true
  try {
    const index = modules.value.findIndex(item => item.id === editModule.value.id)
    if (index >= 0) {
      modules.value[index] = { ...modules.value[index], ...editModule.value }
      await persistModule(modules.value[index])
    }
    editDialogVisible.value = false
    ElMessage.success('模块修改成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '模块修改失败')
  } finally {
    editing.value = false
  }
}

const onContentChange = (item: PromptFlowModule, content: string) => {
  item.content = content
}

const onContentSave = async (item: PromptFlowModule) => {
  try {
    await persistModule(item)
    ElMessage.success('内容已保存')
  } catch (e: any) {
    ElMessage.error(e?.message || '内容保存失败')
  }
}

const onTitleChange = (item: PromptFlowModule, title: string) => {
  item.name = title
}

const onTitleSave = async (item: PromptFlowModule) => {
  try {
    await persistModule(item)
    ElMessage.success('名称已保存')
  } catch (e: any) {
    ElMessage.error(e?.message || '名称保存失败')
  }
}

const openAddDialog = () => {
  newModule.value = { name: '', type: 'RULE', content: '' }
  addDialogVisible.value = true
}

const buildCode = (name: string) => {
  const base = name
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9\u4e00-\u9fa5]+/g, '_')
    .replace(/^_+|_+$/g, '')
  const suffix = Date.now().toString().slice(-6)
  return `${base || 'module'}_${suffix}`
}

const onAddConfirm = async () => {
  if (!newModule.value.name.trim() || !newModule.value.content.trim()) {
    ElMessage.warning('请填写模块名称和内容')
    return
  }
  adding.value = true
  try {
    const code = buildCode(newModule.value.name)
    await promptApi.createModule({
      analysisType: analysisType.value,
      code,
      title: newModule.value.name.trim(),
      content: newModule.value.content,
      enabled: true,
      category: typeToCategory(newModule.value.type),
      sortOrder: modules.value.length + 1
    })
    addDialogVisible.value = false
    await loadModules()
    ElMessage.success('模块创建成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '模块创建失败')
  } finally {
    adding.value = false
  }
}

const validatePromptDsl = async () => {
  if (!enabledModules.value.length) {
    ElMessage.warning('请至少启用一个模块')
    return
  }
  validatingDsl.value = true
  try {
    const promptErrors: string[] = []
    const promptParts = promptDslListText.value
      .split('\n---\n')
      .map(item => item.trim())
      .filter(Boolean)
    for (const dsl of promptParts) {
      const res = await promptApi.validateDsl(dsl)
      if (!res.valid) promptErrors.push(...res.errors)
    }
    const workflowRes = await promptApi.validateDsl(workflowDslText.value)
    dslValidate.value = {
      dslType: 'COMPOSITE',
      valid: promptErrors.length === 0 && workflowRes.valid,
      errors: [...promptErrors, ...(workflowRes.errors || [])]
    }
    ElMessage.success(dslValidate.value.valid ? '校验通过' : '校验未通过')
  } catch (e: any) {
    ElMessage.error(e?.message || '校验失败')
  } finally {
    validatingDsl.value = false
  }
}

const executeWorkflowDsl = async () => {
  if (!enabledModules.value.length) {
    ElMessage.warning('请至少启用一个模块')
    return
  }
  let inputs: Record<string, any> = {}
  try {
    inputs = workflowInputsText.value.trim() ? JSON.parse(workflowInputsText.value) : {}
  } catch {
    ElMessage.error('输入参数必须为合法 JSON')
    return
  }
  const promptDsls = promptDslListText.value
    .split('\n---\n')
    .map(item => item.trim())
    .filter(Boolean)
  executingWorkflow.value = true
  try {
    workflowResult.value = await promptApi.executeWorkflow({
      workflowDsl: workflowDslText.value,
      promptDsls,
      inputs
    })
    ElMessage.success(workflowResult.value.success ? '执行成功' : '执行失败')
  } catch (e: any) {
    ElMessage.error(e?.message || '执行失败')
  } finally {
    executingWorkflow.value = false
  }
}

onMounted(loadModules)
</script>

<style scoped>
.prompt-manage-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.header-card,
.builder-card {
  border-radius: 16px;
  border: 0.5px solid #dde5f1;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.06);
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.title {
  margin: 0;
  font-size: 24px;
  line-height: 1.2;
  color: #0f172a;
}

.subtitle {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
}

.scene-select {
  width: 220px;
}

.builder-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.validate-box {
  margin-top: 10px;
}

.error-scroll {
  margin-top: 8px;
}

.error-line {
  color: #334155;
  font-size: 12px;
  line-height: 1.6;
}

.runner-form {
  margin-bottom: 8px;
}

.metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 10px;
}

.metric-item {
  border: 0.5px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.module-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
}

.module-title {
  color: #334155;
  font-size: 13px;
}
</style>
