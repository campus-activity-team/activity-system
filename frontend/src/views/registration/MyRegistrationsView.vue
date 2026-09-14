<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getMyRegistrations } from '../../api/registration'
import type { Registration } from '../../types/registration'

const router = useRouter()
const loading = ref(false)
const registrations = ref<Registration[]>([])

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

onMounted(loadRegistrations)
</script>

<template>
  <div class="page-card">
    <div class="page-heading">
      <div><h2>我的报名</h2><p>查看报名状态和签到记录。</p></div>
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
      <el-table-column prop="checkedIn" label="签到状态" width="110">
        <template #default="{ row }"><el-tag :type="row.checkedIn ? 'success' : 'info'">{{ row.checkedIn ? '已签到' : '未签到' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="110">
        <template #default="{ row }"><el-button link type="primary" @click="router.push({ name: 'activity-detail', params: { id: row.activityId } })">查看活动</el-button></template>
      </el-table-column>
    </el-table>
  </div>
</template>
