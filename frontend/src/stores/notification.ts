import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getUnreadNotificationCount } from '../api/notification'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)

  async function refreshUnreadCount() {
    try {
      unreadCount.value = (await getUnreadNotificationCount()).data.data.unreadCount
    } catch {
      unreadCount.value = 0
    }
  }

  function reset() {
    unreadCount.value = 0
  }

  return { unreadCount, refreshUnreadCount, reset }
})
