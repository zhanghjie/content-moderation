<template>
  <div class="skill-factory-page">
    <el-card shadow="never" class="header-card">
      <div class="header-row">
        <div>
          <h1 class="title">Skill 注册工厂</h1>
          <p class="subtitle">此页用于创建、管理和部署各类可执行技能</p>
        </div>
        <el-space>
          <el-button :loading="loading" @click="loadSkills">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增技能</el-button>
        </el-space>
      </div>
    </el-card>

    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="panel-header">
          <h2 class="panel-title">技能列表</h2>
        </div>
      </template>
      <el-table :data="pagedSkills" row-key="skillId" max-height="480">
        <el-table-column prop="name" label="技能名称" min-width="140" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            {{ getTypeText(row.type) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            {{ getStatusText(row.status) }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="deleteSkill(row.skillId)">删除</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <el-pagination
          v-model:current-page="currentPage"
          :total="skills.length"
          :page-size="pageSize"
          layout="total, prev, pager, next, sizes"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="(val) => pageSize = val"
        />
      </div>
    </el-card>

    <el-dialog v-model="skillDialogVisible" :title="selectedSkillId ? `编辑技能：${selectedSkillId}` : '新增技能'" width="760px">
      <el-form label-width="110px">
        <el-form-item label="技能名称">
          <el-input v-model="form.name" placeholder="例如：视频解析" />
        </el-form-item>
        <el-form-item label="技能描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="描述技能能力和用途（必填）" />
        </el-form-item>
        <el-form-item label="技能类型">
          <el-select v-model="form.type" style="width: 100%" placeholder="请选择能力类型">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-if="selectedSkillId" v-model="form.status" style="width: 100%">
            <el-option label="启用" value="PUBLISHED" />
            <el-option label="未启用" value="DRAFT" />
          </el-select>
          <el-tag v-else type="info">未启用</el-tag>
        </el-form-item>
        <el-form-item label="大模型">
          <el-select v-model="form.llmConfigCode" style="width: 100%" placeholder="请选择模型配置" :loading="loadingModels">
            <el-option
              v-for="item in llmProfiles"
              :key="item.configCode"
              :label="`${item.displayName}（${item.model}${item.isDefault ? ' / 默认' : ''}）`"
              :value="item.configCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Prompt">
          <el-input v-model="promptText" type="textarea" :rows="6" placeholder="输入给LLM的提示词模板" />
        </el-form-item>
        <el-form-item label="返回Schema">
          <el-input v-model="outputSchemaText" type="textarea" :rows="6" placeholder='例如：{"result":"string"}' />
        </el-form-item>
        <el-form-item label="脚本文件">
          <el-space direction="vertical" alignment="start" style="width: 100%">
            <el-upload
              :show-file-list="false"
              :auto-upload="false"
              accept=".py,text/x-python"
              :on-change="onScriptFileChange"
            >
              <el-button>上传 .py 文件</el-button>
            </el-upload>
            <el-button v-if="scriptFileName" type="link" @click="scriptPreviewVisible = true">
              预览文件：{{ scriptFileName }}
            </el-button>
          </el-space>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-space>
          <el-button @click="skillDialogVisible = false">取消</el-button>
          <el-button :loading="saving" @click="saveDraft">保存草稿</el-button>
          <el-button type="primary" :loading="saving" @click="saveSkill">{{ selectedSkillId ? '保存修改' : '创建技能' }}</el-button>
        </el-space>
      </template>
    </el-dialog>

    <el-dialog v-model="scriptPreviewVisible" :title="`脚本预览：${scriptFileName}`" width="800px">
      <el-input v-model="scriptContentText" type="textarea" :rows="20" readonly />
      <template #footer>
        <el-button @click="scriptPreviewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { skillOsApi, type SkillDefinition } from '@/api/skillos'
import { settingsApi, type LlmProfile } from '@/api/settings'

type SkillFactoryForm = {
  name: string
  description: string
  type: string
  status: string
  llmConfigCode: string
}

const typeOptions: Array<{ value: string; label: string }> = [
  { value: 'PERCEPTION', label: '感知类' },
  { value: 'SEMANTIC', label: '语义类' },
  { value: 'DECISION', label: '决策类' },
  { value: 'GUARD', label: '校验类' },
  { value: 'OUTPUT', label: '输出类' }
]

const loading = ref(false)
const saving = ref(false)
const loadingModels = ref(false)
const skills = ref<SkillDefinition[]>([])
const llmProfiles = ref<LlmProfile[]>([])
const defaultLlmConfigCode = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const selectedSkillId = ref('')
const skillDialogVisible = ref(false)
const scriptPreviewVisible = ref(false)
const promptText = ref('')
const outputSchemaText = ref('{}')
const scriptContentText = ref('print("hello skill")')
const scriptFileName = ref('')
const route = useRoute()
const router = useRouter()

const form = ref<SkillFactoryForm>({
  name: '',
  description: '',
  type: '',
  status: 'DRAFT',
  llmConfigCode: ''
})

const pagedSkills = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return skills.value.slice(start, start + pageSize.value)
})
const totalPages = computed(() => Math.max(1, Math.ceil(skills.value.length / pageSize.value)))

const getTypeText = (type: string) => {
  const found = typeOptions.find(item => item.value === String(type || '').toUpperCase())
  return found?.label || String(type || '-')
}

const getStatusText = (status: string) => {
  const value = String(status || '').toUpperCase()
  if (value === 'PUBLISHED') return '启用'
  if (value === 'DRAFT') return '未启用'
  return status || '-'
}

const normalizeJsonText = (text: string) => {
  const trimmed = String(text || '').trim()
  if (!trimmed) return '{}'
  const withoutBom = trimmed.replace(/^\uFEFF/, '')
  const fencedMatch = withoutBom.match(/^```(?:json)?\s*([\s\S]*?)\s*```$/i)
  return fencedMatch ? fencedMatch[1].trim() : withoutBom
}

const parseObjectJson = (text: string, fieldName: string) => {
  try {
    const normalized = normalizeJsonText(text)
    const parsed = JSON.parse(normalized)
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
      throw new Error(`${fieldName} 必须是 JSON 对象`)
    }
    return parsed as Record<string, any>
  } catch (error: any) {
    throw new Error(`${fieldName} 不是合法 JSON 对象，请输入类似 {"result":"string"} 的内容`)
  }
}

