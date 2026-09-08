export interface ActivityCopyRequest {
  topic: string
  activityType?: string
  targetAudience?: string
  location?: string
  activityTime?: string
  keywords?: string
  tone?: string
}

export interface ActivityCopyResponse {
  title: string
  description: string
  highlights: string[]
  notices: string[]
  tags: string[]
  model: string
}
