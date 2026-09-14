<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createActivity, deleteActivity, getMyActivities, submitActivity, updateActivity } from '../../api/activity'
import { generateActivityCopy } from '../../api/ai'
import { getActivityAttendances, getActivityRegistrations, issueCheckinToken } from '../../api/registration'
import type { Activity, ActivityPayload } from '../../types/activity'
import type { ActivityCopyRequest, ActivityCopyResponse } from '../../types/ai'
import type { Attendance, CheckinToken, Registration } from '../../types/registration'

const loading = ref(false)
const saving = ref(false)
const activities = ref<Activity[]>([])
const form = reactive<ActivityPayload>({
  title: '',
  description: '',
  location: '',
  startTime: '',
  endTime: '',
  registrationStartTime: '',
  registrationEndTime: '',
  capacity: 100,
  requireFeedback: false,
})
const editingId = ref<number | null>(null)
const aiDialogVisible = ref(false)
const aiGenerating = ref(false)
const aiResult = ref<ActivityCopyResponse | null>(null)
const detailDialogVisible = ref(false)
const detailActivity = ref<Activity | null>(null)
const detailRegistrations = ref<Registration[]>([])
const detailAttendances = ref<Attendance[]>([])
const detailLoading = ref(false)
const checkinToken = ref<CheckinToken | null>(null)
const tokenCountdown = ref(0)
let tokenTimer: ReturnType<typeof setInterval> | undefined
const aiForm = reactive<ActivityCopyRequest>({
  topic: '',
  activityType: '',
  targetAudience: '',
  location: '',
  activityTime: '',
  keywords: '',
  tone: '正式活泼',
})

const statusLabel: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '审核中',
  REJECTED: '已驳回',
  APPROVED: '待发布',
  PUBLISHED: '已发布',
  ONGOING: '进行中',
  ENDED: '已结束',
}

async function loadActivities() {
  loading.value = true
  try {
    activities.value = (await getMyActivities()).data.data
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '活动加载失败')
  } finally {
    loading.value = false
  }
}

async function saveDraft() {
  saving.value = true
  try {
    if (editingId.value) {
      await updateActivity(editingId.value, form)
      ElMessage.success('活动已更新')
    } else {
      await createActivity(form)
      ElMessage.success('活动草稿已保存')
    }
    resetForm()
    await loadActivities()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '保存失败')
  } finally {
    saving.value = false
  }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, { title: '', description: '', location: '', startTime: '', endTime: '', registrationStartTime: '', registrationEndTime: '', capacity: 100, requireFeedback: false, coverImage: undefined, feedbackDeadline: undefined })
}

function editActivity(activity: Activity) {
  editingId.value = activity.id
  Object.assign(form, {
    title: activity.title,
    description: activity.description,
    location: activity.location,
    startTime: activity.startTime,
    endTime: activity.endTime,
    registrationStartTime: activity.registrationStartTime,
    registrationEndTime: activity.registrationEndTime,
    capacity: activity.capacity,
    requireFeedback: activity.requireFeedback,
    coverImage: activity.coverImage,
    feedbackDeadline: activity.feedbackDeadline,
  })
}

async function removeActivity(id: number) {
  try {
    await ElMessageBox.confirm('删除后无法恢复，确定继续吗？', '删除活动', { type: 'warning' })
    await deleteActivity(id)
    ElMessage.success('活动已删除')
    await loadActivities()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.response?.data?.message ?? '删除失败')
  }
}

async function submitForReview(id: number) {
  try {
    await submitActivity(id)
    ElMessage.success('已提交审核')
    await loadActivities()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '提交失败')
  }
}

async function openActivityDetail(activity: Activity) {
  detailActivity.value = activity
  detailDialogVisible.value = true
  detailLoading.value = true
  try {
    const [registrationResponse, attendanceResponse] = await Promise.all([
      getActivityRegistrations(activity.id),
      getActivityAttendances(activity.id),
    ])
    detailRegistrations.value = registrationResponse.data.data
    detailAttendances.value = attendanceResponse.data.data
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '活动详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function refreshCheckinToken() {
  if (!detailActivity.value || detailActivity.value.status !== 'ONGOING') return
  try {
    checkinToken.value = (await issueCheckinToken(detailActivity.value.id)).data.data
    tokenCountdown.value = checkinToken.value.expiresInSeconds
    if (tokenTimer) clearInterval(tokenTimer)
    tokenTimer = setInterval(() => {
      tokenCountdown.value = Math.max(tokenCountdown.value - 1, 0)
      if (tokenCountdown.value === 0) void refreshCheckinToken()
    }, 1000)
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '签到令牌生成失败')
  }
}