const loadSkills = async () => {
  loading.value = true
  try {
    const res = await skillOsApi.listSkills()
    skills.value = (res.skills || []).slice().sort((a, b) => b.skillId.localeCompare(a.skillId))
    const maxPage = Math.max(1, Math.ceil(skills.value.length / pageSize.value))
    if (currentPage.value > maxPage) {
      currentPage.value = maxPage
    }
  } finally {
    loading.value = false
  }
}

const loadLlmProfiles = async () => {
  loadingModels.value = true
  try {
    const res = await settingsApi.getApiConfig()
    defaultLlmConfigCode.value = String(res.defaultConfigCode || '')
    llmProfiles.value = (res.profiles || []).filter(item => item.enabled)
    if (!form.value.llmConfigCode) {
      form.value.llmConfigCode = defaultLlmConfigCode.value || llmProfiles.value[0]?.configCode || ''
    }
  } finally {
    loadingModels.value = false
  }
}

const saveSkill = async () => {
  await saveSkillInternal('register')
}

const saveDraft = async () => {
  await saveSkillInternal('draft')
}

const saveSkillInternal = async (mode: 'register' | 'draft') => {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入技能名称')
    return
  }
  if (!form.value.description.trim()) {
    ElMessage.warning('请输入技能描述')
    return
  }
  if (!form.value.type) {
    ElMessage.warning('请选择技能类型')
    return
  }
  if (!promptText.value.trim()) {
    ElMessage.warning('请输入 Prompt')
    return
  }
  if (!scriptContentText.value.trim()) {
    ElMessage.warning('请上传脚本文件')
    return
  }
  if (!form.value.llmConfigCode) {
    ElMessage.warning('请选择大模型')
    return
  }

  saving.value = true
  try {
    const outputSchema = parseObjectJson(outputSchemaText.value, '返回Schema')
    const generatedId = `${String(form.value.type || 'skill').toLowerCase()}_${Date.now()}`
    const skillId = selectedSkillId.value || generatedId
    const isEdit = !!selectedSkillId.value
    const selectedProfile = llmProfiles.value.find(item => item.configCode === form.value.llmConfigCode)
    const payload = {
      skillId,
      name: form.value.name.trim(),
      description: form.value.description.trim(),
      type: form.value.type,
      tags: [],
      status: mode === 'draft' ? 'DRAFT' : form.value.status,
      version: 'v1',
      timeoutMs: 3000,
      outputSchema,
      stateMapping: {},
      executionConfig: {
        execution_mode: 'LLM',
        timeout: 3000,
        llm_config_code: form.value.llmConfigCode,
        llm_model: selectedProfile?.model || ''
      },
      scriptConfig: {
        language: 'python',
        prompt: promptText.value.trim(),
        content: scriptContentText.value,
        env: {}
      }
    }
    if (mode === 'register') {
      if (isEdit) {
        await skillOsApi.updateSkill(skillId, payload)
      } else {
        await skillOsApi.registerSkill(payload)
      }
    } else {
      await skillOsApi.saveSkillDraft(payload)
    }
    if (mode === 'register') {
      const verifyRes = await skillOsApi.listSkills()
      const exists = (verifyRes.skills || []).some(item => item.skillId === skillId)
      if (!exists) {
        throw new Error('新增请求已提交，但未查询到保存结果，请重试')
      }
    }
    selectedSkillId.value = skillId
    ElMessage.success(mode === 'draft' ? '草稿保存成功' : `${isEdit ? '技能修改成功' : '技能创建成功'}：${skillId}`)
    if (mode === 'register') {
      await loadSkills()
      skillDialogVisible.value = false
      resetForm()
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '技能保存失败')
  } finally {
    saving.value = false
  }
}

