package com.example.activity.vo;

import com.example.activity.entity.RegistrationStatus;

import java.time.LocalDateTime;

public record RegistrationView(
        Long id,
        Long activityId,
        String activityTitle,
        Long userId,
        String username,
        String name,
        String studentId,
        RegistrationStatus status,
        LocalDateTime registeredAt,
        LocalDateTime cancelledAt,
        boolean checkedIn,
        LocalDateTime checkinTime
) {
}
