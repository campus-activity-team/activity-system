package com.example.activity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 64, message = "用户名长度应为 3-64 个字符")
        String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度应为 8-72 个字符")
        String password,
        @NotBlank(message = "姓名不能为空")
        @Size(max = 64, message = "姓名不能超过 64 个字符")
        String name,
        @Size(max = 64, message = "学号不能超过 64 个字符")
        String studentId,
        @Email(message = "邮箱格式不正确")
        @Size(max = 128, message = "邮箱不能超过 128 个字符")
        String email,
        @Size(max = 32, message = "手机号不能超过 32 个字符")
        String phone
) {
}