const resetForm = () => {
  selectedSkillId.value = ''
  form.value = {
    name: '',
    description: '',
    type: '',
    status: 'DRAFT',
    llmConfigCode: defaultLlmConfigCode.value || llmProfiles.value[0]?.configCode || ''
  }
  outputSchemaText.value = '{}'
  promptText.value = ''
  scriptContentText.value = 'print("hello skill")'
  scriptFileName.value = ''
}

const openCreateDialog = () => {
  resetForm()
  skillDialogVisible.value = true
}

const openEditDialog = (row: SkillDefinition) => {
  selectedSkillId.value = row.skillId
  form.value = {
    name: row.name || '',
    description: row.description || '',
    type: row.type || '',
    status: String(row.status || 'DRAFT'),
    llmConfigCode: String((row.executionConfig as any)?.llm_config_code || defaultLlmConfigCode.value || llmProfiles.value[0]?.configCode || '')
  }
  outputSchemaText.value = JSON.stringify(row.outputSchema || {}, null, 2)
  promptText.value = String(row.scriptConfig?.prompt || '')
  scriptContentText.value = String(row.scriptConfig?.content || 'print("hello skill")')
  scriptFileName.value = ''
  skillDialogVisible.value = true
}

const applyRouteSkillSelection = () => {
  const mode = String(route.query.mode || '').toLowerCase()
  const skillId = String(route.query.skillId || '').trim()
  if (mode !== 'edit' || !skillId) {
    return
  }
  const target = skills.value.find(item => item.skillId === skillId)
  if (!target) {
    ElMessage.warning(`未找到 Skill：${skillId}`)
    return
  }
  openEditDialog(target)
}

const deleteSkill = async (skillId: string) => {
  try {
    await ElMessageBox.confirm(`确认物理删除技能 ${skillId} 吗？删除后不可恢复。`, '物理删除确认', { type: 'warning' })
    const res = await skillOsApi.deleteSkill(skillId)
    skills.value = (res.skills || []).slice().sort((a, b) => a.skillId.localeCompare(b.skillId))
    const maxPage = Math.max(1, Math.ceil(skills.value.length / pageSize.value))
    if (currentPage.value > maxPage) {
      currentPage.value = maxPage
    }
    ElMessage.success(`已物理删除：${skillId}`)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

const readFileAsText = (file: File) => {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('脚本文件读取失败'))
    reader.readAsText(file, 'utf-8')
  })
}

const onScriptFileChange = async (file: UploadFile) => {
  const raw = file.raw
  if (!raw) {
    ElMessage.warning('未读取到文件内容')
    return
  }
  const fileName = raw.name || ''
  if (!fileName.toLowerCase().endsWith('.py')) {
    ElMessage.warning('仅支持上传 .py 文件')
    return
  }
  try {
    scriptContentText.value = await readFileAsText(raw)
    scriptFileName.value = fileName
    ElMessage.success('脚本文件加载成功')
  } catch (error: any) {
    ElMessage.error(error?.message || '脚本文件读取失败')
  }
}

onMounted(async () => {
  await Promise.all([loadSkills(), loadLlmProfiles()])
  applyRouteSkillSelection()
})

watch(
  () => [route.query.mode, route.query.skillId],
  () => {
    applyRouteSkillSelection()
  }
)

watch(skillDialogVisible, visible => {
  if (!visible && String(route.query.mode || '').toLowerCase() === 'edit') {
    const nextQuery = { ...route.query }
    delete nextQuery.mode
    delete nextQuery.skillId
    router.replace({ path: '/settings/skills', query: nextQuery })
  }
})

</script>

<style scoped>
.skill-factory-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.header-card,
.panel-card {
  border-radius: 14px;
}

.header-row,
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  margin: 0;
  font-size: 22px;
  color: #1e293b;
  font-weight: 600;
}

.subtitle {
  margin-top: 6px;
  color: #64748b;
  font-size: 14px;
}

.panel-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.pagination-row {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
