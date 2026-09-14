<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { getActivity } from '../../api/activity'
import { cancelRegistration, getMyActivityRegistration, registerActivity } from '../../api/registration'
import type { Activity } from '../../types/activity'
import type { Registration } from '../../types/registration'
import { formatActivityTime, getActivityDisplay } from '../../utils/activity'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const registrationLoading = ref(false)
const activity = ref<Activity | null>(null)
const registration = ref<Registration | null>(null)
const now = ref(new Date())
let clockTimer: ReturnType<typeof setInterval> | undefined

const activityDisplay = computed(() => activity.value ? getActivityDisplay(activity.value, now.value) : null)

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

onMounted(() => {
  loadActivity()
  clockTimer = setInterval(() => { now.value = new Date() }, 30_000)
})
onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
})
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
        <el-descriptions-item label="活动状态"><el-tag :type="activityDisplay?.type">{{ activityDisplay?.label }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="活动时间">{{ formatActivityTime(activity.startTime) }} 至 {{ formatActivityTime(activity.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="报名时间">{{ formatActivityTime(activity.registrationStartTime) }} 至 {{ formatActivityTime(activity.registrationEndTime) }}</el-descriptions-item>
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
        <el-button v-else type="primary" :disabled="!activityDisplay?.canRegister" :loading="registrationLoading" @click="register">{{ activityDisplay?.label === '报名未开始' ? '报名未开始' : activityDisplay?.label === '报名已截止' ? '报名已截止' : auth.isAuthenticated ? '立即报名' : '登录后报名' }}</el-button>
        <el-button v-if="auth.isAuthenticated" @click="router.push({ name: 'my-registrations' })">我的报名</el-button>
      </div>
      <div v-if="activityDisplay?.label === '进行中' && auth.isAuthenticated" class="checkin-panel">
        <h3>活动签到</h3>
        <p>请使用手机扫描现场组织者展示的动态二维码，系统会自动验证报名状态并完成签到。</p>
      </div>
    </template>
  </el-card>
</template>
