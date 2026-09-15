export type ActivityStatus =
  | 'DRAFT'
  | 'PENDING_REVIEW'
  | 'REJECTED'
  | 'APPROVED'
  | 'PUBLISHED'
  | 'ONGOING'
  | 'ENDED'
  | 'CANCELLED'

export interface Activity {
  id: number
  title: string
  description: string
  coverImage?: string
  organizerId: number
  location: string
  checkinLatitude?: number
  checkinLongitude?: number
  checkinRadiusMeters?: number
  locationCheckinRequired: boolean
  startTime: string
  endTime: string
  registrationStartTime: string
  registrationEndTime: string
  capacity: number
  currentRegisteredCount: number
  remainingCapacity: number
  status: ActivityStatus
  reviewComment?: string
  requireFeedback: boolean
  feedbackDeadline?: string
  createdAt: string
  updatedAt: string
}

export interface ActivityPayload {
  title: string
  description: string
  coverImage?: string
  location: string
  checkinLatitude?: number
  checkinLongitude?: number
  checkinRadiusMeters?: number
  startTime: string
  endTime: string
  registrationStartTime: string
  registrationEndTime: string
  capacity: number
  requireFeedback: boolean
  feedbackDeadline?: string
}
