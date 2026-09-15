import { http } from './http'
import type { ApiEnvelope } from '../types/auth'
import type { Attendance, CheckinAnomaly, CheckinResult, CheckinToken, Registration } from '../types/registration'

export function getMyRegistrations() {
  return http.get<ApiEnvelope<Registration[]>>('/registrations/mine')
}

export function getMyActivityRegistration(activityId: number) {
  return http.get<ApiEnvelope<Registration | null>>(`/registrations/activities/${activityId}`)
}

export function registerActivity(activityId: number) {
  return http.post<ApiEnvelope<Registration>>(`/registrations/activities/${activityId}`)
}

export function cancelRegistration(activityId: number) {
  return http.delete<ApiEnvelope<null>>(`/registrations/activities/${activityId}`)
}

export function getActivityRegistrations(activityId: number) {
  return http.get<ApiEnvelope<Registration[]>>(`/organizer/activities/${activityId}/registrations`)
}

export function exportActivityRoster(activityId: number) {
  return http.get<Blob>(`/organizer/activities/${activityId}/registrations/export`, { responseType: 'blob' })
}

export function getActivityAttendances(activityId: number) {
  return http.get<ApiEnvelope<Attendance[]>>(`/organizer/activities/${activityId}/attendances`)
}

export function manualCheckinParticipant(activityId: number, userId: number) {
  return http.post<ApiEnvelope<Attendance>>(`/organizer/activities/${activityId}/attendances/${userId}`)
}

export function cancelParticipantAttendance(activityId: number, userId: number) {
  return http.delete<ApiEnvelope<null>>(`/organizer/activities/${activityId}/attendances/${userId}`)
}

export function getActivityCheckinAnomalies(activityId: number) {
  return http.get<ApiEnvelope<CheckinAnomaly[]>>(`/organizer/activities/${activityId}/checkin-anomalies`)
}

export function issueCheckinToken(activityId: number) {
  return http.post<ApiEnvelope<CheckinToken>>(`/organizer/activities/${activityId}/checkin-token`)
}

export function checkin(token: string, location?: { latitude: number; longitude: number }) {
  return http.post<ApiEnvelope<CheckinResult>>('/checkins', { token, ...location })
}
