package com.example.activity.service;

import com.example.activity.entity.Notification;
import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.entity.UserStatus;
import com.example.activity.exception.BusinessException;
import com.example.activity.mapper.NotificationMapper;
import com.example.activity.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private NotificationMapper notificationMapper;
    private NotificationService notificationService;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        notificationMapper = mock(NotificationMapper.class);
        notificationService = new NotificationService(notificationMapper);
        User user = new User();
        user.setId(8L);
        user.setUsername("student");
        user.setName("学生");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(user);
        authentication = new UsernamePasswordAuthenticationToken(
                authenticatedUser, null, authenticatedUser.getAuthorities()
        );
    }

    @Test
    void marksOwnedNotificationAsRead() {
        Notification notification = new Notification();
        notification.setId(10L);
        notification.setUserId(8L);
        when(notificationMapper.selectById(10L)).thenReturn(notification);

        var result = notificationService.markRead(10L, authentication);

        assertEquals(true, result.read());
        verify(notificationMapper).updateById(notification);
    }

    @Test
    void hidesOtherUsersNotification() {
        Notification notification = new Notification();
        notification.setId(11L);
        notification.setUserId(9L);
        when(notificationMapper.selectById(11L)).thenReturn(notification);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> notificationService.markRead(11L, authentication)
        );

        assertEquals(404, exception.getCode());
    }

    @Test
    void ignoresDuplicateScheduledReminder() {
        doThrow(new DuplicateKeyException("duplicate"))
                .when(notificationMapper).insert(any(Notification.class));

        notificationService.createDeduplicated(
                8L,
                "ACTIVITY_START_REMINDER",
                "活动即将开始",
                "提醒内容",
                "ACTIVITY",
                20L,
                "activity-start:20:8"
        );
    }
}
