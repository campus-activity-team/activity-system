package com.example.activity.controller;

import com.example.activity.common.ApiResponse;
import com.example.activity.service.ActivityService;
import com.example.activity.service.CheckinService;
import com.example.activity.service.CheckinAnomalyService;
import com.example.activity.service.RegistrationService;
import com.example.activity.vo.ActivityView;
import com.example.activity.vo.AttendanceView;
import com.example.activity.vo.CheckinTokenView;
import com.example.activity.vo.CheckinAnomalyView;
import com.example.activity.vo.RegistrationView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/organizer/activities")
@PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
public class OrganizerActivityController {

    private final ActivityService activityService;
    private final RegistrationService registrationService;
    private final CheckinService checkinService;
    private final CheckinAnomalyService checkinAnomalyService;

    public OrganizerActivityController(
            ActivityService activityService,
            RegistrationService registrationService,
            CheckinService checkinService,
            CheckinAnomalyService checkinAnomalyService
    ) {
        this.activityService = activityService;
        this.registrationService = registrationService;
        this.checkinService = checkinService;
        this.checkinAnomalyService = checkinAnomalyService;
    }

    @GetMapping
    public ApiResponse<List<ActivityView>> listMine(Authentication authentication) {
        return ApiResponse.success(activityService.listMine(authentication));
    }

    @GetMapping("/{id}")
    public ApiResponse<ActivityView> getMine(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(activityService.getMine(id, authentication));
    }

    @GetMapping("/{id}/registrations")
    public ApiResponse<List<RegistrationView>> registrations(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ApiResponse.success(registrationService.listForActivity(id, authentication));
    }

    @GetMapping("/{id}/attendances")
    public ApiResponse<List<AttendanceView>> attendances(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ApiResponse.success(checkinService.listForActivity(id, authentication));
    }

    @GetMapping("/{id}/checkin-anomalies")
    public ApiResponse<List<CheckinAnomalyView>> checkinAnomalies(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ApiResponse.success(checkinAnomalyService.listForActivity(id, authentication));
    }

    @PostMapping("/{id}/checkin-token")
    public ApiResponse<CheckinTokenView> issueCheckinToken(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ApiResponse.success(checkinService.issueToken(id, authentication));
    }
}
