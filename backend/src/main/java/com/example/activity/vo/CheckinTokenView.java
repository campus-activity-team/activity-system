package com.example.activity.vo;

import java.time.LocalDateTime;

public record CheckinTokenView(
        Long activityId,
        String token,
        LocalDateTime expiresAt,
        long expiresInSeconds
) {
}
