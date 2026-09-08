package com.example.activity.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivityCopyRequest(
        @NotBlank(message = "活动主题不能为空")
        @Size(max = 120, message = "活动主题不能超过 120 个字符")
        String topic,
        @Size(max = 80, message = "活动类型不能超过 80 个字符")
        String activityType,
        @Size(max = 120, message = "目标人群不能超过 120 个字符")
        String targetAudience,
        @Size(max = 160, message = "活动地点不能超过 160 个字符")
        String location,
        @Size(max = 120, message = "活动时间不能超过 120 个字符")
        String activityTime,
        @Size(max = 200, message = "关键词不能超过 200 个字符")
        String keywords,
        @Size(max = 40, message = "文案风格不能超过 40 个字符")
        String tone
) {
}
