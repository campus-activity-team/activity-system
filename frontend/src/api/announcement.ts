import { http } from './http'
import type { ApiEnvelope } from '../types/auth'
import type { ActivityAnnouncement, ActivityAnnouncementPayload } from '../types/announcement'

export function getActivityAnnouncements(activityId: number) {
  return http.get<ApiEnvelope<ActivityAnnouncement[]>>(`/organizer/activities/${activityId}/announcements`)
}

export function publishActivityAnnouncement(activityId: number, payload: ActivityAnnouncementPayload) {
  return http.post<ApiEnvelope<ActivityAnnouncement>>(`/organizer/activities/${activityId}/announcements`, payload)
}
