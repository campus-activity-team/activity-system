package com.example.activity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectOrganizerApplicationRequest(
        @NotBlank(message = "驳回原因不能为空")
        @Size(max = 1000, message = "驳回原因不能超过 1000 个字符")
        String reviewComment
) {
}
