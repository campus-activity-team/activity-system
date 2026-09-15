<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getHealth } from '../api/http'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const backendStatus = ref('检查中')

async function checkBackend() {
  backendStatus.value = '检查中'
  try {
    const response = await getHealth()
    backendStatus.value = response.data.data.status === 'UP' ? '在线' : '异常'
  } catch {
    backendStatus.value = '未连接'
    ElMessage.info('后端尚未启动，请先按照 README 启动 MySQL、Redis 和 Spring Boot')
  }
}

onMounted(checkBackend)
</script>

<template>
  <el-card class="welcome-card" shadow="never">
    <template #header>
      <div class="card-title">高校活动报名与签到系统</div>
    </template>
    <p>活动报名、扫码签到、身份审批和活动反馈流程已接入。当前登录用户：{{ auth.user?.name }}（{{ auth.user?.role }}）</p>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="后端状态">{{ backendStatus }}</el-descriptions-item>
      <el-descriptions-item label="当前阶段">活动管理、报名签到、身份权限审批、反馈评分与统计</el-descriptions-item>
    </el-descriptions>
    <div class="actions">
      <el-button type="primary" @click="checkBackend">重新检查后端</el-button>
      <el-button v-if="auth.user?.role === 'USER'" @click="$router.push('/organizer-application')">申请发起活动</el-button>
      <el-button v-if="auth.user?.role === 'ADMIN'" @click="$router.push('/admin/users')">管理用户权限</el-button>
    </div>
  </el-card>
</template>
