package com.example.activity.service;

import com.example.activity.entity.OperationLog;
import com.example.activity.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public void record(Long userId, String operation, String targetType, Long targetId) {
        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(userId);
        operationLog.setOperation(operation);
        operationLog.setTargetType(targetType);
        operationLog.setTargetId(targetId);
        operationLog.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(operationLog);
    }
}
