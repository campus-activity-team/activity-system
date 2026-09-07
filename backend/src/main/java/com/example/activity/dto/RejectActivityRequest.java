package com.example.activity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectActivityRequest(
        @NotBlank(message = "驳回意见不能为空")
        @Size(max = 1000, message = "驳回意见不能超过 1000 个字符")
        String reviewComment
) {
}
