import type { ActivityStatus } from './activity'

export interface FeedbackPayload {
  overallRating: number
  contentRating: number
  serviceRating: number
  comment?: string
}

export interface Feedback extends FeedbackPayload {
  id: number
  activityId: number
  activityTitle: string
  createdAt: string
  updatedAt: string
}

export interface FeedbackStatus {
  activityId: number
  activityTitle: string
  activityStatus: ActivityStatus
  required: boolean
  attended: boolean
  canSubmit: boolean
  unavailableReason?: string
  feedbackDeadline?: string
  feedback?: Feedback
}

export interface FeedbackDashboard {
  activityId: number
  activityTitle: string
  registeredCount: number
  attendedCount: number
  feedbackCount: number
  responseRate: number
  averageOverallRating: number
  averageContentRating: number
  averageServiceRating: number
  overallRatingDistribution: Record<number, number>
  feedbacks: Feedback[]
}
