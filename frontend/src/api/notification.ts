import { http } from './http'
import type { ApiEnvelope } from '../types/auth'
import type { NotificationItem, UnreadNotificationCount } from '../types/notification'

export function getNotifications() {
  return http.get<ApiEnvelope<NotificationItem[]>>('/notifications')
}

export function getUnreadNotificationCount() {
  return http.get<ApiEnvelope<UnreadNotificationCount>>('/notifications/unread-count')
}

export function markNotificationRead(id: number) {
  return http.post<ApiEnvelope<NotificationItem>>(`/notifications/${id}/read`)
}

export function markAllNotificationsRead() {
  return http.post<ApiEnvelope<null>>('/notifications/read-all')
}
