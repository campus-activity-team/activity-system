package com.example.activity.vo;

import com.example.activity.entity.OperationLog;
import com.example.activity.entity.User;

import java.time.LocalDateTime;

public record OperationLogView(
        Long id,
        Long userId,
        String username,
        String name,
        String operation,
        String targetType,
        Long targetId,
        LocalDateTime createdAt
) {
    public static OperationLogView from(OperationLog operationLog, User user) {
        return new OperationLogView(
                operationLog.getId(),
                operationLog.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? "系统" : user.getName(),
                operationLog.getOperation(),
                operationLog.getTargetType(),
                operationLog.getTargetId(),
                operationLog.getCreatedAt()
        );
    }
}
