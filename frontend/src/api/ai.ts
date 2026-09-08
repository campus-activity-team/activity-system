import { http } from './http'
import type { ApiEnvelope } from '../types/auth'
import type { ActivityCopyRequest, ActivityCopyResponse } from '../types/ai'

export function generateActivityCopy(payload: ActivityCopyRequest) {
  return http.post<ApiEnvelope<ActivityCopyResponse>>('/ai/activity-copy', payload)
}
