package com.example.activity.dto;

import com.example.activity.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminRoleChangeRequest(
        @NotNull(message = "目标角色不能为空")
        UserRole role,
        @NotBlank(message = "请输入当前管理员密码确认操作")
        String currentPassword
) {
}
