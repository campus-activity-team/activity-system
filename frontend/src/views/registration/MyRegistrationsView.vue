<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getMyFeedbackStatus, submitFeedback } from '../../api/feedback'
import { getMyRegistrations } from '../../api/registration'
import type { FeedbackPayload, FeedbackStatus } from '../../types/feedback'
import type { Registration } from '../../types/registration'

const router = useRouter()
const loading = ref(false)
const registrations = ref<Registration[]>([])
const feedbackDialogVisible = ref(false)
const feedbackLoading = ref(false)
const feedbackSubmitting = ref(false)
const feedbackStatus = ref<FeedbackStatus | null>(null)
const feedbackForm = reactive<FeedbackPayload>({
  overallRating: 5,
  contentRating: 5,
  serviceRating: 5,
  comment: '',
})

const activityStatusLabel: Record<string, string> = {
  PUBLISHED: '已发布',
  ONGOING: '进行中',
  ENDED: '已结束',
  CANCELLED: '已取消',
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

async function loadRegistrations() {
  loading.value = true
  try {
    registrations.value = (await getMyRegistrations()).data.data
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '报名记录加载失败')
  } finally {
    loading.value = false
  }
}

async function openFeedback(registration: Registration) {
  feedbackDialogVisible.value = true
  feedbackLoading.value = true
  feedbackStatus.value = null
  Object.assign(feedbackForm, { overallRating: 5, contentRating: 5, serviceRating: 5, comment: '' })
  try {
    feedbackStatus.value = (await getMyFeedbackStatus(registration.activityId)).data.data
    const existing = feedbackStatus.value.feedback
    if (existing) {
      Object.assign(feedbackForm, {
        overallRating: existing.overallRating,
        contentRating: existing.contentRating,
        serviceRating: existing.serviceRating,
        comment: existing.comment ?? '',
      })
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '反馈信息加载失败')
    feedbackDialogVisible.value = false
  } finally {
    feedbackLoading.value = false
  }
}

async function saveFeedback() {
  if (!feedbackStatus.value?.canSubmit) return
  feedbackSubmitting.value = true
  try {
    await submitFeedback(feedbackStatus.value.activityId, feedbackForm)
    ElMessage.success(feedbackStatus.value.feedback ? '反馈已更新' : '反馈已提交')
    feedbackStatus.value = (await getMyFeedbackStatus(feedbackStatus.value.activityId)).data.data
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '反馈提交失败')
  } finally {
    feedbackSubmitting.value = false
  }
}

onMounted(loadRegistrations)
</script>

<template>
  <div class="page-card">
    <div class="page-heading">
      <div><h2>我的报名</h2><p>查看报名、签到状态，并在活动结束后提交反馈。</p></div>
      <el-button :loading="loading" @click="loadRegistrations">刷新</el-button>
    </div>
    <el-empty v-if="!loading && registrations.length === 0" description="暂无报名记录" />
    <el-table v-else v-loading="loading" :data="registrations" stripe>
      <el-table-column prop="activityTitle" label="活动名称" min-width="240" />
      <el-table-column prop="registeredAt" label="报名时间" width="170">
        <template #default="{ row }">{{ formatTime(row.registeredAt) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="报名状态" width="110">
        <template #default="{ row }"><el-tag :type="row.status === 'REGISTERED' ? 'success' : 'info'">{{ row.status === 'REGISTERED' ? '已报名' : '已取消' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="活动状态" width="110">
        <template #default="{ row }"><el-tag :type="row.activityStatus === 'CANCELLED' ? 'danger' : 'info'">{{ activityStatusLabel[row.activityStatus] ?? row.activityStatus }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="checkedIn" label="签到状态" width="110">
        <template #default="{ row }"><el-tag :type="row.checkedIn ? 'success' : 'info'">{{ row.checkedIn ? '已签到' : '未签到' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="活动反馈" width="150">
        <template #default="{ row }">
          <span v-if="row.activityStatus === 'CANCELLED'">活动已取消</span>
          <span v-else-if="!row.requireFeedback">-</span>
          <span v-else-if="row.activityStatus !== 'ENDED'">活动结束后开放</span>
          <span v-else-if="!row.checkedIn || row.status !== 'REGISTERED'">未签到不可反馈</span>
          <el-button
            v-else
            link
            type="success"
            @click="openFeedback(row)"
          >活动反馈</el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="180">
        <template #default="{ row }"><el-popover v-if="row.activityStatus === 'CANCELLED'" placement="top" width="300" trigger="click"><template #reference><el-button link type="danger">查看取消原因</el-button></template>{{ row.activityCancellationReason ?? '未填写取消原因' }}</el-popover><el-button v-else link type="primary" @click="router.push({ name: 'activity-detail', params: { id: row.activityId } })">查看活动</el-button></template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="feedbackDialogVisible" title="活动反馈" width="min(560px, 92vw)" destroy-on-close>
      <div v-loading="feedbackLoading">
        <template v-if="feedbackStatus">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="活动名称">{{ feedbackStatus.activityTitle }}</el-descriptions-item>
            <el-descriptions-item label="反馈截止">{{ formatTime(feedbackStatus.feedbackDeadline) }}</el-descriptions-item>
          </el-descriptions>
          <el-alert
            v-if="!feedbackStatus.canSubmit"
            class="feedback-alert"
            :title="feedbackStatus.unavailableReason ?? '当前不能提交反馈'"
            :description="feedbackStatus.feedback ? '已提交内容仍可查看，但当前不能再修改。' : undefined"
            type="warning"
            show-icon
            :closable="false"
          />
          <el-form class="feedback-form" label-position="top" :disabled="!feedbackStatus.canSubmit">
            <el-form-item label="总体评价"><el-rate v-model="feedbackForm.overallRating" show-score /></el-form-item>
            <el-form-item label="活动内容"><el-rate v-model="feedbackForm.contentRating" show-score /></el-form-item>
            <el-form-item label="组织服务"><el-rate v-model="feedbackForm.serviceRating" show-score /></el-form-item>
            <el-form-item label="意见建议">
              <el-input v-model="feedbackForm.comment" type="textarea" :rows="4" maxlength="2000" show-word-limit placeholder="选填，你的意见将以匿名方式提供给活动发起者" />
            </el-form-item>
          </el-form>
        </template>
      </div>
      <template #footer>
        <el-button @click="feedbackDialogVisible = false">关闭</el-button>
        <el-button v-if="feedbackStatus?.canSubmit" type="primary" :loading="feedbackSubmitting" @click="saveFeedback">
          {{ feedbackStatus.feedback ? '更新反馈' : '提交反馈' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
