package com.example.activity.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ActivityRequest(
        @NotBlank(message = "活动名称不能为空")
        @Size(max = 200, message = "活动名称不能超过 200 个字符")
        String title,
        @NotBlank(message = "活动介绍不能为空")
        String description,
        @Size(max = 512, message = "封面地址不能超过 512 个字符")
        String coverImage,
        @NotBlank(message = "活动地点不能为空")
        @Size(max = 255, message = "活动地点不能超过 255 个字符")
        String location,
        @NotNull(message = "活动开始时间不能为空")
        @Future(message = "活动开始时间必须晚于当前时间")
        LocalDateTime startTime,
        @NotNull(message = "活动结束时间不能为空")
        LocalDateTime endTime,
        @NotNull(message = "报名开始时间不能为空")
        LocalDateTime registrationStartTime,
        @NotNull(message = "报名结束时间不能为空")
        LocalDateTime registrationEndTime,
        @NotNull(message = "活动容量不能为空")
        @Min(value = 1, message = "活动容量至少为 1")
        Integer capacity,
        Boolean requireFeedback,
        LocalDateTime feedbackDeadline
) {
}
