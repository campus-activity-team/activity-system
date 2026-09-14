<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { checkin } from '../../api/registration'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(true)
const succeeded = ref(false)
const message = ref('正在验证签到二维码…')

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
    const result = response.data.data
    succeeded.value = true
    message.value = '签到成功，正在返回活动详情…'
    ElMessage.success('签到成功')
    await router.replace({ name: 'activity-detail', params: { id: result.activityId } })
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
    <el-result v-else-if="succeeded" icon="success" title="签到成功" :sub-title="message" />
    <el-result v-else icon="error" title="签到未完成" :sub-title="message">
      <template #extra><el-button type="primary" @click="router.push({ name: 'activities' })">返回活动列表</el-button></template>
    </el-result>
  </el-card>
</template>
