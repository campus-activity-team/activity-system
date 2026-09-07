package com.example.activity.controller;

import com.example.activity.common.ApiResponse;
import com.example.activity.service.ActivityService;
import com.example.activity.vo.ActivityView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/organizer/activities")
@PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
public class OrganizerActivityController {

    private final ActivityService activityService;

    public OrganizerActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ApiResponse<List<ActivityView>> listMine(Authentication authentication) {
        return ApiResponse.success(activityService.listMine(authentication));
    }

    @GetMapping("/{id}")
    public ApiResponse<ActivityView> getMine(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(activityService.getMine(id, authentication));
    }
}
