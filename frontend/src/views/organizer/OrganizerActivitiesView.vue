<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import QRCode from 'qrcode'
import { cancelActivity, createActivity, deleteActivity, endActivity, getMyActivities, startActivity, submitActivity, updateActivity, withdrawActivityReview } from '../../api/activity'
import { generateActivityCopy } from '../../api/ai'
import { getActivityFeedbackDashboard } from '../../api/feedback'
import { cancelParticipantAttendance, exportActivityRoster, getActivityAttendances, getActivityCheckinAnomalies, getActivityRegistrations, issueCheckinToken, manualCheckinParticipant } from '../../api/registration'
import type { Activity, ActivityPayload } from '../../types/activity'
import type { ActivityCopyRequest, ActivityCopyResponse } from '../../types/ai'
import type { FeedbackDashboard } from '../../types/feedback'
import type { Attendance, CheckinAnomaly, CheckinAnomalyReason, CheckinToken, Registration } from '../../types/registration'

const loading = ref(false)
const saving = ref(false)
const activities = ref<Activity[]>([])
const form = reactive<ActivityPayload>({
  title: '',
  description: '',
  location: '',
  checkinLatitude: undefined,
  checkinLongitude: undefined,
  checkinRadiusMeters: undefined,
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
const detailCheckinAnomalies = ref<CheckinAnomaly[]>([])
const detailFeedbackDashboard = ref<FeedbackDashboard | null>(null)
const detailLoading = ref(false)
const exportingRoster = ref(false)
const attendanceChangingUserId = ref<number | null>(null)
const checkinToken = ref<CheckinToken | null>(null)
const checkinQrCode = ref('')
const tokenCountdown = ref(0)
let tokenTimer: ReturnType<typeof setInterval> | undefined
let attendanceSyncTimer: ReturnType<typeof setInterval> | undefined
const attendanceSyncedAt = ref('')
const publicUrlStorageKey = 'activity-system-public-url'
const configuredPublicAppUrl = (import.meta.env.VITE_PUBLIC_APP_URL as string | undefined)?.trim() ?? ''
const currentOrigin = window.location.origin
const checkinBaseUrl = ref(
  localStorage.getItem(publicUrlStorageKey)?.trim()
    || configuredPublicAppUrl
    || (isLocalOnlyHostname(window.location.hostname) ? '' : currentOrigin),
)
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
  CANCELLED: '已取消',
}

const anomalyReasonLabel: Record<CheckinAnomalyReason, string> = {
  EXPIRED_TOKEN: '二维码过期',
  OUTSIDE_CHECKIN_TIME: '非签到时段',
  NOT_REGISTERED: '未报名',
  LOCATION_REQUIRED: '未提供定位',
  OUTSIDE_GEOFENCE: '超出签到范围',
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
  Object.assign(form, { title: '', description: '', location: '', checkinLatitude: undefined, checkinLongitude: undefined, checkinRadiusMeters: undefined, startTime: '', endTime: '', registrationStartTime: '', registrationEndTime: '', capacity: 100, requireFeedback: false, coverImage: undefined, feedbackDeadline: undefined })
}

function editActivity(activity: Activity) {
  editingId.value = activity.id
  Object.assign(form, {
    title: activity.title,
    description: activity.description,
    location: activity.location,
    checkinLatitude: activity.checkinLatitude,
    checkinLongitude: activity.checkinLongitude,
    checkinRadiusMeters: activity.checkinRadiusMeters,
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

function useCurrentLocation() {
  if (!navigator.geolocation) {
    ElMessage.warning('当前浏览器不支持定位')
    return
  }
  navigator.geolocation.getCurrentPosition(
    (position) => {
      form.checkinLatitude = Number(position.coords.latitude.toFixed(7))
      form.checkinLongitude = Number(position.coords.longitude.toFixed(7))
      if (!form.checkinRadiusMeters) form.checkinRadiusMeters = 100
      ElMessage.success('已填入当前坐标，请确认活动地点后保存')
    },
    () => ElMessage.error('无法获取当前位置，请检查定位权限；正式环境需要 HTTPS'),
    { enableHighAccuracy: true, timeout: 10_000, maximumAge: 30_000 },
  )
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

async function withdrawReviewNow(id: number) {
  try {
    await ElMessageBox.confirm('撤回后活动将恢复为草稿，可修改后重新提交审核。', '撤回审核', { type: 'warning' })
    await withdrawActivityReview(id)
    ElMessage.success('审核已撤回')
    await loadActivities()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.response?.data?.message ?? '撤回失败')
  }
}

async function cancelActivityNow(activity: Activity) {
  try {
    const result = await ElMessageBox.prompt('请输入取消原因，所有已报名用户都会收到通知。', `取消“${activity.title}”`, {
      type: 'warning',
      inputPattern: /\S+/,
      inputErrorMessage: '取消原因不能为空',
      inputPlaceholder: '例如：因场地临时调整，活动取消',
    })
    await cancelActivity(activity.id, result.value.trim())
    ElMessage.success('活动已取消并通知报名者')
    await loadActivities()
    if (detailActivity.value?.id === activity.id) {
      detailActivity.value = activities.value.find((item) => item.id === activity.id) ?? null
    }
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.response?.data?.message ?? '取消活动失败')
  }
}

async function startActivityNow(id: number) {
  try {
    await ElMessageBox.confirm('立即开启活动并进入签到状态？开启后将允许参会者签到。', '手动开启活动', { type: 'warning' })
    await startActivity(id)
    ElMessage.success('活动已开启，现在可以生成签到二维码')
    await loadActivities()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.response?.data?.message ?? '开启活动失败')
  }
}

async function endActivityNow(id: number) {
  try {
    await ElMessageBox.confirm('确定立即结束活动？结束后将停止扫码签到，并开放已配置的活动反馈。', '手动结束活动', { type: 'warning' })
    await endActivity(id)
    ElMessage.success('活动已结束')
    await loadActivities()
    if (detailActivity.value?.id === id) {
      detailActivity.value = activities.value.find((activity) => activity.id === id) ?? null
      await loadActivityDetailData(false)
    }
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.response?.data?.message ?? '结束活动失败')
  }
}

async function openActivityDetail(activity: Activity) {
  detailActivity.value = activity
  detailDialogVisible.value = true
  await loadActivityDetailData(true)
  if (attendanceSyncTimer) clearInterval(attendanceSyncTimer)
  attendanceSyncTimer = setInterval(() => { void loadActivityDetailData(false) }, 3000)
}

async function loadActivityDetailData(showLoading: boolean) {
  if (!detailActivity.value) return
  if (showLoading) detailLoading.value = true
  try {
    const [registrationResponse, attendanceResponse, anomalyResponse, feedbackResponse] = await Promise.all([
      getActivityRegistrations(detailActivity.value.id),
      getActivityAttendances(detailActivity.value.id),
      getActivityCheckinAnomalies(detailActivity.value.id),
      getActivityFeedbackDashboard(detailActivity.value.id),
    ])
    detailRegistrations.value = registrationResponse.data.data
    detailAttendances.value = attendanceResponse.data.data
    detailCheckinAnomalies.value = anomalyResponse.data.data
    detailFeedbackDashboard.value = feedbackResponse.data.data
    attendanceSyncedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } catch (error: any) {
    if (showLoading) ElMessage.error(error?.response?.data?.message ?? '活动详情加载失败')
  } finally {
    if (showLoading) detailLoading.value = false
  }
}

function anomalyLocation(anomaly: CheckinAnomaly) {
  if (anomaly.distanceMeters != null) return `距签到点约 ${anomaly.distanceMeters} 米`
  if (anomaly.latitude != null && anomaly.longitude != null) {
    return `${anomaly.latitude.toFixed(5)}, ${anomaly.longitude.toFixed(5)}`
  }
  return '未提供定位'
}

function checkinMethodLabel(method?: string) {
  if (!method) return '-'
  return method === 'MANUAL' ? '手动补签' : '二维码'
}

async function downloadRoster() {
  if (!detailActivity.value) return
  exportingRoster.value = true
  try {
    const response = await exportActivityRoster(detailActivity.value.id)
    const downloadUrl = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = `activity-${detailActivity.value.id}-roster.csv`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(downloadUrl)
    ElMessage.success('报名与签到名单已导出')
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '名单导出失败')
  } finally {
    exportingRoster.value = false
  }
}

