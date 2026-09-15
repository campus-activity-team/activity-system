import { http } from './http'
import type { ApiEnvelope, UserProfile } from '../types/auth'
import type { AdminRoleChangePayload, OrganizerApplication } from '../types/organizer'

export function getMyOrganizerApplication() {
  return http.get<ApiEnvelope<OrganizerApplication | null>>('/organizer-applications/mine')
}

export function submitOrganizerApplication(reason: string) {
  return http.post<ApiEnvelope<OrganizerApplication>>('/organizer-applications', { reason })
}

export function getAdminUsers() {
  return http.get<ApiEnvelope<UserProfile[]>>('/admin/users')
}

export function changeUserRole(userId: number, payload: AdminRoleChangePayload) {
  return http.post<ApiEnvelope<UserProfile>>(`/admin/users/${userId}/role`, payload)
}

export function getOrganizerApplications() {
  return http.get<ApiEnvelope<OrganizerApplication[]>>('/admin/organizer-applications')
}

export function approveOrganizerApplication(id: number) {
  return http.post<ApiEnvelope<OrganizerApplication>>(`/admin/organizer-applications/${id}/approve`)
}

export function rejectOrganizerApplication(id: number, reviewComment: string) {
  return http.post<ApiEnvelope<OrganizerApplication>>(`/admin/organizer-applications/${id}/reject`, { reviewComment })
}
