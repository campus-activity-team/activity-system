package com.example.activity.controller;

import com.example.activity.common.ApiResponse;
import com.example.activity.dto.ActivityAnnouncementRequest;
import com.example.activity.service.ActivityAnnouncementService;
import com.example.activity.service.ActivityService;
import com.example.activity.service.CheckinService;
import com.example.activity.service.CheckinAnomalyService;
import com.example.activity.service.FeedbackService;
import com.example.activity.service.RegistrationService;
import com.example.activity.vo.ActivityAnnouncementView;
import com.example.activity.vo.ActivityView;
import com.example.activity.vo.AttendanceView;
import com.example.activity.vo.CheckinTokenView;
import com.example.activity.vo.CheckinAnomalyView;
import com.example.activity.vo.FeedbackDashboardView;
import com.example.activity.vo.RegistrationView;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final FeedbackService feedbackService;
    private final ActivityAnnouncementService announcementService;

    public OrganizerActivityController(
            ActivityService activityService,
            RegistrationService registrationService,
            CheckinService checkinService,
            CheckinAnomalyService checkinAnomalyService,
            FeedbackService feedbackService,
            ActivityAnnouncementService announcementService
    ) {
        this.activityService = activityService;
        this.registrationService = registrationService;
        this.checkinService = checkinService;
        this.checkinAnomalyService = checkinAnomalyService;
        this.feedbackService = feedbackService;
        this.announcementService = announcementService;
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

    @GetMapping("/{id}/registrations/export")
    public ResponseEntity<byte[]> exportRegistrations(
            @PathVariable Long id,
            Authentication authentication
    ) {
        byte[] content = registrationService.exportForActivity(id, authentication);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=activity-" + id + "-roster.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(content);
    }

    @PostMapping("/{id}/attendances/{userId}")
    public ApiResponse<AttendanceView> manualCheckin(
            @PathVariable Long id,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        return ApiResponse.success(checkinService.manualCheckin(id, userId, authentication));
    }

    @DeleteMapping("/{id}/attendances/{userId}")
    public ApiResponse<Void> cancelAttendance(
            @PathVariable Long id,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        checkinService.cancelAttendance(id, userId, authentication);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/checkin-anomalies")
    public ApiResponse<List<CheckinAnomalyView>> checkinAnomalies(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ApiResponse.success(checkinAnomalyService.listForActivity(id, authentication));
    }

    @GetMapping("/{id}/feedbacks")
    public ApiResponse<FeedbackDashboardView> feedbacks(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ApiResponse.success(feedbackService.dashboard(id, authentication));
    }

    @GetMapping("/{id}/announcements")
    public ApiResponse<List<ActivityAnnouncementView>> announcements(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ApiResponse.success(announcementService.list(id, authentication));
    }

    @PostMapping("/{id}/announcements")
    public ApiResponse<ActivityAnnouncementView> publishAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody ActivityAnnouncementRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(announcementService.publish(id, request, authentication));
    }

    @PostMapping("/{id}/checkin-token")
    public ApiResponse<CheckinTokenView> issueCheckinToken(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ApiResponse.success(checkinService.issueToken(id, authentication));
    }
}
