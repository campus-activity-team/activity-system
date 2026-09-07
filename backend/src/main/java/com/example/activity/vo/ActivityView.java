package com.example.activity.vo;

import com.example.activity.entity.Activity;
import com.example.activity.entity.ActivityStatus;

import java.time.LocalDateTime;

public record ActivityView(
        Long id,
        String title,
        String description,
        String coverImage,
        Long organizerId,
        String location,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime registrationStartTime,
        LocalDateTime registrationEndTime,
        Integer capacity,
        Integer currentRegisteredCount,
        Integer remainingCapacity,
        ActivityStatus status,
        String reviewComment,
        Boolean requireFeedback,
        LocalDateTime feedbackDeadline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ActivityView from(Activity activity) {
        int registered = activity.getCurrentRegisteredCount() == null ? 0 : activity.getCurrentRegisteredCount();
        int capacity = activity.getCapacity() == null ? 0 : activity.getCapacity();
        return new ActivityView(
                activity.getId(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getCoverImage(),
                activity.getOrganizerId(),
                activity.getLocation(),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getRegistrationStartTime(),
                activity.getRegistrationEndTime(),
                activity.getCapacity(),
                registered,
                Math.max(capacity - registered, 0),
                activity.getStatus(),
                activity.getReviewComment(),
                activity.getRequireFeedback(),
                activity.getFeedbackDeadline(),
                activity.getCreatedAt(),
                activity.getUpdatedAt()
        );
    }
}
