package com.example.activity.controller;

import com.example.activity.ai.ActivityCopyRequest;
import com.example.activity.ai.ActivityCopyResponse;
import com.example.activity.ai.ActivityCopyService;
import com.example.activity.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.example.activity.security.AuthenticatedUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final ActivityCopyService activityCopyService;

    public AiController(ActivityCopyService activityCopyService) {
        this.activityCopyService = activityCopyService;
    }

    @PostMapping("/activity-copy")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ApiResponse<ActivityCopyResponse> generateActivityCopy(
            @Valid @RequestBody ActivityCopyRequest request,
            Authentication authentication
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.success(activityCopyService.generate(request, user.user().getId()));
    }
}
