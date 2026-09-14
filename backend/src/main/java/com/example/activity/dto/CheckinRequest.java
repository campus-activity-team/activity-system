package com.example.activity.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckinRequest(
        @NotBlank(message = "签到令牌不能为空")
        String token
) {
}
