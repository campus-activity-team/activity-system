export type RegistrationStatus = 'REGISTERED' | 'CANCELLED'

export interface Registration {
  id: number
  activityId: number
  activityTitle: string
  userId: number
  username: string
  name: string
  studentId?: string
  status: RegistrationStatus
  registeredAt: string
  cancelledAt?: string
  checkedIn: boolean
  checkinTime?: string
}

export interface CheckinToken {
  activityId: number
  token: string
  expiresAt: string
  expiresInSeconds: number
}

export interface Attendance {
  activityId: number
  userId: number
  activityTitle: string
  username: string
  name: string
  studentId?: string
  checkinTime: string
  checkinMethod: string
}

export interface CheckinResult {
  activityId: number
  userId: number
  activityTitle: string
  username: string
  name: string
  studentId?: string
  checkinTime: string
  checkinMethod: string
}
