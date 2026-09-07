<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getHealth } from './api/http'

const backendStatus = ref('检查中')

async function checkBackend() {
  backendStatus.value = '检查中'
  try {
    const response = await getHealth()
    backendStatus.value = response.data.data.status === 'UP' ? '在线' : '异常'
  } catch {
    backendStatus.value = '未连接'
    ElMessage.info('后端尚未启动，可先查看 README 中的启动方式')
  }
}

checkBackend()
</script>

<template>
  <el-container class="app-shell">
    <el-header class="app-header">
      <div class="brand">
        <span class="brand-mark">A</span>
        <span>活动报名与签到系统</span>
      </div>
      <el-tag :type="backendStatus === '在线' ? 'success' : 'info'">后端：{{ backendStatus }}</el-tag>
    </el-header>
    <el-main class="app-main">
      <el-card class="welcome-card" shadow="never">
        <template #header>
          <div class="card-title">Phase 1 项目基础已就绪</div>
        </template>
        <p>前后端分离骨架、统一 API 返回结构、异常处理、MySQL/Redis 配置和 Docker 编排已完成。</p>
        <div class="actions">
          <el-button type="primary" @click="checkBackend">重新检查后端</el-button>
        </div>
      </el-card>
    </el-main>
  </el-container>
</template>