async function manualCheckin(registration: Registration) {
  if (!detailActivity.value) return
  try {
    await ElMessageBox.confirm(`确认为 ${registration.name} 手动补签？`, '手动补签', { type: 'warning' })
    attendanceChangingUserId.value = registration.userId
    await manualCheckinParticipant(detailActivity.value.id, registration.userId)
    ElMessage.success('手动补签成功')
    await loadActivityDetailData(false)
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.response?.data?.message ?? '手动补签失败')
  } finally {
    attendanceChangingUserId.value = null
  }
}

async function cancelAttendance(registration: Registration) {
  if (!detailActivity.value) return
  try {
    await ElMessageBox.confirm(`撤销 ${registration.name} 的签到记录？`, '撤销签到', { type: 'warning' })
    attendanceChangingUserId.value = registration.userId
    await cancelParticipantAttendance(detailActivity.value.id, registration.userId)
    ElMessage.success('签到记录已撤销')
    await loadActivityDetailData(false)
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.response?.data?.message ?? '撤销签到失败')
  } finally {
    attendanceChangingUserId.value = null
  }
}

function feedbackDistributionPercentage(rating: number) {
  const dashboard = detailFeedbackDashboard.value
  if (!dashboard || dashboard.feedbackCount === 0) return 0
  return Math.round(((dashboard.overallRatingDistribution[rating] ?? 0) * 100) / dashboard.feedbackCount)
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '未设置'
}