function closeDetailDialog() {
  detailDialogVisible.value = false
  checkinToken.value = null
  tokenCountdown.value = 0
  if (tokenTimer) {
    clearInterval(tokenTimer)
    tokenTimer = undefined
  }
}

function openAiAssistant() {
  Object.assign(aiForm, {
    topic: form.title,
    activityType: '',
    targetAudience: '高校学生',
    location: form.location,
    activityTime: [form.startTime, form.endTime].filter(Boolean).join(' 至 '),
    keywords: '',
    tone: '正式活泼',
  })
  aiResult.value = null
  aiDialogVisible.value = true
}

async function generateCopy() {
  if (!aiForm.topic.trim()) {
    ElMessage.warning('请先填写活动主题')
    return
  }
  aiGenerating.value = true
  try {
    aiResult.value = (await generateActivityCopy(aiForm)).data.data
    ElMessage.success('文案生成完成，请确认后应用')
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? 'AI 文案生成失败，请稍后重试')
  } finally {
    aiGenerating.value = false
  }
}

function applyAiResult() {
  if (!aiResult.value) return
  form.title = aiResult.value.title
  const sections = [aiResult.value.description]
  if (aiResult.value.highlights.length) {
    sections.push(`活动亮点：\n${aiResult.value.highlights.map((item) => `• ${item}`).join('\n')}`)
  }
  if (aiResult.value.notices.length) {
    sections.push(`注意事项：\n${aiResult.value.notices.map((item) => `• ${item}`).join('\n')}`)
  }
  form.description = sections.join('\n\n')
  aiDialogVisible.value = false
  ElMessage.success('AI 文案已填入活动表单，请检查后保存')
}

onMounted(loadActivities)
onUnmounted(() => {
  if (tokenTimer) clearInterval(tokenTimer)
})
</script>

