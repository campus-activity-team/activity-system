<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { getActivity } from '../../api/activity'
import { cancelRegistration, checkin, getMyActivityRegistration, registerActivity } from '../../api/registration'
import type { Activity } from '../../types/activity'
import type { Registration } from '../../types/registration'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const registrationLoading = ref(false)
const checkinLoading = ref(false)
const activity = ref<Activity | null>(null)
const registration = ref<Registration | null>(null)
const checkinToken = ref('')

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

async function loadActivity() {
  loading.value = true
  try {
    activity.value = (await getActivity(Number(route.params.id))).data.data
    if (auth.isAuthenticated) await loadRegistration()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '活动加载失败')
  } finally {
    loading.value = false
  }
}

async function loadRegistration() {
  if (!activity.value || !auth.isAuthenticated) return
  try {
    registration.value = (await getMyActivityRegistration(activity.value.id)).data.data
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '报名状态加载失败')
  }
}

async function register() {
  if (!activity.value) return
  if (!auth.isAuthenticated) {
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  registrationLoading.value = true
  try {
    registration.value = (await registerActivity(activity.value.id)).data.data
    ElMessage.success('报名成功')
    await loadActivity()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '报名失败')
  } finally {
    registrationLoading.value = false
  }
}

async function cancel() {
  if (!activity.value) return
  registrationLoading.value = true
  try {
    await cancelRegistration(activity.value.id)
    registration.value = null
    ElMessage.success('已取消报名')
    await loadActivity()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '取消报名失败')
  } finally {
    registrationLoading.value = false
  }
}

async function doCheckin() {
  if (!checkinToken.value.trim()) {
    ElMessage.warning('请输入签到令牌')
    return
  }
  checkinLoading.value = true
  try {
    await checkin(checkinToken.value.trim())
    ElMessage.success('签到成功')
    checkinToken.value = ''
    await loadRegistration()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '签到失败')
  } finally {
    checkinLoading.value = false
  }
}

onMounted(loadActivity)
</script>

<template>
  <el-card v-loading="loading" class="detail-card" shadow="never">
    <el-empty v-if="!loading && !activity" description="活动不存在或尚未发布" />
    <template v-else-if="activity">
      <div class="detail-header">
        <div class="activity-cover detail-cover">{{ activity.title.slice(0, 1) }}</div>
        <div><h2>{{ activity.title }}</h2><p>{{ activity.location }}</p></div>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="活动时间">{{ formatTime(activity.startTime) }} 至 {{ formatTime(activity.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="报名时间">{{ formatTime(activity.registrationStartTime) }} 至 {{ formatTime(activity.registrationEndTime) }}</el-descriptions-item>
        <el-descriptions-item label="报名情况">{{ activity.currentRegisteredCount }} / {{ activity.capacity }}</el-descriptions-item>
        <el-descriptions-item label="剩余名额">{{ activity.remainingCapacity }}</el-descriptions-item>
      </el-descriptions>
      <h3>活动介绍</h3>
      <p class="detail-description">{{ activity.description }}</p>
      <div class="detail-actions">
        <template v-if="registration?.status === 'REGISTERED'">
          <el-tag type="success">已报名{{ registration.checkedIn ? ' · 已签到' : '' }}</el-tag>
          <el-button :loading="registrationLoading" @click="cancel">取消报名</el-button>
        </template>
        <el-button v-else type="primary" :loading="registrationLoading" @click="register">{{ auth.isAuthenticated ? '立即报名' : '登录后报名' }}</el-button>
        <el-button v-if="auth.isAuthenticated" @click="router.push({ name: 'my-registrations' })">我的报名</el-button>
      </div>
      <div v-if="activity.status === 'ONGOING' && auth.isAuthenticated" class="checkin-panel">
        <h3>活动签到</h3>
        <p>请向现场组织者获取当前 60 秒有效的签到令牌。</p>
        <el-input v-model="checkinToken" placeholder="粘贴签到令牌" clearable>
          <template #append><el-button :loading="checkinLoading" @click="doCheckin">签到</el-button></template>
        </el-input>
      </div>
    </template>
  </el-card>
</template>
