package com.example.activity.controller;

import com.example.activity.common.ApiResponse;
import com.example.activity.dto.OrganizerApplicationRequest;
import com.example.activity.service.OrganizerApplicationService;
import com.example.activity.vo.OrganizerApplicationView;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizer-applications")
public class OrganizerApplicationController {

    private final OrganizerApplicationService organizerApplicationService;

    public OrganizerApplicationController(OrganizerApplicationService organizerApplicationService) {
        this.organizerApplicationService = organizerApplicationService;
    }

    @GetMapping("/mine")
    public ApiResponse<OrganizerApplicationView> mine(Authentication authentication) {
        return ApiResponse.success(organizerApplicationService.mine(authentication));
    }

    @PostMapping
    public ApiResponse<OrganizerApplicationView> apply(
            @Valid @RequestBody OrganizerApplicationRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(organizerApplicationService.apply(request, authentication));
    }
}
