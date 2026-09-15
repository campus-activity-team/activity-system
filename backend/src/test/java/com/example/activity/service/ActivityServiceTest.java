package com.example.activity.service;

import com.example.activity.dto.ActivityRequest;
import com.example.activity.entity.Activity;
import com.example.activity.entity.ActivityStatus;
import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.entity.UserStatus;
import com.example.activity.exception.BusinessException;
import com.example.activity.mapper.ActivityMapper;
import com.example.activity.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityServiceTest {

    private ActivityMapper activityMapper;
    private OperationLogService operationLogService;
    private NotificationService notificationService;
    private ActivityService activityService;
    private Authentication organizerAuthentication;

    @BeforeEach
    void setUp() {
        activityMapper = mock(ActivityMapper.class);
        operationLogService = mock(OperationLogService.class);
        notificationService = mock(NotificationService.class);
        activityService = new ActivityService(activityMapper, operationLogService, notificationService);
        User user = new User();
        user.setId(7L);
        user.setUsername("organizer");
        user.setName("组织者");
        user.setRole(UserRole.ORGANIZER);
        user.setStatus(UserStatus.ACTIVE);
        organizerAuthentication = new UsernamePasswordAuthenticationToken(
                AuthenticatedUser.from(user), null, AuthenticatedUser.from(user).getAuthorities()
        );
    }

    @Test
    void createsDraftForAuthenticatedOrganizer() {
        when(activityMapper.insert(any(Activity.class))).thenAnswer(invocation -> {
            Activity activity = invocation.getArgument(0);
            activity.setId(12L);
            return 1;
        });

        ActivityRequest request = validRequest();

        var result = activityService.create(request, organizerAuthentication);

        assertEquals(12L, result.id());
        assertEquals(7L, result.organizerId());
        assertEquals(ActivityStatus.DRAFT, result.status());
        verify(activityMapper).insert(any(Activity.class));
    }

    @Test
    void rejectsInvalidScheduleBeforePersisting() {
        ActivityRequest request = new ActivityRequest(
                "活动", "介绍", null, "地点",
                null, null, null,
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusHours(12),
                10, false, null
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> activityService.create(request, organizerAuthentication)
        );

        assertEquals(400, exception.getCode());
    }

    @Test
    void preventsOrganizerFromUpdatingAnotherOrganizersActivity() {
        Activity activity = new Activity();
        activity.setId(20L);
        activity.setOrganizerId(99L);
        activity.setStatus(ActivityStatus.DRAFT);
        activity.setCurrentRegisteredCount(0);
        when(activityMapper.selectById(20L)).thenReturn(activity);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> activityService.update(20L, validRequest(), organizerAuthentication)
        );

        assertEquals(403, exception.getCode());
    }

    @Test
    void rejectsPublicLookupForDraftActivity() {
        Activity activity = new Activity();
        activity.setId(21L);
        activity.setStatus(ActivityStatus.DRAFT);
        when(activityMapper.selectById(21L)).thenReturn(activity);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> activityService.getPublic(21L)
        );

        assertEquals(404, exception.getCode());
    }

    @Test
    void publicActivityHidesCheckinCoordinates() {
        Activity activity = new Activity();
        activity.setId(23L);
        activity.setStatus(ActivityStatus.ONGOING);
        activity.setEndTime(LocalDateTime.now().plusHours(2));
        activity.setCheckinLatitude(31.2304);
        activity.setCheckinLongitude(121.4737);
        activity.setCheckinRadiusMeters(100);
        when(activityMapper.selectById(23L)).thenReturn(activity);

        var result = activityService.getPublic(23L);

        assertTrue(result.locationCheckinRequired());
        assertNull(result.checkinLatitude());
        assertNull(result.checkinLongitude());
        assertNull(result.checkinRadiusMeters());
    }

    @Test
    void organizerCanStartPublishedActivityBeforeScheduledTime() {
        Activity activity = new Activity();
        activity.setId(22L);
        activity.setOrganizerId(7L);
        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setStartTime(LocalDateTime.now().plusDays(1));
        activity.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        when(activityMapper.selectById(22L)).thenReturn(activity);

        var result = activityService.start(22L, organizerAuthentication);

        assertEquals(ActivityStatus.ONGOING, result.status());
        verify(activityMapper).updateById(activity);
        verify(operationLogService).record(7L, "ACTIVITY_MANUALLY_STARTED", "ACTIVITY", 22L);
    }

    @Test
    void administratorApprovalNotifiesOrganizerAndWritesAuditLog() {
        User administrator = new User();
        administrator.setId(1L);
        administrator.setUsername("admin");
        administrator.setName("管理员");
        administrator.setRole(UserRole.ADMIN);
        administrator.setStatus(UserStatus.ACTIVE);
        Authentication administratorAuthentication = new UsernamePasswordAuthenticationToken(
                AuthenticatedUser.from(administrator), null, AuthenticatedUser.from(administrator).getAuthorities()
        );
        Activity activity = new Activity();
        activity.setId(27L);
        activity.setOrganizerId(7L);
        activity.setTitle("校园讲座");
        activity.setStatus(ActivityStatus.PENDING_REVIEW);
        when(activityMapper.selectById(27L)).thenReturn(activity);

        var result = activityService.approve(27L, administratorAuthentication);

        assertEquals(ActivityStatus.APPROVED, result.status());
        verify(operationLogService).record(1L, "ACTIVITY_APPROVED", "ACTIVITY", 27L);
        verify(notificationService).create(
                7L,
                "ACTIVITY_APPROVED",
                "活动审核已通过",
                "“校园讲座”已通过审核，等待管理员发布。",
                "ACTIVITY",
                27L
        );
    }

    @Test
    void organizerCanManuallyEndOngoingActivity() {
        Activity activity = new Activity();
        activity.setId(24L);
        activity.setOrganizerId(7L);
        activity.setStatus(ActivityStatus.ONGOING);
        activity.setEndTime(LocalDateTime.now().plusHours(2));
        when(activityMapper.selectById(24L)).thenReturn(activity);

        var result = activityService.end(24L, organizerAuthentication);

        assertEquals(ActivityStatus.ENDED, result.status());
        verify(activityMapper).updateById(activity);
        verify(operationLogService).record(7L, "ACTIVITY_MANUALLY_ENDED", "ACTIVITY", 24L);
    }

    @Test
    void adminCanManuallyEndAnotherOrganizersActivity() {
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setName("管理员");
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        Authentication adminAuthentication = new UsernamePasswordAuthenticationToken(
                AuthenticatedUser.from(admin), null, AuthenticatedUser.from(admin).getAuthorities()
        );
        Activity activity = new Activity();
        activity.setId(26L);
        activity.setOrganizerId(99L);
        activity.setStatus(ActivityStatus.ONGOING);
        activity.setEndTime(LocalDateTime.now().plusHours(2));
        when(activityMapper.selectById(26L)).thenReturn(activity);

        var result = activityService.end(26L, adminAuthentication);

        assertEquals(ActivityStatus.ENDED, result.status());
        verify(activityMapper).updateById(activity);
        verify(operationLogService).record(1L, "ACTIVITY_MANUALLY_ENDED", "ACTIVITY", 26L);
    }

    @Test
    void cannotManuallyEndActivityBeforeItStarts() {
        Activity activity = new Activity();
        activity.setId(25L);
        activity.setOrganizerId(7L);
        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setEndTime(LocalDateTime.now().plusHours(2));
        when(activityMapper.selectById(25L)).thenReturn(activity);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> activityService.end(25L, organizerAuthentication)
        );

        assertEquals(409, exception.getCode());
    }

    private ActivityRequest validRequest() {
        LocalDateTime now = LocalDateTime.now();
        return new ActivityRequest(
                "活动", "介绍", null, "地点",
                null, null, null,
                now.plusDays(2),
                now.plusDays(2).plusHours(2),
                now.plusDays(1),
                now.plusDays(1).plusHours(12),
                10, false, null
        );
    }
}
