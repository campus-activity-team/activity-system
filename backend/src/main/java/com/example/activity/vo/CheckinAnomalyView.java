package com.example.activity.vo;

import com.example.activity.entity.CheckinAnomalyReason;

import java.time.LocalDateTime;

public record CheckinAnomalyView(
        Long id,
        Long activityId,
        Long userId,
        String username,
        String name,
        String studentId,
        CheckinAnomalyReason reason,
        String message,
        Double latitude,
        Double longitude,
        Integer distanceMeters,
        LocalDateTime createdAt
) {
}
