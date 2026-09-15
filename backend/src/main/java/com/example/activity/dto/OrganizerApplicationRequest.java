package com.example.activity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizerApplicationRequest(
        @NotBlank(message = "申请理由不能为空")
        @Size(max = 1000, message = "申请理由不能超过 1000 个字符")
        String reason
) {
}
