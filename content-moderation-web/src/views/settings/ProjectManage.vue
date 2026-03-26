<template>
  <div class="project-manage-page">
    <el-card shadow="never" class="header-card">
      <div class="header-row">
        <div>
          <h2 class="title">项目管理</h2>
          <p class="subtitle">为智能治理分析任务提供项目选择能力</p>
        </div>
        <el-space>
          <el-button :loading="loading" @click="loadData">刷新</el-button>
          <el-button type="primary" @click="openCreateDialog">新增项目</el-button>
        </el-space>
      </div>
    </el-card>

    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="panel-header">
          <span>项目列表</span>
          <el-tag effect="plain">{{ projects.length }} 个</el-tag>
        </div>
      </template>

      <el-table :data="projects" row-key="projectId" v-loading="loading" max-height="520">
        <el-table-column prop="projectId" label="项目ID" min-width="160" show-overflow-tooltip />
        <el-table-column prop="name" label="项目名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="plain">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button link type="primary" @click="toggleStatus(row)">
                {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
              </el-button>
              <el-button link type="danger" @click="removeProject(row.projectId)">删除</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!projects.length" description="暂无项目，请先新增项目" :image-size="96" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增项目' : '编辑项目'" width="560px">
      <el-form label-width="100px">
        <el-form-item label="项目ID" required>
          <el-input v-model="form.projectId" placeholder="例如：video_moderation" :disabled="dialogMode === 'edit'" />
        </el-form-item>
        <el-form-item label="项目名称" required>
          <el-input v-model="form.name" placeholder="例如：视频治理项目" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入项目描述" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'edit'" label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-space>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveProject">保存</el-button>
        </el-space>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { projectApi, type ProjectItem } from '@/api/project'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingProjectId = ref('')
const projects = ref<ProjectItem[]>([])
const form = reactive({
  projectId: '',
  name: '',
  description: '',
  status: 'ACTIVE' as 'ACTIVE' | 'INACTIVE'
})

async function loadData() {
  loading.value = true
  try {
    const res = await projectApi.listProjects()
    projects.value = res.projects || []
  } catch (error: any) {
    ElMessage.error(error?.message || '加载项目失败')
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  dialogMode.value = 'create'
  editingProjectId.value = ''
  form.projectId = ''
  form.name = ''
  form.description = ''
  form.status = 'ACTIVE'
  dialogVisible.value = true
}

function openEditDialog(row: ProjectItem) {
  dialogMode.value = 'edit'
  editingProjectId.value = row.projectId
  form.projectId = row.projectId
  form.name = row.name
  form.description = row.description
  form.status = row.status
  dialogVisible.value = true
}

async function saveProject() {
  if (!form.projectId.trim()) {
    ElMessage.warning('请输入项目ID')
    return
  }
  if (!form.name.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }
  saving.value = true
  try {
    if (dialogMode.value === 'edit') {
      const res = await projectApi.updateProject(editingProjectId.value, {
        name: form.name.trim(),
        description: form.description.trim(),
        status: form.status
      })
      projects.value = res.projects || []
    } else {
      const res = await projectApi.createProject({
        projectId: form.projectId.trim(),
        name: form.name.trim(),
        description: form.description.trim(),
        status: 'ACTIVE'
      })
      projects.value = res.projects || []
    }
    dialogVisible.value = false
    ElMessage.success(dialogMode.value === 'edit' ? '项目已更新' : '项目创建成功')
  } catch (error: any) {
    ElMessage.error(error?.message || '保存项目失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: ProjectItem) {
  const nextStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  try {
    const res = await projectApi.updateProject(row.projectId, {
      name: row.name,
      description: row.description,
      status: nextStatus
    })
    projects.value = res.projects || []
    ElMessage.success(nextStatus === 'ACTIVE' ? '项目已启用' : '项目已停用')
  } catch (error: any) {
    ElMessage.error(error?.message || '更新项目状态失败')
  }
}

async function removeProject(projectId: string) {
  try {
    await ElMessageBox.confirm(`确认删除项目 ${projectId} 吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await projectApi.deleteProject(projectId)
    projects.value = res.projects || []
    ElMessage.success('项目已删除')
  } catch (error: any) {
    ElMessage.error(error?.message || '删除项目失败')
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.project-manage-page {
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
  gap: 12px;
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
</style>
