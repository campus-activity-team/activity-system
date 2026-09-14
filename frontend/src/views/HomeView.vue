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
    <p>Phase 1-4 已完成，报名与签到流程已接入。当前登录用户：{{ auth.user?.name }}（{{ auth.user?.role }}）</p>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="后端状态">{{ backendStatus }}</el-descriptions-item>
      <el-descriptions-item label="当前阶段">活动管理、报名、动态二维码签到和签到记录</el-descriptions-item>
    </el-descriptions>
    <div class="actions">
      <el-button type="primary" @click="checkBackend">重新检查后端</el-button>
    </div>
  </el-card>
</template>
