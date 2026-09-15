<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getNotifications, markAllNotificationsRead, markNotificationRead } from '../../api/notification'
import { useAuthStore } from '../../stores/auth'
import { useNotificationStore } from '../../stores/notification'
import type { NotificationItem } from '../../types/notification'

const router = useRouter()
const auth = useAuthStore()
const notificationStore = useNotificationStore()
const loading = ref(false)
const notifications = ref<NotificationItem[]>([])

const typeLabel: Record<string, string> = {
  REGISTRATION_SUCCESS: '报名',
  ACTIVITY_START_REMINDER: '提醒',
  ACTIVITY_APPROVED: '审核',
  ACTIVITY_REJECTED: '审核',
  ACTIVITY_PUBLISHED: '发布',
  ACTIVITY_UNPUBLISHED: '下架',
  ACTIVITY_CANCELLED: '取消',
  ACTIVITY_CANCELLED_BY_ADMIN: '取消',
  ACTIVITY_ANNOUNCEMENT: '公告',
  ACTIVITY_FEEDBACK_SUBMITTED: '反馈',
  ORGANIZER_APPLICATION_APPROVED: '权限',
  ORGANIZER_APPLICATION_REJECTED: '权限',
}

async function loadNotifications() {
  loading.value = true
  try {
    notifications.value = (await getNotifications()).data.data
    await notificationStore.refreshUnreadCount()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '通知加载失败')
  } finally {
    loading.value = false
  }
}

async function markRead(notification: NotificationItem) {
  if (notification.read) return
  try {
    const updated = (await markNotificationRead(notification.id)).data.data
    notifications.value = notifications.value.map((item) => item.id === updated.id ? updated : item)
    await notificationStore.refreshUnreadCount()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '通知状态更新失败')
  }
}

async function markAllRead() {
  try {
    await markAllNotificationsRead()
    notifications.value = notifications.value.map((item) => ({ ...item, read: true }))
    await notificationStore.refreshUnreadCount()
    ElMessage.success('全部通知已读')
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '操作失败')
  }
}

async function openTarget(notification: NotificationItem) {
  await markRead(notification)
  if (notification.type === 'ORGANIZER_APPLICATION_APPROVED') {
    await auth.loadCurrentUser()
  }
  if (notification.targetType === 'ACTIVITY' && notification.targetId) {
    if (notification.type === 'ACTIVITY_CANCELLED') {
      await router.push({ name: 'my-registrations' })
    } else if (notification.type === 'REGISTRATION_SUCCESS' || notification.type === 'ACTIVITY_START_REMINDER' || notification.type === 'ACTIVITY_ANNOUNCEMENT') {
      await router.push({ name: 'activity-detail', params: { id: notification.targetId } })
    } else {
      await router.push({ name: 'organizer-activities' })
    }
  } else if (notification.targetType === 'ORGANIZER_APPLICATION') {
    await router.push({ name: 'organizer-application' })
  }
}

function hasTarget(notification: NotificationItem) {
  return notification.targetType === 'ORGANIZER_APPLICATION'
    || (notification.targetType === 'ACTIVITY' && Boolean(notification.targetId))
}

function formatTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

onMounted(loadNotifications)
</script>

<template>
  <div class="page-card">
    <div class="page-heading">
      <div><h2>通知中心</h2><p>查看报名结果、审核结果、活动公告和开始提醒。</p></div>
      <div class="notification-actions"><el-button @click="loadNotifications">刷新</el-button><el-button type="primary" :disabled="notificationStore.unreadCount === 0" @click="markAllRead">全部已读</el-button></div>
    </div>
    <el-empty v-if="!loading && notifications.length === 0" description="暂无通知" />
    <div v-else v-loading="loading" class="notification-list">
      <div v-for="notification in notifications" :key="notification.id" class="notification-item" :class="{ unread: !notification.read }">
        <div class="notification-dot" />
        <div class="notification-content">
          <div class="notification-title-row"><strong>{{ notification.title }}</strong><el-tag size="small" effect="plain">{{ typeLabel[notification.type] ?? '系统' }}</el-tag></div>
          <p>{{ notification.content }}</p>
          <small>{{ formatTime(notification.createdAt) }}</small>
        </div>
        <div class="notification-item-actions"><el-button v-if="!notification.read" link type="primary" @click="markRead(notification)">标为已读</el-button><el-button v-if="hasTarget(notification)" link @click="openTarget(notification)">查看详情</el-button></div>
      </div>
    </div>
  </div>
</template>
