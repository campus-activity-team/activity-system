import type { UserProfile, UserRole } from './auth'

export type OrganizerApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface OrganizerApplication {
  id: number
  userId: number
  username: string
  name: string
  studentId?: string
  reason: string
  status: OrganizerApplicationStatus
  reviewComment?: string
  reviewedBy?: number
  reviewedAt?: string
  createdAt: string
  updatedAt: string
}

export interface AdminRoleChangePayload {
  role: UserRole
  currentPassword: string
}

export type ManagedUser = UserProfile
