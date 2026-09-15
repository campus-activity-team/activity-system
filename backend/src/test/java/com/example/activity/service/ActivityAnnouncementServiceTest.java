package com.example.activity.service;

import com.example.activity.dto.ActivityAnnouncementRequest;
import com.example.activity.entity.Activity;
import com.example.activity.entity.ActivityAnnouncement;
import com.example.activity.entity.ActivityStatus;
import com.example.activity.entity.Registration;
import com.example.activity.entity.RegistrationStatus;
import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.entity.UserStatus;
import com.example.activity.exception.BusinessException;
import com.example.activity.mapper.ActivityAnnouncementMapper;
import com.example.activity.mapper.ActivityMapper;
import com.example.activity.mapper.RegistrationMapper;
import com.example.activity.mapper.UserMapper;
import com.example.activity.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityAnnouncementServiceTest {

    private ActivityAnnouncementMapper announcementMapper;
    private ActivityMapper activityMapper;
    private RegistrationMapper registrationMapper;
    private UserMapper userMapper;
    private NotificationService notificationService;
    private OperationLogService operationLogService;
    private ActivityAnnouncementService announcementService;
    private Authentication organizerAuthentication;

    @BeforeEach
    void setUp() {
        announcementMapper = mock(ActivityAnnouncementMapper.class);
        activityMapper = mock(ActivityMapper.class);
        registrationMapper = mock(RegistrationMapper.class);
        userMapper = mock(UserMapper.class);
        notificationService = mock(NotificationService.class);
        operationLogService = mock(OperationLogService.class);
        announcementService = new ActivityAnnouncementService(
                announcementMapper,
                activityMapper,
                registrationMapper,
                userMapper,
                notificationService,
                operationLogService
        );
        organizerAuthentication = authentication(user(7L, "组织者", UserRole.ORGANIZER));
    }

    @Test
    void publishesAnnouncementAndNotifiesRegisteredUsers() {
        Activity activity = activity(11L, 7L, ActivityStatus.PUBLISHED);
        when(activityMapper.selectById(11L)).thenReturn(activity);
        when(registrationMapper.selectByActivityId(11L)).thenReturn(List.of(
                registration(8L, RegistrationStatus.REGISTERED),
                registration(9L, RegistrationStatus.CANCELLED)
        ));
        when(announcementMapper.insert(any(ActivityAnnouncement.class))).thenAnswer(invocation -> {
            ActivityAnnouncement announcement = invocation.getArgument(0);
            announcement.setId(20L);
            return 1;
        });

        var result = announcementService.publish(
                11L,
                new ActivityAnnouncementRequest("  临时调整  ", "  请改到东门集合  "),
                organizerAuthentication
        );

        assertEquals(20L, result.id());
        assertEquals("临时调整", result.title());
        assertEquals("请改到东门集合", result.content());
        verify(notificationService).createDeduplicated(
                8L,
                "ACTIVITY_ANNOUNCEMENT",
                "活动公告：临时调整",
                "“活动”发布了新公告：\n请改到东门集合",
                "ACTIVITY",
                11L,
                "activity-announcement:20:8"
        );
        verify(operationLogService).record(7L, "ACTIVITY_ANNOUNCEMENT_SENT", "ACTIVITY", 11L);
    }

    @Test
    void rejectsAnnouncementForEndedActivity() {
        Activity activity = activity(12L, 7L, ActivityStatus.ENDED);
        when(activityMapper.selectById(12L)).thenReturn(activity);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> announcementService.publish(
                        12L,
                        new ActivityAnnouncementRequest("公告", "内容"),
                        organizerAuthentication
                )
        );

        assertEquals(409, exception.getCode());
    }

    @Test
    void rejectsAnnouncementFromAnotherOrganizer() {
        Activity activity = activity(13L, 99L, ActivityStatus.PUBLISHED);
        when(activityMapper.selectById(13L)).thenReturn(activity);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> announcementService.publish(
                        13L,
                        new ActivityAnnouncementRequest("公告", "内容"),
                        organizerAuthentication
                )
        );

        assertEquals(403, exception.getCode());
    }

    private Activity activity(Long id, Long organizerId, ActivityStatus status) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setOrganizerId(organizerId);
        activity.setTitle("活动");
        activity.setStatus(status);
        activity.setStartTime(LocalDateTime.now().minusHours(1));
        activity.setEndTime(LocalDateTime.now().plusHours(1));
        return activity;
    }

    private Registration registration(Long userId, RegistrationStatus status) {
        Registration registration = new Registration();
        registration.setUserId(userId);
        registration.setStatus(status);
        return registration;
    }

    private User user(Long id, String name, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setUsername(name);
        user.setName(name);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Authentication authentication(User user) {
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(user);
        return new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                authenticatedUser.getAuthorities()
        );
    }
}
