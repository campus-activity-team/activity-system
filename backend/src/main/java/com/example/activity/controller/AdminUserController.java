package com.example.activity.controller;

import com.example.activity.common.ApiResponse;
import com.example.activity.dto.AdminRoleChangeRequest;
import com.example.activity.dto.RejectOrganizerApplicationRequest;
import com.example.activity.service.AdminUserService;
import com.example.activity.service.OrganizerApplicationService;
import com.example.activity.vo.OrganizerApplicationView;
import com.example.activity.vo.UserProfile;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final OrganizerApplicationService organizerApplicationService;

    public AdminUserController(
            AdminUserService adminUserService,
            OrganizerApplicationService organizerApplicationService
    ) {
        this.adminUserService = adminUserService;
        this.organizerApplicationService = organizerApplicationService;
    }

    @GetMapping("/users")
    public ApiResponse<List<UserProfile>> listUsers() {
        return ApiResponse.success(adminUserService.listUsers());
    }

    @PostMapping("/users/{id}/role")
    public ApiResponse<UserProfile> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody AdminRoleChangeRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(adminUserService.changeRole(id, request, authentication));
    }

    @GetMapping("/organizer-applications")
    public ApiResponse<List<OrganizerApplicationView>> listOrganizerApplications() {
        return ApiResponse.success(organizerApplicationService.listAll());
    }

    @PostMapping("/organizer-applications/{id}/approve")
    public ApiResponse<OrganizerApplicationView> approveOrganizerApplication(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ApiResponse.success(organizerApplicationService.approve(id, authentication));
    }

    @PostMapping("/organizer-applications/{id}/reject")
    public ApiResponse<OrganizerApplicationView> rejectOrganizerApplication(
            @PathVariable Long id,
            @Valid @RequestBody RejectOrganizerApplicationRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(organizerApplicationService.reject(id, request, authentication));
    }
}
