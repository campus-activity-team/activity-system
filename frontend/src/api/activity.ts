import { http } from './http'
import type { ApiEnvelope } from '../types/auth'
import type { Activity, ActivityPayload, ActivityStatus } from '../types/activity'

export function getActivities(params?: { keyword?: string; status?: ActivityStatus }) {
  return http.get<ApiEnvelope<Activity[]>>('/activities', { params })
}

export function getActivity(id: number) {
  return http.get<ApiEnvelope<Activity>>(`/activities/${id}`)
}

export function createActivity(payload: ActivityPayload) {
  return http.post<ApiEnvelope<Activity>>('/activities', payload)
}

export function updateActivity(id: number, payload: ActivityPayload) {
  return http.put<ApiEnvelope<Activity>>(`/activities/${id}`, payload)
}

export function submitActivity(id: number) {
  return http.post<ApiEnvelope<Activity>>(`/activities/${id}/submit`)
}

export function startActivity(id: number) {
  return http.post<ApiEnvelope<Activity>>(`/activities/${id}/start`)
}

export function deleteActivity(id: number) {
  return http.delete<ApiEnvelope<null>>(`/activities/${id}`)
}

export function getMyActivities() {
  return http.get<ApiEnvelope<Activity[]>>('/organizer/activities')
}

export function getReviewActivities() {
  return http.get<ApiEnvelope<Activity[]>>('/admin/reviews')
}

export function getAdminActivities() {
  return http.get<ApiEnvelope<Activity[]>>('/admin/activities')
}

export function approveActivity(id: number) {
  return http.post<ApiEnvelope<Activity>>(`/admin/activities/${id}/approve`)
}

export function rejectActivity(id: number, reviewComment: string) {
  return http.post<ApiEnvelope<Activity>>(`/admin/activities/${id}/reject`, { reviewComment })
}

export function publishActivity(id: number) {
  return http.post<ApiEnvelope<Activity>>(`/admin/activities/${id}/publish`)
}

export function unpublishActivity(id: number) {
  return http.post<ApiEnvelope<Activity>>(`/admin/activities/${id}/unpublish`)
}
