export interface NotificationItem {
  id: number
  type: string
  title: string
  content: string
  targetType?: string
  targetId?: number
  read: boolean
  readAt?: string
  createdAt: string
}

export interface UnreadNotificationCount {
  unreadCount: number
}
