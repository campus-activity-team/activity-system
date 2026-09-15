package com.example.activity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(
        @NotNull(message = "请填写总体评分")
        @Min(value = 1, message = "总体评分不能低于 1 分")
        @Max(value = 5, message = "总体评分不能高于 5 分")
        Integer overallRating,
        @NotNull(message = "请填写内容评分")
        @Min(value = 1, message = "内容评分不能低于 1 分")
        @Max(value = 5, message = "内容评分不能高于 5 分")
        Integer contentRating,
        @NotNull(message = "请填写服务评分")
        @Min(value = 1, message = "服务评分不能低于 1 分")
        @Max(value = 5, message = "服务评分不能高于 5 分")
        Integer serviceRating,
        @Size(max = 2000, message = "反馈内容不能超过 2000 个字符")
        String comment
) {
}
