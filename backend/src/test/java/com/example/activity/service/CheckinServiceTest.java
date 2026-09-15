package com.example.activity.service;

import com.example.activity.dto.CheckinRequest;
import com.example.activity.entity.Activity;
import com.example.activity.entity.ActivityStatus;
import com.example.activity.entity.Attendance;
import com.example.activity.entity.AttendanceStatus;
import com.example.activity.entity.CheckinAnomalyReason;
import com.example.activity.entity.CheckinMethod;
import com.example.activity.entity.CheckinToken;
import com.example.activity.entity.Feedback;
import com.example.activity.entity.Registration;
import com.example.activity.entity.RegistrationStatus;
import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.entity.UserStatus;
import com.example.activity.exception.BusinessException;
import com.example.activity.mapper.ActivityMapper;
import com.example.activity.mapper.AttendanceMapper;
import com.example.activity.mapper.CheckinTokenMapper;
import com.example.activity.mapper.FeedbackMapper;
import com.example.activity.mapper.RegistrationMapper;
import com.example.activity.mapper.UserMapper;
import com.example.activity.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckinServiceTest {

    private static final String TOKEN = "valid-token";

    private ActivityMapper activityMapper;
    private CheckinTokenMapper checkinTokenMapper;
    private RegistrationMapper registrationMapper;
    private AttendanceMapper attendanceMapper;
    private CheckinAnomalyService checkinAnomalyService;
    private FeedbackMapper feedbackMapper;
    private OperationLogService operationLogService;
    private UserMapper userMapper;
    private CheckinService checkinService;
    private Authentication participantAuthentication;
    private User participant;

    @BeforeEach
    void setUp() {
        activityMapper = mock(ActivityMapper.class);
        checkinTokenMapper = mock(CheckinTokenMapper.class);
        registrationMapper = mock(RegistrationMapper.class);
        attendanceMapper = mock(AttendanceMapper.class);
        userMapper = mock(UserMapper.class);
        checkinAnomalyService = mock(CheckinAnomalyService.class);
        feedbackMapper = mock(FeedbackMapper.class);
        operationLogService = mock(OperationLogService.class);
        checkinService = new CheckinService(
                activityMapper,
                checkinTokenMapper,
                registrationMapper,
                attendanceMapper,
                userMapper,
                checkinAnomalyService,
                feedbackMapper,
                operationLogService
        );

        participant = new User();
        participant.setId(8L);
        participant.setUsername("participant");
        participant.setName("参与者");
        participant.setRole(UserRole.USER);
        participant.setStatus(UserStatus.ACTIVE);
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(participant);
        participantAuthentication = new UsernamePasswordAuthenticationToken(
                authenticatedUser, null, authenticatedUser.getAuthorities()
        );

        CheckinToken token = new CheckinToken();
        token.setToken(TOKEN);
        token.setActivityId(11L);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(1));
        when(checkinTokenMapper.selectByToken(TOKEN)).thenReturn(token);

        Registration registration = new Registration();
        registration.setId(17L);
        registration.setActivityId(11L);
        registration.setUserId(8L);
        registration.setStatus(RegistrationStatus.REGISTERED);
        when(registrationMapper.selectByActivityAndUser(11L, 8L)).thenReturn(registration);
        when(attendanceMapper.selectByActivityAndUser(11L, 8L)).thenReturn(null);
    }

    @Test
    void rejectsLocationCheckinWithoutCoordinates() {
        when(activityMapper.selectById(11L)).thenReturn(locationRestrictedActivity());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> checkinService.checkin(new CheckinRequest(TOKEN, null, null), participantAuthentication)
        );

        assertEquals(409, exception.getCode());
        verify(attendanceMapper, never()).insert(any(Attendance.class));
        verify(checkinAnomalyService).recordFailure(
                11L,
                participant,
                CheckinAnomalyReason.LOCATION_REQUIRED,
                "该活动需要开启手机定位后才能签到",
                null,
                null,
                null
        );
    }

    @Test
    void acceptsLocationCheckinInsideRadius() {
        when(activityMapper.selectById(11L)).thenReturn(locationRestrictedActivity());

        var result = checkinService.checkin(
                new CheckinRequest(TOKEN, 31.2305, 121.4738),
                participantAuthentication
        );

        assertEquals(11L, result.activityId());
        assertEquals(8L, result.userId());
        verify(attendanceMapper).insert(any(Attendance.class));
        verify(checkinAnomalyService, never()).recordFailure(
                any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void rejectsLocationCheckinOutsideRadius() {
        when(activityMapper.selectById(11L)).thenReturn(locationRestrictedActivity());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> checkinService.checkin(
                        new CheckinRequest(TOKEN, 31.2404, 121.4737),
                        participantAuthentication
                )
        );

        assertEquals(409, exception.getCode());
        verify(attendanceMapper, never()).insert(any(Attendance.class));
        verify(checkinAnomalyService).recordFailure(
                org.mockito.ArgumentMatchers.eq(11L),
                org.mockito.ArgumentMatchers.eq(participant),
                org.mockito.ArgumentMatchers.eq(CheckinAnomalyReason.OUTSIDE_GEOFENCE),
                any(),
                org.mockito.ArgumentMatchers.eq(31.2404),
                org.mockito.ArgumentMatchers.eq(121.4737),
                anyInt()
        );
    }

    @Test
    void recordsExpiredTokenAttempt() {
        CheckinToken expiredToken = new CheckinToken();
        expiredToken.setToken(TOKEN);
        expiredToken.setActivityId(11L);
        expiredToken.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(checkinTokenMapper.selectByToken(TOKEN)).thenReturn(expiredToken);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> checkinService.checkin(new CheckinRequest(TOKEN, null, null), participantAuthentication)
        );

        assertEquals(409, exception.getCode());
        verify(checkinAnomalyService).recordFailure(
                11L,
                participant,
                CheckinAnomalyReason.EXPIRED_TOKEN,
                "签到二维码已过期，请扫描最新二维码",
                null,
                null,
                null
        );
    }

    @Test
    void recordsAttemptOutsideCheckinTime() {
        Activity activity = locationRestrictedActivity();
        activity.setStatus(ActivityStatus.ENDED);
        activity.setEndTime(LocalDateTime.now().minusMinutes(1));
        when(activityMapper.selectById(11L)).thenReturn(activity);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> checkinService.checkin(new CheckinRequest(TOKEN, null, null), participantAuthentication)
        );

        assertEquals(409, exception.getCode());
        verify(checkinAnomalyService).recordFailure(
                11L,
                participant,
                CheckinAnomalyReason.OUTSIDE_CHECKIN_TIME,
                "当前不在活动签到时间内",
                null,
                null,
                null
        );
    }

    @Test
    void recordsAttemptByUnregisteredUser() {
        when(activityMapper.selectById(11L)).thenReturn(locationRestrictedActivity());
        when(registrationMapper.selectByActivityAndUser(11L, 8L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> checkinService.checkin(new CheckinRequest(TOKEN, null, null), participantAuthentication)
        );

        assertEquals(409, exception.getCode());
        verify(checkinAnomalyService).recordFailure(
                11L,
                participant,
                CheckinAnomalyReason.NOT_REGISTERED,
                "请先报名后再签到",
                null,
                null,
                null
        );
    }

    @Test
    void organizerCanManuallyCheckInRegisteredParticipant() {
        Activity activity = locationRestrictedActivity();
        activity.setOrganizerId(7L);
        when(activityMapper.selectById(11L)).thenReturn(activity);
        when(userMapper.selectById(8L)).thenReturn(participant);
        when(attendanceMapper.insert(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance attendance = invocation.getArgument(0);
            attendance.setId(30L);
            return 1;
        });

        var result = checkinService.manualCheckin(11L, 8L, organizerAuthentication());

        assertEquals(CheckinMethod.MANUAL.name(), result.checkinMethod());
        verify(operationLogService).record(7L, "ATTENDANCE_MANUAL_CHECKIN", "ATTENDANCE", 30L);
    }

    @Test
    void organizerCanCancelAttendanceWithoutFeedback() {
        Activity activity = locationRestrictedActivity();
        activity.setOrganizerId(7L);
        Attendance attendance = new Attendance();
        attendance.setId(30L);
        attendance.setActivityId(11L);
        attendance.setUserId(8L);
        attendance.setStatus(AttendanceStatus.SUCCESS);
        when(activityMapper.selectById(11L)).thenReturn(activity);
        when(attendanceMapper.selectByActivityAndUser(11L, 8L)).thenReturn(attendance);

        checkinService.cancelAttendance(11L, 8L, organizerAuthentication());

        assertEquals(AttendanceStatus.CANCELLED, attendance.getStatus());
        verify(attendanceMapper).updateById(attendance);
        verify(operationLogService).record(7L, "ATTENDANCE_CANCELLED", "ATTENDANCE", 30L);
    }

    @Test
    void cannotCancelAttendanceAfterParticipantSubmittedFeedback() {
        Activity activity = locationRestrictedActivity();
        activity.setOrganizerId(7L);
        Attendance attendance = new Attendance();
        attendance.setId(30L);
        attendance.setStatus(AttendanceStatus.SUCCESS);
        when(activityMapper.selectById(11L)).thenReturn(activity);
        when(attendanceMapper.selectByActivityAndUser(11L, 8L)).thenReturn(attendance);
        when(feedbackMapper.selectByActivityAndUser(11L, 8L)).thenReturn(new Feedback());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> checkinService.cancelAttendance(11L, 8L, organizerAuthentication())
        );

        assertEquals(409, exception.getCode());
        verify(attendanceMapper, never()).updateById(attendance);
    }

    private Activity locationRestrictedActivity() {
        Activity activity = new Activity();
        activity.setId(11L);
        activity.setTitle("定位签到活动");
        activity.setStatus(ActivityStatus.ONGOING);
        activity.setEndTime(LocalDateTime.now().plusHours(2));
        activity.setCheckinLatitude(31.2304);
        activity.setCheckinLongitude(121.4737);
        activity.setCheckinRadiusMeters(100);
        return activity;
    }

    private Authentication organizerAuthentication() {
        User organizer = new User();
        organizer.setId(7L);
        organizer.setUsername("organizer");
        organizer.setName("组织者");
        organizer.setRole(UserRole.ORGANIZER);
        organizer.setStatus(UserStatus.ACTIVE);
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(organizer);
        return new UsernamePasswordAuthenticationToken(
                authenticatedUser, null, authenticatedUser.getAuthorities()
        );
    }
}
