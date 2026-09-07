package com.example.activity.vo;

import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.entity.UserStatus;

public record UserProfile(
        Long id,
        String username,
        String name,
        String studentId,
        String email,
        String phone,
        String avatar,
        UserRole role,
        UserStatus status
) {

    public static UserProfile from(User user) {
        return new UserProfile(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getStudentId(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatar(),
                user.getRole(),
                user.getStatus()
        );
    }
}
