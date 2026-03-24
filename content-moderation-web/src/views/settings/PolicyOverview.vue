<template>
  <div class="policy-overview-page">
    <el-card shadow="never" class="panel-card">
      <div class="overview-header">
        <div>
          <h2 class="title">Policy 编排总览</h2>
          <p class="subtitle">此页展示摘要，详细编排与新增编辑在独立页面完成</p>
        </div>
        <el-space>
          <el-button :loading="loadingPolicies" @click="loadPolicies">刷新</el-button>
          <el-button type="primary" @click="openEditorForCreate">新建编排</el-button>
          <el-button :disabled="!activePolicy" @click="openEditorForEdit">打开完整编排</el-button>
        </el-space>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="24" :md="24" :lg="8">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>Policy 列表</span>
              <el-tag effect="plain">{{ policies.length }} 个</el-tag>
            </div>
          </template>
          <el-scrollbar max-height="560px">
            <div
              v-for="item in policies"
              :key="item.policyId"
              class="policy-item"
              :class="{ active: activePolicy?.policyId === item.policyId }"
              @click="activePolicy = item"
            >
              <div class="policy-row">
                <div class="policy-title">{{ item.name || item.policyId }}</div>
                <el-button
                  text
                  type="danger"
                  size="small"
                  :loading="deletingPolicyId === item.policyId"
                  @click.stop="deletePolicy(item)"
                >
                  删除
                </el-button>
              </div>
              <div class="policy-meta">版本 {{ item.version || 'v1' }} · {{ item.skillPipeline.length }} 步</div>
            </div>
            <el-empty v-if="!policies.length" description="暂无 Policy" :image-size="84" />
          </el-scrollbar>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="24" :lg="16">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>流程摘要</span>
            </div>
          </template>
          <div v-if="activePolicy" class="summary-body">
            <div class="summary-top">
              <div class="summary-name clickable" @click="openEditorForEdit">{{ activePolicy.name || activePolicy.policyId }}</div>
              <el-tag type="info" effect="plain">{{ activePolicy.policyId }}</el-tag>
            </div>
            <div class="summary-flow">
              <div class="summary-node">起点</div>
              <template v-for="(skillId, index) in visibleSkills" :key="`${skillId}-${index}`">
                <div class="summary-arrow">→</div>
                <div class="summary-node skill">{{ getSkillDisplayName(skillId) }}</div>
              </template>
              <div v-if="remainingSteps > 0" class="summary-arrow">→</div>
              <div v-if="remainingSteps > 0" class="summary-node more">+{{ remainingSteps }} 步</div>
              <div class="summary-arrow">→</div>
              <div class="summary-node">结束</div>
            </div>
            <div class="summary-tip">当前页面仅展示摘要，新增与编辑请进入完整编排页面。</div>
          </div>
          <el-empty v-else description="请选择左侧 Policy 查看摘要" :image-size="96" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { skillOsApi, type PolicyDefinition, type SkillDefinition } from '@/api/skillos'

const router = useRouter()
const loadingPolicies = ref(false)
const loadingSkills = ref(false)
const deletingPolicyId = ref<string | null>(null)
const policies = ref<PolicyDefinition[]>([])
const skills = ref<SkillDefinition[]>([])
const activePolicy = ref<PolicyDefinition | null>(null)
const skillNameMap = computed(() => {
  const map = new Map<string, string>()
  skills.value.forEach(item => {
    map.set(item.skillId, item.name || item.skillId)
  })
  return map
})

const visibleSkills = computed(() => activePolicy.value?.skillPipeline.slice(0, 3) || [])
const remainingSteps = computed(() => {
  const total = activePolicy.value?.skillPipeline.length || 0
  return total > 3 ? total - 3 : 0
})

const getSkillDisplayName = (skillId: string) => {
  return skillNameMap.value.get(skillId) || skillId
}

const loadPolicies = async () => {
  loadingPolicies.value = true
  try {
    const res = await skillOsApi.listPolicies()
    policies.value = (res.policies || []).slice().sort((a, b) => a.policyId.localeCompare(b.policyId))
    if (!policies.value.length) {
      activePolicy.value = null
      return
    }
    if (activePolicy.value) {
      const matched = policies.value.find(item => item.policyId === activePolicy.value?.policyId)
      activePolicy.value = matched || policies.value[0]
      return
    }
    activePolicy.value = policies.value[0]
  } finally {
    loadingPolicies.value = false
  }
}

const loadSkills = async () => {
  loadingSkills.value = true
  try {
    const res = await skillOsApi.listSkills()
    skills.value = (res.skills || []).slice().sort((a, b) => a.skillId.localeCompare(b.skillId))
  } finally {
    loadingSkills.value = false
  }
}

const openEditorForCreate = () => {
  router.push({ path: '/settings/policies/editor', query: { mode: 'new' } })
}

const openEditorForEdit = () => {
  if (!activePolicy.value) return
  router.push({ path: '/settings/policies/editor', query: { policyId: activePolicy.value.policyId } })
}

const deletePolicy = async (policy: PolicyDefinition) => {
  const policyId = String(policy.policyId || '').trim()
  if (!policyId) return
  try {
    await ElMessageBox.confirm(`确认删除 Policy：${policy.name || policyId}（${policyId}）？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  deletingPolicyId.value = policyId
  try {
    const res = await skillOsApi.deletePolicy(policyId)
    policies.value = (res.policies || []).slice().sort((a, b) => a.policyId.localeCompare(b.policyId))
    if (!policies.value.length) {
      activePolicy.value = null
    } else if (activePolicy.value?.policyId === policyId) {
      activePolicy.value = policies.value[0]
    }
    ElMessage.success('Policy 已删除')
  } catch (error: any) {
    ElMessage.error(error?.message || 'Policy 删除失败')
  } finally {
    deletingPolicyId.value = null
  }
}

onMounted(async () => {
  await Promise.all([loadPolicies(), loadSkills()])
})
</script>

<style scoped>
.policy-overview-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px;
}

.panel-card {
  border-radius: 14px;
}

.overview-header,
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
}

.subtitle {
  margin: 6px 0 0;
  color: #64748b;
}

.policy-item {
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.policy-item.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.policy-title {
  color: #0f172a;
  font-weight: 600;
}

.policy-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.policy-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.summary-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.summary-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary-name {
  font-size: 16px;
  font-weight: 700;
  color: #2563eb;
}

.summary-name.clickable {
  cursor: pointer;
  transition: color 0.2s ease;
}

.summary-name.clickable:hover {
  color: #1d4ed8;
  text-decoration: underline;
}

.summary-flow {
  border: 1px dashed #bfdbfe;
  border-radius: 12px;
  background: #f8fbff;
  padding: 14px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.summary-node {
  border-radius: 999px;
  border: 1px solid #bfdbfe;
  color: #1e3a8a;
  background: #eff6ff;
  font-size: 12px;
  padding: 4px 10px;
}

.summary-node.skill {
  background: #ffffff;
  max-width: 210px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.summary-node.more {
  border-color: #cbd5e1;
  color: #475569;
  background: #f8fafc;
}

.summary-arrow {
  color: #94a3b8;
}

.summary-tip {
  color: #64748b;
  font-size: 12px;
}
</style>
