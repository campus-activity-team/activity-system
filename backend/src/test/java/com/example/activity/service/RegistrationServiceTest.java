package com.example.activity.service;

import com.example.activity.entity.Activity;
import com.example.activity.entity.ActivityStatus;
import com.example.activity.entity.Attendance;
import com.example.activity.entity.AttendanceStatus;
import com.example.activity.entity.CheckinMethod;
import com.example.activity.entity.Registration;
import com.example.activity.entity.RegistrationStatus;
import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.entity.UserStatus;
import com.example.activity.exception.BusinessException;
import com.example.activity.mapper.ActivityMapper;
import com.example.activity.mapper.AttendanceMapper;
import com.example.activity.mapper.RegistrationMapper;
import com.example.activity.mapper.UserMapper;
import com.example.activity.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationServiceTest {

    private ActivityMapper activityMapper;
    private RegistrationMapper registrationMapper;
    private AttendanceMapper attendanceMapper;
    private UserMapper userMapper;
    private RegistrationService registrationService;
    private Activity activity;

    @BeforeEach
    void setUp() {
        activityMapper = mock(ActivityMapper.class);
        registrationMapper = mock(RegistrationMapper.class);
        attendanceMapper = mock(AttendanceMapper.class);
        userMapper = mock(UserMapper.class);
        registrationService = new RegistrationService(
                activityMapper,
                registrationMapper,
                attendanceMapper,
                userMapper
        );

        activity = new Activity();
        activity.setId(11L);
        activity.setTitle("活动");
        activity.setOrganizerId(7L);
        activity.setStatus(ActivityStatus.ENDED);
        activity.setEndTime(LocalDateTime.now().minusHours(1));
        when(activityMapper.selectById(11L)).thenReturn(activity);
    }

    @Test
    void exportsUtf8RosterAndEscapesSpreadsheetFormula() {
        Registration registration = new Registration();
        registration.setId(20L);
        registration.setActivityId(11L);
        registration.setUserId(8L);
        registration.setStatus(RegistrationStatus.REGISTERED);
        registration.setRegisteredAt(LocalDateTime.of(2026, 9, 15, 10, 30));
        User participant = user(8L, UserRole.USER);
        participant.setName("=HYPERLINK(\"https://example.com\")");
        Attendance attendance = new Attendance();
        attendance.setActivityId(11L);
        attendance.setUserId(8L);
        attendance.setStatus(AttendanceStatus.SUCCESS);
        attendance.setCheckinMethod(CheckinMethod.MANUAL);
        attendance.setCheckinTime(LocalDateTime.of(2026, 9, 15, 11, 0));
        when(registrationMapper.selectByActivityId(11L)).thenReturn(List.of(registration));
        when(userMapper.selectById(8L)).thenReturn(participant);
        when(attendanceMapper.selectByActivityAndUser(11L, 8L)).thenReturn(attendance);

        String csv = new String(registrationService.exportForActivity(11L, authentication(user(7L, UserRole.ORGANIZER))), StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("\uFEFF姓名,用户名"));
        assertTrue(csv.contains("\"'=HYPERLINK(\"\"https://example.com\"\")\""));
        assertTrue(csv.contains("\"手动补签\""));
    }

    @Test
    void otherOrganizerCannotExportRoster() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> registrationService.exportForActivity(11L, authentication(user(9L, UserRole.ORGANIZER)))
        );

        assertEquals(403, exception.getCode());
        verify(registrationMapper, never()).selectByActivityId(11L);
    }

    private User user(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setName("用户" + id);
        user.setStudentId("STU-" + id);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Authentication authentication(User user) {
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(user);
        return new UsernamePasswordAuthenticationToken(
                authenticatedUser, null, authenticatedUser.getAuthorities()
        );
    }
}
