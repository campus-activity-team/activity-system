package com.example.activity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivityAnnouncementRequest(
        @NotBlank(message = "公告标题不能为空")
        @Size(max = 120, message = "公告标题不能超过 120 个字符")
        String title,
        @NotBlank(message = "公告内容不能为空")
        @Size(max = 2000, message = "公告内容不能超过 2000 个字符")
        String content
) {
}
