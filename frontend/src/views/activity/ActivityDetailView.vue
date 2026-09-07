<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActivity } from '../../api/activity'
import type { Activity } from '../../types/activity'

const route = useRoute()
const loading = ref(false)
const activity = ref<Activity | null>(null)

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

async function loadActivity() {
  loading.value = true
  try {
    activity.value = (await getActivity(Number(route.params.id))).data.data
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '活动加载失败')
  } finally {
    loading.value = false
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
      <el-button type="primary" disabled>报名功能将在 Phase 4 开放</el-button>
    </template>
  </el-card>
</template>
