package com.example.activity.vo;

import com.example.activity.entity.Notification;

import java.time.LocalDateTime;

public record NotificationView(
        Long id,
        String type,
        String title,
        String content,
        String targetType,
        Long targetId,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    public static NotificationView from(Notification notification) {
        return new NotificationView(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.getReadAt() != null,
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
