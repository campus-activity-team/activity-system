package com.example.activity.vo;

import com.example.activity.entity.OrganizerApplicationStatus;

import java.time.LocalDateTime;

public record OrganizerApplicationView(
        Long id,
        Long userId,
        String username,
        String name,
        String studentId,
        String reason,
        OrganizerApplicationStatus status,
        String reviewComment,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
