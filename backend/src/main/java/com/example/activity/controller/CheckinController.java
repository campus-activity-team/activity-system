package com.example.activity.controller;

import com.example.activity.common.ApiResponse;
import com.example.activity.dto.CheckinRequest;
import com.example.activity.service.CheckinService;
import com.example.activity.vo.AttendanceView;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkins")
public class CheckinController {

    private final CheckinService checkinService;

    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    @PostMapping
    public ApiResponse<AttendanceView> checkin(
            @Valid @RequestBody CheckinRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(checkinService.checkin(request, authentication));
    }
}
