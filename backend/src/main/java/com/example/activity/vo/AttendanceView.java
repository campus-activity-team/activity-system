package com.example.activity.vo;

import java.time.LocalDateTime;

public record AttendanceView(
        Long activityId,
        Long userId,
        String activityTitle,
        String username,
        String name,
        String studentId,
        LocalDateTime checkinTime,
        String checkinMethod
) {
}
