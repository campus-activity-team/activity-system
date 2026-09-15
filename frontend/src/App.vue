<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from './stores/auth'
import { useNotificationStore } from './stores/notification'
import CheckinScanner from './components/CheckinScanner.vue'

const router = useRouter()
const auth = useAuthStore()
const notifications = useNotificationStore()
const scannerVisible = ref(false)
let notificationTimer: ReturnType<typeof setInterval> | undefined

watch(() => auth.isAuthenticated, (authenticated) => {
  if (authenticated) void notifications.refreshUnreadCount()
  else notifications.reset()
}, { immediate: true })

onMounted(() => {
  void auth.loadCurrentUser()
  notificationTimer = setInterval(() => {
    if (auth.isAuthenticated) void notifications.refreshUnreadCount()
  }, 5_000)
})

onUnmounted(() => {
  if (notificationTimer) clearInterval(notificationTimer)
})

async function logout() {
  await auth.logout()
  notifications.reset()
  ElMessage.success('已退出登录')
  await router.push({ name: 'login' })
}
</script>

<template>
  <el-container class="app-shell">
    <el-header class="app-header">
      <router-link class="brand" to="/">
        <span class="brand-mark">A</span>
        <span>活动报名与签到系统</span>
      </router-link>
      <nav class="main-nav">
        <router-link to="/activities">活动</router-link>
        <router-link v-if="auth.isAuthenticated" to="/my-registrations">我的报名</router-link>
        <router-link v-if="auth.user?.role === 'USER'" to="/organizer-application">申请发起活动</router-link>
        <router-link v-if="auth.user?.role === 'ORGANIZER' || auth.user?.role === 'ADMIN'" to="/organizer/activities">我的活动</router-link>
        <router-link v-if="auth.user?.role === 'ADMIN'" to="/admin/review">审核管理</router-link>
        <router-link v-if="auth.user?.role === 'ADMIN'" to="/admin/users">用户权限</router-link>
        <router-link v-if="auth.user?.role === 'ADMIN'" to="/admin/operation-logs">操作日志</router-link>
      </nav>
      <div class="header-actions">
        <template v-if="auth.isAuthenticated">
          <span class="user-greeting">{{ auth.user?.name }}</span>
          <el-badge :value="notifications.unreadCount" :hidden="notifications.unreadCount === 0" :max="99" class="notification-badge"><el-button link type="info" @click="router.push('/notifications')">通知</el-button></el-badge>
          <el-button link type="info" @click="scannerVisible = true">扫码签到</el-button>
          <el-button link type="info" @click="logout">退出</el-button>
        </template>
        <template v-else>
          <el-button link type="info" @click="router.push('/login')">登录</el-button>
          <el-button link type="info" @click="router.push('/register')">注册</el-button>
        </template>
      </div>
    </el-header>
    <el-main class="app-main">
      <router-view />
    </el-main>
    <CheckinScanner v-model="scannerVisible" />
  </el-container>
</template>
