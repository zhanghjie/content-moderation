<template>
  <div class="api-config-page">
    <div class="header-grid pulse-card">
      <div class="page-title-wrapper">
        <h1 class="page-title">系统配置</h1>
        <p class="page-subtitle">保留原有配置结构，并升级 LLM 为多模型入库管理</p>
      </div>
      <el-space>
        <el-button @click="loadProfiles" :loading="loading">刷新</el-button>
      </el-space>
    </div>

    <el-row :gutter="16">
      <el-col :span="16">
        <el-card shadow="never" class="pulse-card config-card">
          <template #header>
            <div class="card-header">
              <span>LLM 配置</span>
              <el-space>
                <el-tag type="success" effect="plain">多模型</el-tag>
                <el-button type="primary" size="small" @click="openCreate">新增模型配置</el-button>
              </el-space>
            </div>
          </template>
          <el-table :data="profiles" stripe>
            <el-table-column prop="displayName" label="名称" min-width="160" />
            <el-table-column prop="configCode" label="编码" min-width="120" />
            <el-table-column label="Provider" min-width="110">
              <template #default="{ row }">
                {{ providerLabelMap[row.provider] || row.provider }}
              </template>
            </el-table-column>
            <el-table-column prop="model" label="Model" min-width="180" />
            <el-table-column prop="endpoint" label="Endpoint" min-width="280" show-overflow-tooltip />
            <el-table-column label="状态" min-width="120">
              <template #default="{ row }">
                <el-space>
                  <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">{{ row.enabled ? '启用' : '停用' }}</el-tag>
                  <el-tag v-if="row.isDefault" type="warning" effect="plain">默认</el-tag>
                </el-space>
              </template>
            </el-table-column>
            <el-table-column label="密钥" min-width="100">
              <template #default="{ row }">
                <el-tag :type="row.apiKeyConfigured ? 'success' : 'danger'" effect="plain">
                  {{ row.apiKeyConfigured ? '已配置' : '未配置' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="260" fixed="right">
              <template #default="{ row }">
                <el-space>
                  <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                  <el-button link type="primary" @click="testByProfile(row)" :loading="testingCode === row.configCode">测试连接</el-button>
                  <el-button link type="warning" :disabled="row.isDefault" @click="setDefault(row)">设为默认</el-button>
                  <el-button link type="danger" :disabled="row.isDefault" @click="removeProfile(row)">删除</el-button>
                </el-space>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never" class="pulse-card config-card">
          <template #header>
            <div class="card-header">
              <span>易盾配置</span>
              <el-tag :type="dunStatus ? 'success' : 'info'" effect="plain">{{ dunStatus ? '可连通' : '未测试' }}</el-tag>
            </div>
          </template>
          <el-form :model="dunConfig" label-width="90px">
            <el-form-item label="回调地址">
              <el-input v-model="dunConfig.callbackUrl" placeholder="https://your-domain.com/callback" />
            </el-form-item>
            <el-form-item label="超时时间">
              <el-input-number v-model="dunConfig.timeoutMs" :min="1000" :step="1000" style="width: 100%" />
            </el-form-item>
            <el-form-item>
              <el-space>
                <el-button type="primary" @click="handleTestDunConnection" :loading="testingDun">测试连接</el-button>
              </el-space>
            </el-form-item>
          </el-form>
          <el-alert type="info" :closable="false" title="该区域保留原页面能力；本轮仅升级 LLM 配置结构" />
        </el-card>

        <el-card shadow="never" class="pulse-card config-card">
          <template #header>
            <div class="card-header">
              <span>系统信息</span>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="系统名称">内容风控系统</el-descriptions-item>
            <el-descriptions-item label="运行状态">正常运行</el-descriptions-item>
            <el-descriptions-item label="配置说明">仅对 LLM 模块做多配置改造</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="editorVisible" :title="isEdit ? '编辑模型配置' : '新增模型配置'" width="720px">
      <el-form :model="editor" label-width="110px">
        <el-form-item label="配置编码">
          <el-input v-model="editor.configCode" :disabled="isEdit" placeholder="如 GPT_4O_MAIN" />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="editor.displayName" placeholder="如 OpenAI 主模型" />
        </el-form-item>
        <el-form-item label="Provider">
          <el-select v-model="editor.provider" class="full-width">
            <el-option v-for="item in providerOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="API Endpoint">
          <el-input v-model="editor.endpoint" placeholder="https://xxx" />
        </el-form-item>
        <el-form-item label="Model">
          <el-input v-model="editor.model" placeholder="模型名称" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input
            v-model="editor.apiKey"
            type="password"
            show-password
            :placeholder="isEdit && editor.apiKeyConfigured ? '已保存密钥，留空则不修改' : '请输入 API Key'"
          />
        </el-form-item>
        <el-form-item label="超时时间(ms)">
          <el-input-number v-model="editor.timeoutMs" :min="1000" :step="1000" />
        </el-form-item>
        <el-form-item label="最大Token">
          <el-input-number v-model="editor.maxTokens" :min="256" :step="256" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="editor.enabled" />
        </el-form-item>
        <el-form-item label="默认">
          <el-switch v-model="editor.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { settingsApi, type LlmProfile, type LlmProfilePayload } from '@/api/settings'

const loading = ref(false)
const saving = ref(false)
const editorVisible = ref(false)
const isEdit = ref(false)
const testingCode = ref('')
const testingDun = ref(false)
const dunStatus = ref(false)
const profiles = ref<LlmProfile[]>([])
const providerOptions = [
  { label: 'BytePlus', value: 'byteplus' },
  { label: 'OpenAI', value: 'openai' },
  { label: 'Anthropic', value: 'anthropic' },
  { label: 'DeepSeek', value: 'deepseek' },
  { label: 'Custom', value: 'custom' }
]
const providerLabelMap = providerOptions.reduce<Record<string, string>>((acc, item) => {
  acc[item.value] = item.label
  return acc
}, {})
const dunConfig = reactive({
  callbackUrl: '',
  timeoutMs: 60000
})

const editor = reactive<LlmProfilePayload & { apiKeyConfigured?: boolean }>({
  configCode: '',
  displayName: '',
  provider: 'byteplus',
  endpoint: '',
  model: '',
  apiKey: '',
  timeoutMs: 120000,
  maxTokens: 3000,
  enabled: true,
  isDefault: false,
  apiKeyConfigured: false
})

const normalizeProvider = (provider?: string) => {
  const value = provider?.trim().toLowerCase() || ''
  if (!value) return 'byteplus'
  if (value === 'deepseek' || value === 'deepseek-ai' || value === 'deepSeek'.toLowerCase()) return 'deepseek'
  if (providerLabelMap[value]) return value
  return 'custom'
}

const normalizeProfiles = (list: LlmProfile[]) =>
  list.map((item) => ({
    ...item,
    provider: normalizeProvider(item.provider)
  }))

const buildCreateEditor = (): LlmProfilePayload & { apiKeyConfigured?: boolean } => {
  const defaultProfile = profiles.value.find((item) => item.isDefault) || profiles.value[0]
  if (!defaultProfile) {
    return {
      configCode: '',
      displayName: '',
      provider: 'byteplus',
      endpoint: '',
      model: '',
      apiKey: '',
      timeoutMs: 120000,
      maxTokens: 3000,
      enabled: true,
      isDefault: false,
      apiKeyConfigured: false
    }
  }
  return {
    configCode: '',
    displayName: '',
    provider: normalizeProvider(defaultProfile.provider),
    endpoint: defaultProfile.endpoint,
    model: defaultProfile.model,
    apiKey: '',
    timeoutMs: defaultProfile.timeoutMs,
    maxTokens: defaultProfile.maxTokens,
    enabled: defaultProfile.enabled,
    isDefault: false,
    apiKeyConfigured: false
  }
}

const resetEditor = () => {
  const nextEditor = buildCreateEditor()
  editor.configCode = nextEditor.configCode
  editor.displayName = nextEditor.displayName
  editor.provider = nextEditor.provider
  editor.endpoint = nextEditor.endpoint
  editor.model = nextEditor.model
  editor.apiKey = nextEditor.apiKey || ''
  editor.timeoutMs = nextEditor.timeoutMs
  editor.maxTokens = nextEditor.maxTokens
  editor.enabled = nextEditor.enabled
  editor.isDefault = !!nextEditor.isDefault
  editor.apiKeyConfigured = !!nextEditor.apiKeyConfigured
}

const loadProfiles = async () => {
  loading.value = true
  try {
    const res = await settingsApi.getApiConfig()
    profiles.value = normalizeProfiles(res.profiles || [])
  } catch (e: any) {
    ElMessage.error(e?.message || '读取配置失败')
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  isEdit.value = false
  resetEditor()
  editorVisible.value = true
}

const openEdit = (row: LlmProfile) => {
  isEdit.value = true
  editor.configCode = row.configCode
  editor.displayName = row.displayName
  editor.provider = normalizeProvider(row.provider)
  editor.endpoint = row.endpoint
  editor.model = row.model
  editor.apiKey = ''
  editor.timeoutMs = row.timeoutMs
  editor.maxTokens = row.maxTokens
  editor.enabled = row.enabled
  editor.isDefault = row.isDefault
  editor.apiKeyConfigured = row.apiKeyConfigured
  editorVisible.value = true
}

const saveProfile = async () => {
  if (!editor.configCode.trim() || !editor.displayName.trim() || !editor.endpoint.trim() || !editor.model.trim()) {
    ElMessage.warning('请先填写完整信息')
    return
  }
  const code = editor.configCode.trim().toUpperCase()
  if (!isEdit.value && profiles.value.some((item) => item.configCode.toUpperCase() === code)) {
    ElMessage.warning('配置编码已存在，请更换编码后再新增')
    return
  }
  if (!isEdit.value && !editor.apiKey?.trim()) {
    ElMessage.warning('新增模型必须填写 API Key')
    return
  }
  saving.value = true
  try {
    const res = await settingsApi.saveApiConfig({
      configCode: code,
      displayName: editor.displayName.trim(),
      provider: normalizeProvider(editor.provider),
      endpoint: editor.endpoint.trim(),
      model: editor.model.trim(),
      apiKey: editor.apiKey?.trim() || undefined,
      timeoutMs: editor.timeoutMs,
      maxTokens: editor.maxTokens,
      enabled: editor.enabled,
      isDefault: editor.isDefault
    })
    profiles.value = normalizeProfiles(res.profiles || [])
    await loadProfiles()
    editorVisible.value = false
    const saved = profiles.value.some((item) => item.configCode.toUpperCase() === code)
    if (saved || isEdit.value) {
      ElMessage.success('保存成功')
    } else {
      ElMessage.warning('保存成功，但后端当前未返回新增编码，请检查是否仍为单模型接口')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const setDefault = async (row: LlmProfile) => {
  try {
    const res = await settingsApi.setDefaultApiConfig(row.configCode)
    profiles.value = normalizeProfiles(res.profiles || [])
    ElMessage.success('默认模型已更新')
  } catch (e: any) {
    ElMessage.error(e?.message || '设置默认失败')
  }
}

const removeProfile = async (row: LlmProfile) => {
  await ElMessageBox.confirm(`确认删除模型配置 ${row.displayName}？`, '提示', { type: 'warning' })
  try {
    const res = await settingsApi.deleteApiConfig(row.configCode)
    profiles.value = normalizeProfiles(res.profiles || [])
    ElMessage.success('已删除')
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

const testByProfile = async (row: LlmProfile) => {
  testingCode.value = row.configCode
  try {
    const res = await settingsApi.testLlm(row.endpoint, row.timeoutMs, row.configCode)
    if (res.success) {
      ElMessage.success(`连接成功 (${res.statusCode || 0})`)
    } else {
      ElMessage.error(res.message || '连接失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '连接失败')
  } finally {
    testingCode.value = ''
  }
}

const handleTestDunConnection = async () => {
  testingDun.value = true
  try {
    const res = await settingsApi.testYidun(dunConfig.callbackUrl, dunConfig.timeoutMs)
    dunStatus.value = !!res.success
    if (res.success) {
      ElMessage.success(`易盾连接测试成功 (${res.statusCode || 0})`)
    } else {
      ElMessage.error(res.message || '易盾连接失败')
    }
  } catch (e: any) {
    dunStatus.value = false
    ElMessage.error(e?.message || '易盾连接失败')
  } finally {
    testingDun.value = false
  }
}

onMounted(loadProfiles)
</script>

<style scoped>
.api-config-page {
  padding: 0;
}

.pulse-card {
  border-radius: 12px;
  border: 0.5px solid #dde3ee;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
}

.header-grid {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px;
}

.page-title-wrapper {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.page-title {
  font-size: 28px;
  font-weight: 650;
  color: #0f172a;
  margin: 0;
}

.page-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.full-width {
  width: 100%;
}

.config-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
@media (max-width: 768px) {
  .header-grid {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
