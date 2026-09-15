package com.example.activity.service;

import com.example.activity.entity.OperationLog;
import com.example.activity.entity.User;
import com.example.activity.mapper.OperationLogMapper;
import com.example.activity.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperationLogServiceTest {

    @Test
    void includesOperatorIdentityInAdminLogView() {
        OperationLogMapper operationLogMapper = mock(OperationLogMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        OperationLogService service = new OperationLogService(operationLogMapper, userMapper);
        OperationLog operationLog = new OperationLog();
        operationLog.setId(5L);
        operationLog.setUserId(1L);
        operationLog.setOperation("ACTIVITY_APPROVED");
        operationLog.setTargetType("ACTIVITY");
        operationLog.setTargetId(20L);
        operationLog.setCreatedAt(LocalDateTime.of(2026, 9, 15, 10, 0));
        User administrator = new User();
        administrator.setId(1L);
        administrator.setUsername("admin");
        administrator.setName("管理员");
        when(operationLogMapper.selectList(any())).thenReturn(List.of(operationLog));
        when(userMapper.selectById(1L)).thenReturn(administrator);

        var result = service.list("activity_approved", "activity", 1L, 100);

        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).username());
        assertEquals("管理员", result.get(0).name());
    }
}
