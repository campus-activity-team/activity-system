package com.example.activity.controller;

import com.example.activity.common.ApiResponse;
import com.example.activity.dto.RejectActivityRequest;
import com.example.activity.service.ActivityService;
import com.example.activity.vo.ActivityView;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminActivityController {

    private final ActivityService activityService;

    public AdminActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/reviews")
    public ApiResponse<List<ActivityView>> listPendingReviews() {
        return ApiResponse.success(activityService.listPendingReview());
    }

    @GetMapping("/activities")
    public ApiResponse<List<ActivityView>> listAll() {
        return ApiResponse.success(activityService.listAll());
    }

    @PostMapping("/activities/{id}/approve")
    public ApiResponse<ActivityView> approve(@PathVariable Long id) {
        return ApiResponse.success(activityService.approve(id));
    }

    @PostMapping("/activities/{id}/reject")
    public ApiResponse<ActivityView> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectActivityRequest request
    ) {
        return ApiResponse.success(activityService.reject(id, request));
    }

    @PostMapping("/activities/{id}/publish")
    public ApiResponse<ActivityView> publish(@PathVariable Long id) {
        return ApiResponse.success(activityService.publish(id));
    }

    @PostMapping("/activities/{id}/unpublish")
    public ApiResponse<ActivityView> unpublish(@PathVariable Long id) {
        return ApiResponse.success(activityService.unpublish(id));
    }
}
