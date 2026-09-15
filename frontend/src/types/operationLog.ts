export interface OperationLogItem {
  id: number
  userId?: number
  username?: string
  name: string
  operation: string
  targetType?: string
  targetId?: number
  createdAt: string
}
