<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyOrganizerApplication, submitOrganizerApplication } from '../../api/userManagement'
import { useAuthStore } from '../../stores/auth'
import type { OrganizerApplication } from '../../types/organizer'

const auth = useAuthStore()
const loading = ref(false)
const submitting = ref(false)
const reason = ref('')
const application = ref<OrganizerApplication | null>(null)

const statusLabel = {
  PENDING: '审核中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
}

const statusType = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
} as const

async function loadApplication() {
  loading.value = true
  try {
    await auth.loadCurrentUser()
    application.value = (await getMyOrganizerApplication()).data.data
    if (application.value?.status === 'REJECTED') reason.value = application.value.reason
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '申请状态加载失败')
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!reason.value.trim()) {
    ElMessage.warning('请填写申请理由')
    return
  }
  submitting.value = true
  try {
    application.value = (await submitOrganizerApplication(reason.value)).data.data
    ElMessage.success('申请已提交，请等待管理员审核')
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '申请提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadApplication)
</script>

<template>
  <el-card class="welcome-card" shadow="never" v-loading="loading">
    <template #header>
      <div class="form-card-header">
        <div><strong>申请成为活动发起者</strong><p class="muted-copy">普通学生通过审核后可以创建活动，但活动仍需管理员审核发布。</p></div>
        <el-button @click="loadApplication">刷新状态</el-button>
      </div>
    </template>

    <el-alert v-if="auth.user?.role === 'ORGANIZER'" title="你的账号已经是活动发起者" description="现在可以进入“我的活动”创建并提交活动。" type="success" show-icon :closable="false" />
    <el-alert v-else-if="auth.user?.role === 'ADMIN'" title="管理员已经具备全部活动管理权限" type="success" show-icon :closable="false" />

    <template v-else>
      <el-descriptions v-if="application" :column="1" border class="application-status">
        <el-descriptions-item label="当前状态"><el-tag :type="statusType[application.status]">{{ statusLabel[application.status] }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="申请理由">{{ application.reason }}</el-descriptions-item>
        <el-descriptions-item v-if="application.reviewComment" label="审核意见">{{ application.reviewComment }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ application.updatedAt.replace('T', ' ').slice(0, 16) }}</el-descriptions-item>
      </el-descriptions>

      <el-alert v-if="application?.status === 'PENDING'" title="申请正在审核中" description="管理员处理后刷新本页面即可获得最新状态。" type="warning" show-icon :closable="false" />
      <el-alert v-else-if="application?.status === 'APPROVED'" title="申请已通过" description="刷新页面后即可进入“我的活动”。" type="success" show-icon :closable="false" />
      <el-form v-else label-position="top" class="application-form">
        <el-form-item label="申请理由">
          <el-input v-model="reason" type="textarea" :rows="5" maxlength="1000" show-word-limit placeholder="请说明计划发起的活动类型、所属社团或组织及联系方式" />
        </el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">{{ application?.status === 'REJECTED' ? '重新提交申请' : '提交申请' }}</el-button>
      </el-form>
    </template>
  </el-card>
</template>
