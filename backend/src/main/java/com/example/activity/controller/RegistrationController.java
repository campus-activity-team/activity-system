package com.example.activity.controller;

import com.example.activity.common.ApiResponse;
import com.example.activity.service.RegistrationService;
import com.example.activity.vo.RegistrationView;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/mine")
    public ApiResponse<List<RegistrationView>> mine(Authentication authentication) {
        return ApiResponse.success(registrationService.mine(authentication));
    }

    @GetMapping("/activities/{activityId}")
    public ApiResponse<RegistrationView> mineForActivity(
            @PathVariable Long activityId,
            Authentication authentication
    ) {
        return ApiResponse.success(registrationService.mineForActivity(activityId, authentication));
    }

    @PostMapping("/activities/{activityId}")
    public ApiResponse<RegistrationView> register(
            @PathVariable Long activityId,
            Authentication authentication
    ) {
        return ApiResponse.success(registrationService.register(activityId, authentication));
    }

    @DeleteMapping("/activities/{activityId}")
    public ApiResponse<Void> cancel(
            @PathVariable Long activityId,
            Authentication authentication
    ) {
        registrationService.cancel(activityId, authentication);
        return ApiResponse.success();
    }
}
