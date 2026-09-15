import { http } from './http'
import type { ApiEnvelope } from '../types/auth'
import type { OperationLogItem } from '../types/operationLog'

export interface OperationLogQuery {
  operation?: string
  targetType?: string
  userId?: number
  limit?: number
}

export function getOperationLogs(params?: OperationLogQuery) {
  return http.get<ApiEnvelope<OperationLogItem[]>>('/admin/operation-logs', { params })
}
