import { http } from './http'
import type { ApiEnvelope } from '../types/auth'
import type { Attendance, CheckinResult, CheckinToken, Registration } from '../types/registration'

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

export function getActivityAttendances(activityId: number) {
  return http.get<ApiEnvelope<Attendance[]>>(`/organizer/activities/${activityId}/attendances`)
}

export function issueCheckinToken(activityId: number) {
  return http.post<ApiEnvelope<CheckinToken>>(`/organizer/activities/${activityId}/checkin-token`)
}

export function checkin(token: string) {
  return http.post<ApiEnvelope<CheckinResult>>('/checkins', { token })
}
