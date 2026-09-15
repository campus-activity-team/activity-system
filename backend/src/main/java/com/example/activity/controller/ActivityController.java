package com.example.activity.controller;

import com.example.activity.common.ApiResponse;
import com.example.activity.dto.ActivityRequest;
import com.example.activity.dto.CancelActivityRequest;
import com.example.activity.entity.ActivityStatus;
import com.example.activity.service.ActivityService;
import com.example.activity.vo.ActivityView;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ApiResponse<List<ActivityView>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ActivityStatus status
    ) {
        return ApiResponse.success(activityService.listPublic(keyword, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<ActivityView> get(@PathVariable Long id) {
        return ApiResponse.success(activityService.getPublic(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ApiResponse<ActivityView> create(
            @Valid @RequestBody ActivityRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(activityService.create(request, authentication));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ApiResponse<ActivityView> update(
            @PathVariable Long id,
            @Valid @RequestBody ActivityRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(activityService.update(id, request, authentication));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication authentication) {
        activityService.delete(id, authentication);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ApiResponse<ActivityView> submit(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(activityService.submit(id, authentication));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ApiResponse<ActivityView> start(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(activityService.start(id, authentication));
    }

    @PostMapping("/{id}/end")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ApiResponse<ActivityView> end(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(activityService.end(id, authentication));
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ApiResponse<ActivityView> withdraw(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(activityService.withdrawReview(id, authentication));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ApiResponse<ActivityView> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelActivityRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(activityService.cancel(id, request, authentication));
    }
}