<template>
  <div class="page-card">
    <div class="page-heading"><div><h2>我的活动</h2><p>创建活动草稿并提交管理员审核。</p></div><el-button @click="loadActivities">刷新</el-button></div>
    <el-card shadow="never" class="form-card">
      <template #header>
        <div class="form-card-header">
          <span>{{ editingId ? '编辑活动草稿' : '创建活动草稿' }}</span>
          <el-button type="primary" plain @click="openAiAssistant">AI 文案助手</el-button>
        </div>
      </template>
      <el-form label-position="top">
        <div class="form-columns">
          <el-form-item label="活动名称"><el-input v-model="form.title" placeholder="例如：校园春季运动会" /></el-form-item>
          <el-form-item label="活动地点"><el-input v-model="form.location" placeholder="例如：大学生活动中心" /></el-form-item>
        </div>
        <el-form-item label="活动介绍"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <div class="form-columns">
          <el-form-item label="活动开始"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
          <el-form-item label="活动结束"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
          <el-form-item label="报名开始"><el-date-picker v-model="form.registrationStartTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
          <el-form-item label="报名结束"><el-date-picker v-model="form.registrationEndTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
        </div>
        <div class="form-columns">
          <el-form-item label="人数上限"><el-input-number v-model="form.capacity" :min="1" /></el-form-item>
          <el-form-item label="活动结束后收集反馈"><el-switch v-model="form.requireFeedback" /></el-form-item>
        </div>
        <el-button type="primary" :loading="saving" @click="saveDraft">{{ editingId ? '保存修改' : '保存草稿' }}</el-button>
        <el-button v-if="editingId" @click="resetForm">取消编辑</el-button>
      </el-form>
    </el-card>
    <el-table v-loading="loading" :data="activities" stripe>
      <el-table-column prop="title" label="活动名称" min-width="180" />
      <el-table-column prop="status" label="状态" width="110"><template #default="{ row }"><el-tag>{{ statusLabel[row.status] ?? row.status }}</el-tag></template></el-table-column>
      <el-table-column prop="capacity" label="容量" width="90" />
      <el-table-column label="操作" min-width="300"><template #default="{ row }"><el-button link type="primary" @click="openActivityDetail(row)">详情/名单</el-button><el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" link type="primary" @click="editActivity(row)">编辑</el-button><el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" link type="primary" @click="submitForReview(row.id)">提交审核</el-button><el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" link type="danger" @click="removeActivity(row.id)">删除</el-button></template></el-table-column>
    </el-table>
    <el-dialog v-model="aiDialogVisible" title="DeepSeek AI 活动文案助手" width="min(720px, 92vw)" destroy-on-close>
      <el-alert title="AI 生成内容仅作为草稿，不会自动保存或提交审核，请在应用后人工检查。" type="info" :closable="false" show-icon />
      <el-form class="ai-form" label-position="top">
        <div class="form-columns">
          <el-form-item label="活动主题" required><el-input v-model="aiForm.topic" maxlength="120" show-word-limit /></el-form-item>
          <el-form-item label="活动类型"><el-input v-model="aiForm.activityType" placeholder="讲座、比赛、志愿服务等" /></el-form-item>
          <el-form-item label="目标人群"><el-input v-model="aiForm.targetAudience" /></el-form-item>
          <el-form-item label="活动地点"><el-input v-model="aiForm.location" /></el-form-item>
          <el-form-item label="活动时间"><el-input v-model="aiForm.activityTime" /></el-form-item>
          <el-form-item label="文案风格">
            <el-select v-model="aiForm.tone">
              <el-option label="正式活泼" value="正式活泼" />
              <el-option label="青春热情" value="青春热情" />
              <el-option label="简洁专业" value="简洁专业" />
              <el-option label="温暖亲切" value="温暖亲切" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="关键词"><el-input v-model="aiForm.keywords" maxlength="200" placeholder="用逗号分隔，例如：创新、实践、团队合作" /></el-form-item>
      </el-form>
      <div class="ai-actions"><el-button type="primary" :loading="aiGenerating" @click="generateCopy">{{ aiResult ? '重新生成' : '生成文案' }}</el-button></div>
      <el-card v-if="aiResult" class="ai-result" shadow="never">
        <template #header><strong>{{ aiResult.title }}</strong></template>
        <p>{{ aiResult.description }}</p>
        <div v-if="aiResult.highlights.length"><h4>活动亮点</h4><ul><li v-for="item in aiResult.highlights" :key="item">{{ item }}</li></ul></div>
        <div v-if="aiResult.notices.length"><h4>注意事项</h4><ul><li v-for="item in aiResult.notices" :key="item">{{ item }}</li></ul></div>
        <div v-if="aiResult.tags.length" class="ai-tags"><el-tag v-for="tag in aiResult.tags" :key="tag">{{ tag }}</el-tag></div>
        <small>生成模型：{{ aiResult.model }}</small>
      </el-card>
      <template #footer>
        <el-button @click="aiDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!aiResult" @click="applyAiResult">应用到活动表单</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="detailDialogVisible" title="活动详情与报名名单" width="min(900px, 94vw)" @closed="closeDetailDialog">
      <div v-loading="detailLoading" v-if="detailActivity">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="活动名称">{{ detailActivity.title }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusLabel[detailActivity.status] ?? detailActivity.status }}</el-descriptions-item>
          <el-descriptions-item label="活动地点">{{ detailActivity.location }}</el-descriptions-item>
          <el-descriptions-item label="报名情况">{{ detailActivity.currentRegisteredCount }} / {{ detailActivity.capacity }}</el-descriptions-item>
          <el-descriptions-item label="活动时间" :span="2">{{ detailActivity.startTime.replace('T', ' ').slice(0, 16) }} 至 {{ detailActivity.endTime.replace('T', ' ').slice(0, 16) }}</el-descriptions-item>
        </el-descriptions>
        <h3>活动介绍</h3>
        <p class="detail-description">{{ detailActivity.description }}</p>
        <div v-if="detailActivity.status === 'ONGOING'" class="checkin-token-box">
          <div class="page-heading"><div><h3>签到令牌</h3><p>令牌每 60 秒自动刷新，参会者可将当前令牌粘贴到活动详情页完成签到。</p></div><el-button type="primary" @click="refreshCheckinToken">立即刷新</el-button></div>
          <el-input :model-value="checkinToken?.token ?? '点击刷新生成令牌'" readonly>
            <template #append>{{ tokenCountdown > 0 ? `${tokenCountdown}s` : '-' }}</template>
          </el-input>
        </div>
        <el-tabs>
          <el-tab-pane label="报名名单">
            <el-empty v-if="detailRegistrations.length === 0" description="暂无报名" />
            <el-table v-else :data="detailRegistrations" stripe>
              <el-table-column prop="name" label="姓名" width="120" />
              <el-table-column prop="username" label="用户名" width="150" />
              <el-table-column prop="studentId" label="学号" width="150" />
              <el-table-column prop="status" label="报名状态" width="110" />
              <el-table-column prop="checkedIn" label="签到" width="90"><template #default="{ row }">{{ row.checkedIn ? '已签到' : '未签到' }}</template></el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="签到记录">
            <el-empty v-if="detailAttendances.length === 0" description="暂无签到" />
            <el-table v-else :data="detailAttendances" stripe>
              <el-table-column prop="name" label="姓名" width="120" />
              <el-table-column prop="username" label="用户名" width="150" />
              <el-table-column prop="studentId" label="学号" width="150" />
              <el-table-column prop="checkinTime" label="签到时间" min-width="180" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
      <template #footer><el-button @click="closeDetailDialog">关闭</el-button></template>
    </el-dialog>
  </div>
</template>
