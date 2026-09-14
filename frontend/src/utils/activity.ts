import type { Activity } from '../types/activity'

export type ActivityDisplay = {
  label: string
  type: 'success' | 'warning' | 'info' | 'danger'
  canRegister: boolean
}

function toDate(value?: string) {
  return value ? new Date(value) : null
}

export function getActivityDisplay(activity: Activity, now = new Date()): ActivityDisplay {
  const start = toDate(activity.startTime)
  const end = toDate(activity.endTime)
  const registrationStart = toDate(activity.registrationStartTime)
  const registrationEnd = toDate(activity.registrationEndTime)

  if (activity.status === 'ENDED' || (end && now >= end && (activity.status === 'PUBLISHED' || activity.status === 'ONGOING'))) {
    return { label: '已结束', type: 'info', canRegister: false }
  }
  if (activity.status === 'ONGOING' || (activity.status === 'PUBLISHED' && start && now >= start)) {
    return { label: '进行中', type: 'warning', canRegister: Boolean(registrationStart && registrationEnd && now >= registrationStart && now <= registrationEnd) }
  }
  if (activity.status === 'PUBLISHED') {
    if (registrationStart && now < registrationStart) {
      return { label: '报名未开始', type: 'info', canRegister: false }
    }
    if (registrationEnd && now > registrationEnd) {
      return { label: '报名已截止', type: 'danger', canRegister: false }
    }
    return { label: '报名中', type: 'success', canRegister: true }
  }

  const labels: Record<string, string> = {
    DRAFT: '草稿',
    PENDING_REVIEW: '审核中',
    REJECTED: '已驳回',
    APPROVED: '待发布',
    CANCELLED: '已取消',
  }
  return { label: labels[activity.status] ?? activity.status, type: 'info', canRegister: false }
}

export function formatActivityTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}
