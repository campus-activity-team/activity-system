package com.example.activity.vo;

import java.time.LocalDateTime;

public record FeedbackView(
        Long id,
        Long activityId,
        String activityTitle,
        Integer overallRating,
        Integer contentRating,
        Integer serviceRating,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
