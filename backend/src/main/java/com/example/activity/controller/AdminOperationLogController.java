package com.example.activity.controller;

import com.example.activity.common.ApiResponse;
import com.example.activity.service.OperationLogService;
import com.example.activity.vo.OperationLogView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/operation-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOperationLogController {

    private final OperationLogService operationLogService;

    public AdminOperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public ApiResponse<List<OperationLogView>> list(
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "100") Integer limit
    ) {
        return ApiResponse.success(operationLogService.list(operation, targetType, userId, limit));
    }
}
