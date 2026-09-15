package com.example.activity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record CheckinRequest(
        @NotBlank(message = "签到令牌不能为空")
        String token,
        @DecimalMin(value = "-90.0", message = "定位纬度无效")
        @DecimalMax(value = "90.0", message = "定位纬度无效")
        Double latitude,
        @DecimalMin(value = "-180.0", message = "定位经度无效")
        @DecimalMax(value = "180.0", message = "定位经度无效")
        Double longitude
) {
}
