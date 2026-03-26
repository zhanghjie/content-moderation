<template>
  <div class="task-new-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-page-header @back="router.back()">
        <template #content>
          <span class="page-title">发起视频分析</span>
        </template>
      </el-page-header>
    </div>

    <el-card shadow="never">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        style="max-width: 600px"
      >
        <el-form-item label="分析模式" prop="analysisType">
          <el-radio-group v-model="form.analysisType">
            <el-radio-button label="STANDARD">标准分析</el-radio-button>
            <el-radio-button label="HOST_VIOLATION">主播违规识别</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.analysisType !== 'HOST_VIOLATION'" label="Call ID" prop="callId">
          <el-input v-model="form.callId" placeholder="请输入 Call ID" />
        </el-form-item>
        
        <el-form-item label="Content ID" prop="contentId">
          <el-input
            v-model="form.contentId"
            :placeholder="form.analysisType === 'HOST_VIOLATION' ? '请输入 Content ID（将作为 Call ID）' : '请输入 Content ID'"
          />
        </el-form-item>

        <el-form-item v-if="form.analysisType === 'HOST_VIOLATION'" label="User ID" prop="userId">
          <el-input v-model.number="form.userId" placeholder="请输入主播 User ID" />
        </el-form-item>
        
        <el-form-item label="视频 URL" prop="videoUrl">
          <el-input v-model="form.videoUrl" placeholder="请输入视频地址" />
        </el-form-item>
        
        <el-form-item label="封面 URL">
          <el-input v-model="form.coverUrl" placeholder="请输入封面地址（可选）" />
        </el-form-item>

        <el-form-item v-if="form.analysisType === 'HOST_VIOLATION'" label="Prompt模块">
          <el-checkbox-group v-model="form.promptModules">
            <el-checkbox v-for="m in promptModules" :key="m.code" :label="m.code">
              {{ m.title }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting" :disabled="submitting">
            <el-icon><Position /></el-icon>
            创建任务并异步执行
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 说明信息 -->
    <el-card shadow="never" class="info-card">
      <template #header>
        <span>分析说明</span>
      </template>
      <el-alert type="info" :closable="false">
        <ul style="margin: 0; padding-left: 20px;">
          <li>支持的视频格式：MP4、MOV、AVI</li>
          <li>视频大小限制：500MB 以内</li>
          <li>分析时长：约 10-30 秒</li>
          <li>检测维度：环境、行为、内容等 13 种违规类型</li>
        </ul>
      </el-alert>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Position } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { videoApi } from '@/api/video'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const promptModules = ref<Array<{ code: string; title: string }>>([])

const form = reactive({
  analysisType: 'STANDARD' as 'STANDARD' | 'HOST_VIOLATION',
  callId: '',
  contentId: '',
  userId: undefined as number | undefined,
  videoUrl: '',
  coverUrl: '',
  promptModules: [] as string[]
})

const rules: FormRules = {
  analysisType: [{ required: true, message: '请选择分析模式', trigger: 'change' }],
  callId: [
    {
      validator: (_rule, value, callback) => {
        if (form.analysisType === 'HOST_VIOLATION') return callback()
        if (!value) return callback(new Error('请输入 Call ID'))
        callback()
      },
      trigger: 'blur'
    }
  ],
  contentId: [{ required: true, message: '请输入 Content ID', trigger: 'blur' }],
  userId: [
    {
      validator: (_rule, value, callback) => {
        if (form.analysisType !== 'HOST_VIOLATION') return callback()
        if (!value) return callback(new Error('请输入主播 User ID'))
        callback()
      },
      trigger: 'blur'
    }
  ],
  videoUrl: [
    { required: true, message: '请输入视频地址', trigger: 'blur' },
    { type: 'url', message: '请输入有效的 URL', trigger: 'blur' }
  ]
}

watch(
  () => form.analysisType,
  async (type) => {
    if (type === 'HOST_VIOLATION') {
      const res = await videoApi.getPromptModules('HOST_VIOLATION')
      promptModules.value = res.modules || []
      form.promptModules = (res.defaultModules || []).slice()
      form.callId = form.contentId
    } else {
      promptModules.value = []
      form.promptModules = []
      form.userId = undefined
    }
  },
  { immediate: true }
)

watch(
  () => form.contentId,
  (val) => {
    if (form.analysisType === 'HOST_VIOLATION') {
      form.callId = val
    }
  }
)

async function handleSubmit() {
  if (submitting.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  
  submitting.value = true
  try {
    const task = await videoApi.analyze({
      callId: form.analysisType === 'HOST_VIOLATION' ? form.contentId : form.callId,
      contentId: form.contentId,
      analysisType: form.analysisType,
      userId: form.analysisType === 'HOST_VIOLATION' ? form.userId : undefined,
      promptModules: form.analysisType === 'HOST_VIOLATION' ? form.promptModules : undefined,
      videoUrl: form.videoUrl,
      coverUrl: form.coverUrl || undefined
    })
    ElMessage.success(`任务已创建，状态：${task.status || 'PENDING'}，正在异步执行`)
    router.push(`/video/${task.callId}`)
  } catch (error) {
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

function handleReset() {
  formRef.value?.resetFields()
}
</script>

<style scoped>
.task-new-page {
  padding: 0;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.info-card {
  margin-top: 20px;
}
</style>