function isLocalOnlyHostname(hostname: string) {
  return ['localhost', '127.0.0.1', '0.0.0.0', '::1', '::'].includes(hostname.toLowerCase())
}

function resolveCheckinBaseUrl() {
  const value = checkinBaseUrl.value.trim()
  if (!value) {
    ElMessage.warning('请先填写手机能够访问的系统地址')
    return null
  }
  try {
    const url = new URL(value)
    if (!['http:', 'https:'].includes(url.protocol)) {
      ElMessage.error('手机访问地址必须以 http:// 或 https:// 开头')
      return null
    }
    if (isLocalOnlyHostname(url.hostname)) {
      ElMessage.error('不能使用本机地址，请填写局域网 IP 或 HTTPS 域名')
      return null
    }
    return url.origin
  } catch {
    ElMessage.error('手机访问地址格式不正确')
    return null
  }
}

async function refreshCheckinToken() {
  if (!detailActivity.value || detailActivity.value.status !== 'ONGOING') return
  const qrBaseUrl = resolveCheckinBaseUrl()
  if (!qrBaseUrl) return
  try {
    localStorage.setItem(publicUrlStorageKey, qrBaseUrl)
    checkinBaseUrl.value = qrBaseUrl
    checkinToken.value = (await issueCheckinToken(detailActivity.value.id)).data.data
    const checkinUrl = new URL('/checkin', qrBaseUrl)
    checkinUrl.searchParams.set('token', checkinToken.value.token)
    checkinUrl.searchParams.set('activityId', String(detailActivity.value.id))
    checkinQrCode.value = await QRCode.toDataURL(checkinUrl.toString(), {
      width: 280,
      margin: 2,
      errorCorrectionLevel: 'M',
    })
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
  checkinQrCode.value = ''
  detailFeedbackDashboard.value = null
  tokenCountdown.value = 0
  if (tokenTimer) {
    clearInterval(tokenTimer)
    tokenTimer = undefined
  }
  if (attendanceSyncTimer) {
    clearInterval(attendanceSyncTimer)
    attendanceSyncTimer = undefined
  }
  attendanceSyncedAt.value = ''
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
  if (attendanceSyncTimer) clearInterval(attendanceSyncTimer)
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
        <el-divider content-position="left">签到位置（可选）</el-divider>
        <div class="form-columns">
          <el-form-item label="签到纬度"><el-input-number v-model="form.checkinLatitude" :precision="7" :step="0.000001" controls-position="right" placeholder="例如 31.2304" /></el-form-item>
          <el-form-item label="签到经度"><el-input-number v-model="form.checkinLongitude" :precision="7" :step="0.000001" controls-position="right" placeholder="例如 121.4737" /></el-form-item>
          <el-form-item label="允许半径（米）"><el-input-number v-model="form.checkinRadiusMeters" :min="20" :max="2000" :step="10" controls-position="right" placeholder="100" /></el-form-item>
        </div>
        <div class="location-actions">
          <el-button size="small" @click="useCurrentLocation">使用当前设备定位</el-button>
          <small>填写坐标后，扫码签到会要求手机定位并限制在该半径内；局域网 HTTP 通常无法获取定位。</small>
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
          <el-form-item v-if="form.requireFeedback" label="反馈截止时间">
            <el-date-picker v-model="form.feedbackDeadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="不填则长期开放" />
          </el-form-item>
        </div>
        <el-button type="primary" :loading="saving" @click="saveDraft">{{ editingId ? '保存修改' : '保存草稿' }}</el-button>
        <el-button v-if="editingId" @click="resetForm">取消编辑</el-button>
      </el-form>
    </el-card>
    <el-table v-loading="loading" :data="activities" stripe>
      <el-table-column prop="title" label="活动名称" min-width="180" />
      <el-table-column prop="status" label="状态" width="110"><template #default="{ row }"><el-tag>{{ statusLabel[row.status] ?? row.status }}</el-tag></template></el-table-column>
      <el-table-column prop="capacity" label="容量" width="90" />
      <el-table-column label="操作" min-width="480"><template #default="{ row }"><el-button link type="primary" @click="openActivityDetail(row)">详情/名单</el-button><el-button v-if="row.status === 'PUBLISHED'" link type="success" @click="startActivityNow(row.id)">手动开启</el-button><el-button v-if="row.status === 'ONGOING'" link type="warning" @click="endActivityNow(row.id)">立即结束</el-button><el-button v-if="row.status === 'PENDING_REVIEW'" link type="warning" @click="withdrawReviewNow(row.id)">撤回审核</el-button><el-button v-if="row.status === 'APPROVED' || row.status === 'PUBLISHED' || row.status === 'ONGOING'" link type="danger" @click="cancelActivityNow(row)">取消活动</el-button><el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" link type="primary" @click="editActivity(row)">编辑</el-button><el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" link type="primary" @click="submitForReview(row.id)">提交审核</el-button><el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" link type="danger" @click="removeActivity(row.id)">删除</el-button></template></el-table-column>
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
    <el-dialog v-model="detailDialogVisible" title="活动详情与现场管理" width="min(1000px, 94vw)" @closed="closeDetailDialog">
      <div v-loading="detailLoading" v-if="detailActivity">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="活动名称">{{ detailActivity.title }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusLabel[detailActivity.status] ?? detailActivity.status }}</el-descriptions-item>
          <el-descriptions-item label="活动地点">{{ detailActivity.location }}</el-descriptions-item>
          <el-descriptions-item label="报名情况">{{ detailActivity.currentRegisteredCount }} / {{ detailActivity.capacity }}</el-descriptions-item>
          <el-descriptions-item label="活动时间" :span="2">{{ detailActivity.startTime.replace('T', ' ').slice(0, 16) }} 至 {{ detailActivity.endTime.replace('T', ' ').slice(0, 16) }}</el-descriptions-item>
          <el-descriptions-item v-if="detailActivity.requireFeedback" label="反馈截止" :span="2">{{ formatTime(detailActivity.feedbackDeadline) }}</el-descriptions-item>
          <el-descriptions-item v-if="detailActivity.status === 'CANCELLED'" label="取消原因" :span="2">{{ detailActivity.cancellationReason }}</el-descriptions-item>
        </el-descriptions>
        <h3>活动介绍</h3>
        <p class="detail-description">{{ detailActivity.description }}</p>
        <div v-if="detailActivity.status === 'ONGOING'" class="checkin-token-box">
          <div class="page-heading"><div><h3>动态签到二维码</h3><p>二维码每 60 秒自动刷新，参会者扫码后会自动打开签到页面并完成签到。</p></div><el-button type="primary" @click="refreshCheckinToken">{{ checkinQrCode ? '立即刷新' : '生成二维码' }}</el-button></div>
          <div class="checkin-network-config">
            <el-input v-model="checkinBaseUrl" clearable placeholder="例如：http://192.168.1.20:5173 或 https://activity.example.edu">
              <template #prepend>手机访问地址</template>
            </el-input>
            <small>局域网演示填写电脑的局域网 IP；正式使用填写已配置 HTTPS 的域名。该设置只保存在当前浏览器。</small>
          </div>
          <div v-if="checkinQrCode" class="checkin-qr-content">
            <img :src="checkinQrCode" alt="活动动态签到二维码" class="checkin-qr-image" />
            <div class="checkin-qr-meta">
              <el-tag type="warning">{{ tokenCountdown }} 秒后刷新</el-tag>
              <p>二维码访问：{{ checkinBaseUrl }}</p>
              <p>请先用手机浏览器打开该地址确认网络连通，再扫描二维码。</p>
            </div>
          </div>
          <el-alert v-if="!checkinBaseUrl.trim()" title="需要配置手机访问地址" description="系统不会生成 localhost 二维码，请填写局域网 IP 或正式 HTTPS 域名。" type="warning" show-icon :closable="false" />
          <el-empty v-if="!checkinQrCode" description="点击生成本场签到二维码" :image-size="80" />
        </div>
        <el-tabs>
          <el-tab-pane :label="`报名名单（${detailRegistrations.length}）`">
            <div class="attendance-sync-bar">
              <small>导出文件包含报名状态、签到状态、时间和签到方式。</small>
              <el-button type="primary" plain :loading="exportingRoster" @click="downloadRoster">导出 CSV 名单</el-button>
            </div>
            <el-empty v-if="detailRegistrations.length === 0" description="暂无报名" />
            <el-table v-else :data="detailRegistrations" stripe>
              <el-table-column prop="name" label="姓名" width="120" />
              <el-table-column prop="username" label="用户名" width="150" />
              <el-table-column prop="studentId" label="学号" width="150" />
              <el-table-column prop="status" label="报名状态" width="110" />
              <el-table-column prop="checkedIn" label="签到" width="90"><template #default="{ row }">{{ row.checkedIn ? '已签到' : '未签到' }}</template></el-table-column>
              <el-table-column label="签到方式" width="100"><template #default="{ row }">{{ checkinMethodLabel(row.checkinMethod) }}</template></el-table-column>
              <el-table-column v-if="detailActivity.status === 'ONGOING' || detailActivity.status === 'ENDED'" label="现场操作" width="120">
                <template #default="{ row }">
                  <el-button
                    v-if="row.status === 'REGISTERED' && !row.checkedIn"
                    link
                    type="success"
                    :loading="attendanceChangingUserId === row.userId"
                    @click="manualCheckin(row)"
                  >手动补签</el-button>
                  <el-button
                    v-else-if="row.checkedIn"
                    link
                    type="danger"
                    :loading="attendanceChangingUserId === row.userId"
                    @click="cancelAttendance(row)"
                  >撤销签到</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`签到记录（${detailAttendances.length}）`">
            <div class="attendance-sync-bar">
              <small>每 3 秒自动同步{{ attendanceSyncedAt ? ` · 最近同步 ${attendanceSyncedAt}` : '' }}</small>
              <el-button link type="primary" @click="loadActivityDetailData(false)">立即刷新</el-button>
            </div>
            <el-empty v-if="detailAttendances.length === 0" description="暂无签到" />
            <el-table v-else :data="detailAttendances" stripe>
              <el-table-column prop="name" label="姓名" width="120" />
              <el-table-column prop="username" label="用户名" width="150" />
              <el-table-column prop="studentId" label="学号" width="150" />
              <el-table-column prop="checkinTime" label="签到时间" min-width="180" />
              <el-table-column label="签到方式" width="110"><template #default="{ row }">{{ checkinMethodLabel(row.checkinMethod) }}</template></el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`异常尝试（${detailCheckinAnomalies.length}）`">
            <div class="attendance-sync-bar">
              <small>记录过期二维码、未报名、非签到时段和定位异常；每 3 秒自动同步。</small>
              <el-button link type="primary" @click="loadActivityDetailData(false)">立即刷新</el-button>
            </div>
            <el-empty v-if="detailCheckinAnomalies.length === 0" description="暂无异常签到尝试" />
            <el-table v-else :data="detailCheckinAnomalies" stripe>
              <el-table-column prop="name" label="姓名" width="110" />
              <el-table-column prop="studentId" label="学号" width="140" />
              <el-table-column label="异常类型" width="130">
                <template #default="{ row }"><el-tag type="danger">{{ anomalyReasonLabel[row.reason as CheckinAnomalyReason] }}</el-tag></template>
              </el-table-column>
              <el-table-column label="定位信息" min-width="170">
                <template #default="{ row }">{{ anomalyLocation(row) }}</template>
              </el-table-column>
              <el-table-column prop="createdAt" label="尝试时间" min-width="180" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane v-if="detailActivity.requireFeedback" :label="`反馈统计（${detailFeedbackDashboard?.feedbackCount ?? 0}）`">
            <template v-if="detailFeedbackDashboard">
              <div class="feedback-metrics">
                <div><strong>{{ detailFeedbackDashboard.registeredCount }}</strong><span>报名人数</span></div>
                <div><strong>{{ detailFeedbackDashboard.attendedCount }}</strong><span>签到人数</span></div>
                <div><strong>{{ detailFeedbackDashboard.feedbackCount }}</strong><span>反馈份数</span></div>
                <div><strong>{{ detailFeedbackDashboard.responseRate }}%</strong><span>反馈率</span></div>
              </div>
              <div class="feedback-rating-grid">
                <el-card shadow="never"><span>总体评分</span><strong>{{ detailFeedbackDashboard.averageOverallRating.toFixed(1) }}</strong><el-rate :model-value="detailFeedbackDashboard.averageOverallRating" disabled /></el-card>
                <el-card shadow="never"><span>内容评分</span><strong>{{ detailFeedbackDashboard.averageContentRating.toFixed(1) }}</strong><el-rate :model-value="detailFeedbackDashboard.averageContentRating" disabled /></el-card>
                <el-card shadow="never"><span>服务评分</span><strong>{{ detailFeedbackDashboard.averageServiceRating.toFixed(1) }}</strong><el-rate :model-value="detailFeedbackDashboard.averageServiceRating" disabled /></el-card>
              </div>
              <div class="feedback-distribution">
                <div v-for="rating in [5, 4, 3, 2, 1]" :key="rating">
                  <span>{{ rating }} 星</span>
                  <el-progress :percentage="feedbackDistributionPercentage(rating)" :stroke-width="10" />
                  <small>{{ detailFeedbackDashboard.overallRatingDistribution[rating] ?? 0 }} 份</small>
                </div>
              </div>
              <el-alert title="反馈内容默认匿名展示，不向活动发起者提供评价者身份。" type="info" show-icon :closable="false" />
              <el-empty v-if="detailFeedbackDashboard.feedbacks.length === 0" description="暂无参与者反馈" />
              <el-table v-else :data="detailFeedbackDashboard.feedbacks" stripe>
                <el-table-column prop="overallRating" label="总体" width="70" />
                <el-table-column prop="contentRating" label="内容" width="70" />
                <el-table-column prop="serviceRating" label="服务" width="70" />
                <el-table-column prop="comment" label="匿名意见" min-width="260"><template #default="{ row }">{{ row.comment || '未填写' }}</template></el-table-column>
                <el-table-column label="提交时间" width="165"><template #default="{ row }">{{ formatTime(row.updatedAt || row.createdAt) }}</template></el-table-column>
              </el-table>
            </template>
          </el-tab-pane>
        </el-tabs>
      </div>
      <template #footer><el-button @click="closeDetailDialog">关闭</el-button></template>
    </el-dialog>
  </div>
</template>
