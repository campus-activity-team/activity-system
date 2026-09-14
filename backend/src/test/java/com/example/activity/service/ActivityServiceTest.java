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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityServiceTest {

    private ActivityMapper activityMapper;
    private ActivityService activityService;
    private Authentication organizerAuthentication;

    @BeforeEach
    void setUp() {
        activityMapper = mock(ActivityMapper.class);
        activityService = new ActivityService(activityMapper);
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
    }

    private ActivityRequest validRequest() {
        LocalDateTime now = LocalDateTime.now();
        return new ActivityRequest(
                "活动", "介绍", null, "地点",
                now.plusDays(2),
                now.plusDays(2).plusHours(2),
                now.plusDays(1),
                now.plusDays(1).plusHours(12),
                10, false, null
        );
    }
}
