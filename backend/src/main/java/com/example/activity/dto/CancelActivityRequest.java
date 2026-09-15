package com.example.activity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelActivityRequest(
        @NotBlank(message = "取消原因不能为空")
        @Size(max = 500, message = "取消原因不能超过 500 个字符")
        String reason
) {
}
