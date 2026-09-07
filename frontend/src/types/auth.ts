export type UserRole = 'USER' | 'ORGANIZER' | 'ADMIN'
export type UserStatus = 'ACTIVE' | 'DISABLED'

export interface UserProfile {
  id: number
  username: string
  name: string
  studentId?: string
  email?: string
  phone?: string
  avatar?: string
  role: UserRole
  status: UserStatus
}

export interface LoginResponse {
  token: string
  expiresIn: number
  user: UserProfile
}

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}
