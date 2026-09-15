package com.example.activity.vo;

import com.example.activity.entity.ActivityAnnouncement;

import java.time.LocalDateTime;

public record ActivityAnnouncementView(
        Long id,
        Long activityId,
        Long publisherId,
        String publisherName,
        String title,
        String content,
        LocalDateTime createdAt
) {
    public static ActivityAnnouncementView from(ActivityAnnouncement announcement, String publisherName) {
        return new ActivityAnnouncementView(
                announcement.getId(),
                announcement.getActivityId(),
                announcement.getPublisherId(),
                publisherName,
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getCreatedAt()
        );
    }
}
