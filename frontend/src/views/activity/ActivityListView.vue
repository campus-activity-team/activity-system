<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getActivities } from '../../api/activity'
import type { Activity } from '../../types/activity'
import { formatActivityTime, getActivityDisplay } from '../../utils/activity'

const keyword = ref('')
const loading = ref(false)
const activities = ref<Activity[]>([])
const router = useRouter()
const now = ref(new Date())
let clockTimer: ReturnType<typeof setInterval> | undefined

async function loadActivities() {
  loading.value = true
  try {
    const response = await getActivities({ keyword: keyword.value || undefined })
    activities.value = response.data.data
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '活动加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadActivities()
  clockTimer = setInterval(() => { now.value = new Date() }, 30_000)
})
onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<template>
  <div class="page-card">
    <div class="page-heading">
      <div>
        <h2>活动列表</h2>
        <p>查看已经发布的校园活动。</p>
      </div>
      <el-button type="primary" :loading="loading" @click="loadActivities">刷新</el-button>
    </div>
    <el-input v-model="keyword" clearable placeholder="搜索活动名称或地点" @keyup.enter="loadActivities">
      <template #append><el-button @click="loadActivities">搜索</el-button></template>
    </el-input>
    <el-empty v-if="!loading && activities.length === 0" description="暂无公开活动" />
    <div v-else v-loading="loading" class="activity-grid">
      <el-card v-for="activity in activities" :key="activity.id" class="activity-card" shadow="hover" @click="router.push({ name: 'activity-detail', params: { id: activity.id } })">
        <div class="activity-cover">{{ activity.title.slice(0, 1) }}</div>
        <div class="activity-content">
          <div class="activity-title-row">
            <h3>{{ activity.title }}</h3>
            <el-tag :type="getActivityDisplay(activity, now).type">{{ getActivityDisplay(activity, now).label }}</el-tag>
          </div>
          <p>{{ activity.description }}</p>
          <small>{{ formatActivityTime(activity.startTime) }} · {{ activity.location }}</small>
          <small>报名：{{ formatActivityTime(activity.registrationStartTime) }} 至 {{ formatActivityTime(activity.registrationEndTime) }}</small>
          <small>剩余名额：{{ activity.remainingCapacity }} / {{ activity.capacity }}</small>
        </div>
      </el-card>
    </div>
  </div>
</template>
