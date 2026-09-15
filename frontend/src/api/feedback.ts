import { http } from './http'
import type { ApiEnvelope } from '../types/auth'
import type { Feedback, FeedbackDashboard, FeedbackPayload, FeedbackStatus } from '../types/feedback'

export function getMyFeedbackStatus(activityId: number) {
  return http.get<ApiEnvelope<FeedbackStatus>>(`/feedbacks/activities/${activityId}`)
}

export function submitFeedback(activityId: number, payload: FeedbackPayload) {
  return http.put<ApiEnvelope<Feedback>>(`/feedbacks/activities/${activityId}`, payload)
}

export function getActivityFeedbackDashboard(activityId: number) {
  return http.get<ApiEnvelope<FeedbackDashboard>>(`/organizer/activities/${activityId}/feedbacks`)
}
