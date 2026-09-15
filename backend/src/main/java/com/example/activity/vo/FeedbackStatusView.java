package com.example.activity.vo;

import com.example.activity.entity.ActivityStatus;

import java.time.LocalDateTime;

public record FeedbackStatusView(
        Long activityId,
        String activityTitle,
        ActivityStatus activityStatus,
        boolean required,
        boolean attended,
        boolean canSubmit,
        String unavailableReason,
        LocalDateTime feedbackDeadline,
        FeedbackView feedback
) {
}
