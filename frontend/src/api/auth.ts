import { http } from './http'
import type { ApiEnvelope, LoginResponse, UserProfile } from '../types/auth'

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload extends LoginPayload {
  name: string
  studentId?: string
  email?: string
  phone?: string
}

export function login(payload: LoginPayload) {
  return http.post<ApiEnvelope<LoginResponse>>('/auth/login', payload)
}

export function register(payload: RegisterPayload) {
  return http.post<ApiEnvelope<UserProfile>>('/auth/register', payload)
}

export function getCurrentUser() {
  return http.get<ApiEnvelope<UserProfile>>('/auth/me')
}

export function logout() {
  return http.post<ApiEnvelope<null>>('/auth/logout')
}
