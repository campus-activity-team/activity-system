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
        Double checkinLatitude,
        Double checkinLongitude,
        Integer checkinRadiusMeters,
        boolean locationCheckinRequired,
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
        return from(activity, true);
    }

    public static ActivityView publicFrom(Activity activity) {
        return from(activity, false);
    }

    private static ActivityView from(Activity activity, boolean includeCheckinCoordinates) {
        int registered = activity.getCurrentRegisteredCount() == null ? 0 : activity.getCurrentRegisteredCount();
        int capacity = activity.getCapacity() == null ? 0 : activity.getCapacity();
        boolean locationCheckinRequired = activity.getCheckinLatitude() != null
                && activity.getCheckinLongitude() != null
                && activity.getCheckinRadiusMeters() != null;
        return new ActivityView(
                activity.getId(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getCoverImage(),
                activity.getOrganizerId(),
                activity.getLocation(),
                includeCheckinCoordinates ? activity.getCheckinLatitude() : null,
                includeCheckinCoordinates ? activity.getCheckinLongitude() : null,
                includeCheckinCoordinates ? activity.getCheckinRadiusMeters() : null,
                locationCheckinRequired,
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
