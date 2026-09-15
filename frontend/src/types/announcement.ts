export interface ActivityAnnouncement {
  id: number
  activityId: number
  publisherId: number
  publisherName?: string
  title: string
  content: string
  createdAt: string
}

export interface ActivityAnnouncementPayload {
  title: string
  content: string
}
