<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { checkin } from '../../api/registration'
import { useAuthStore } from '../../stores/auth'
import type { CheckinResult } from '../../types/registration'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(true)
const succeeded = ref(false)
const message = ref('正在验证签到二维码…')
const result = ref<CheckinResult | null>(null)

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

async function completeCheckin() {
  const token = typeof route.query.token === 'string' ? route.query.token.trim() : ''
  if (!token) {
    message.value = '二维码缺少签到信息，请让组织者刷新二维码后重试。'
    loading.value = false
    return
  }
  if (!auth.isAuthenticated) {
    await router.replace({ name: 'login', query: { redirect: route.fullPath } })
    return
  }

  try {
    const response = await checkin(token)
    result.value = response.data.data
    succeeded.value = true
    message.value = '签到记录已保存，并同步给活动管理者和发起者。'
    ElMessage.success('已完成签到')
  } catch (error: any) {
    message.value = error?.response?.data?.message ?? '签到失败，请让组织者刷新二维码后重试。'
  } finally {
    loading.value = false
  }
}

onMounted(completeCheckin)
</script>

<template>
  <el-card class="checkin-card" shadow="never">
    <el-result v-if="loading" icon="info" title="正在签到" :sub-title="message" />
    <template v-else-if="succeeded && result">
      <el-result icon="success" title="已完成签到" :sub-title="message" />
      <el-descriptions :column="1" border>
        <el-descriptions-item label="活动名称">{{ result.activityTitle }}</el-descriptions-item>
        <el-descriptions-item label="签到人员">{{ result.name }}</el-descriptions-item>
        <el-descriptions-item label="签到时间">{{ formatTime(result.checkinTime) }}</el-descriptions-item>
        <el-descriptions-item label="签到方式">动态二维码</el-descriptions-item>
      </el-descriptions>
      <div class="actions checkin-actions">
        <el-button type="primary" @click="router.push({ name: 'activity-detail', params: { id: result.activityId } })">查看活动</el-button>
        <el-button @click="router.push({ name: 'my-registrations' })">查看我的报名</el-button>
      </div>
    </template>
    <el-result v-else icon="error" title="签到未完成" :sub-title="message">
      <template #extra><el-button type="primary" @click="router.push({ name: 'activities' })">返回活动列表</el-button></template>
    </el-result>
  </el-card>
</template>
