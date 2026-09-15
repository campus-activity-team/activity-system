package com.example.activity.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.activity.entity.OperationLog;
import com.example.activity.entity.User;
import com.example.activity.mapper.OperationLogMapper;
import com.example.activity.mapper.UserMapper;
import com.example.activity.vo.OperationLogView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;
    private final UserMapper userMapper;

    public OperationLogService(OperationLogMapper operationLogMapper, UserMapper userMapper) {
        this.operationLogMapper = operationLogMapper;
        this.userMapper = userMapper;
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

    @Transactional(readOnly = true)
    public List<OperationLogView> list(String operation, String targetType, Long userId, Integer limit) {
        int safeLimit = Math.max(1, Math.min(limit == null ? 100 : limit, 200));
        return operationLogMapper.selectList(Wrappers.<OperationLog>lambdaQuery()
                        .eq(operation != null && !operation.isBlank(), OperationLog::getOperation, normalize(operation))
                        .eq(targetType != null && !targetType.isBlank(), OperationLog::getTargetType, normalize(targetType))
                        .eq(userId != null, OperationLog::getUserId, userId)
                        .orderByDesc(OperationLog::getCreatedAt)
                        .last("LIMIT " + safeLimit))
                .stream()
                .map(operationLog -> {
                    User user = operationLog.getUserId() == null ? null : userMapper.selectById(operationLog.getUserId());
                    return OperationLogView.from(operationLog, user);
                })
                .toList();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
