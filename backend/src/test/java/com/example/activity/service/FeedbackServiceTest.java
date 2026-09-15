package com.example.activity.service;

import com.example.activity.dto.FeedbackRequest;
import com.example.activity.entity.Activity;
import com.example.activity.entity.ActivityStatus;
import com.example.activity.entity.Attendance;
import com.example.activity.entity.AttendanceStatus;
import com.example.activity.entity.Feedback;
import com.example.activity.entity.Registration;
import com.example.activity.entity.RegistrationStatus;
import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.entity.UserStatus;
import com.example.activity.exception.BusinessException;
import com.example.activity.mapper.ActivityMapper;
import com.example.activity.mapper.AttendanceMapper;
import com.example.activity.mapper.FeedbackMapper;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackServiceTest {

    private ActivityMapper activityMapper;
    private RegistrationMapper registrationMapper;
    private AttendanceMapper attendanceMapper;
    private FeedbackMapper feedbackMapper;
    private UserMapper userMapper;
    private NotificationService notificationService;
    private OperationLogService operationLogService;
    private FeedbackService feedbackService;
    private Activity activity;
    private User participant;
    private Authentication participantAuthentication;

    @BeforeEach
    void setUp() {
        activityMapper = mock(ActivityMapper.class);
        registrationMapper = mock(RegistrationMapper.class);
        attendanceMapper = mock(AttendanceMapper.class);
        feedbackMapper = mock(FeedbackMapper.class);
        userMapper = mock(UserMapper.class);
        notificationService = mock(NotificationService.class);
        operationLogService = mock(OperationLogService.class);
        feedbackService = new FeedbackService(
                activityMapper,
                registrationMapper,
                attendanceMapper,
                feedbackMapper,
                userMapper,
                notificationService,
                operationLogService
        );
        when(userMapper.selectList(any())).thenReturn(List.of());

        activity = new Activity();
        activity.setId(11L);
        activity.setTitle("往期活动");
        activity.setOrganizerId(7L);
        activity.setStatus(ActivityStatus.ENDED);
        activity.setEndTime(LocalDateTime.now().minusDays(1));
        activity.setRequireFeedback(true);
        activity.setFeedbackDeadline(LocalDateTime.now().plusDays(7));
        activity.setCurrentRegisteredCount(2);
        when(activityMapper.selectById(11L)).thenReturn(activity);

        participant = user(8L, UserRole.USER);
        participantAuthentication = authentication(participant);

        Registration registration = new Registration();
        registration.setActivityId(11L);
        registration.setUserId(8L);
        registration.setStatus(RegistrationStatus.REGISTERED);
        when(registrationMapper.selectByActivityAndUser(11L, 8L)).thenReturn(registration);

        Attendance attendance = new Attendance();
        attendance.setActivityId(11L);
        attendance.setUserId(8L);
        attendance.setStatus(AttendanceStatus.SUCCESS);
        when(attendanceMapper.selectByActivityAndUser(11L, 8L)).thenReturn(attendance);
    }

    @Test
    void checkedInParticipantCanSubmitFeedback() {
        when(feedbackMapper.insert(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback feedback = invocation.getArgument(0);
            feedback.setId(20L);
            return 1;
        });

        var result = feedbackService.submit(11L, request(), participantAuthentication);

        assertEquals(20L, result.id());
        assertEquals(5, result.overallRating());
        verify(feedbackMapper).insert(any(Feedback.class));
    }

    @Test
    void notifiesOrganizerAndActiveAdminsAfterFeedbackSubmission() {
        when(userMapper.selectList(any())).thenReturn(List.of(user(1L, UserRole.ADMIN)));
        when(feedbackMapper.insert(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback feedback = invocation.getArgument(0);
            feedback.setId(20L);
            return 1;
        });

        feedbackService.submit(11L, request(), participantAuthentication);

        verify(notificationService).create(
                7L,
                "ACTIVITY_FEEDBACK_SUBMITTED",
                "收到活动反馈",
                "“往期活动”收到一份新的匿名反馈，总体评分 5/5。请在活动管理中查看最新统计。",
                "ACTIVITY",
                11L
        );
        verify(notificationService).create(
                1L,
                "ACTIVITY_FEEDBACK_SUBMITTED",
                "收到活动反馈",
                "“往期活动”收到一份新的匿名反馈，总体评分 5/5。请在活动管理中查看最新统计。",
                "ACTIVITY",
                11L
        );
        verify(operationLogService).record(8L, "ACTIVITY_FEEDBACK_SUBMITTED", "ACTIVITY", 11L);
    }

    @Test
    void repeatedSubmissionUpdatesExistingFeedback() {
        Feedback existing = feedback(20L, 3, 4, 3);
        when(feedbackMapper.selectByActivityAndUser(11L, 8L)).thenReturn(existing);

        var result = feedbackService.submit(11L, request(), participantAuthentication);

        assertEquals(5, result.overallRating());
        verify(feedbackMapper).updateById(existing);
        verify(feedbackMapper, never()).insert(any(Feedback.class));
    }

    @Test
    void rejectsParticipantWithoutSuccessfulAttendance() {
        when(attendanceMapper.selectByActivityAndUser(11L, 8L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> feedbackService.submit(11L, request(), participantAuthentication)
        );

        assertEquals(409, exception.getCode());
        assertEquals("只有已签到的参与者才能提交反馈", exception.getMessage());
    }

    @Test
    void rejectsSubmissionAfterDeadline() {
        activity.setFeedbackDeadline(LocalDateTime.now().minusMinutes(1));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> feedbackService.submit(11L, request(), participantAuthentication)
        );

        assertEquals(409, exception.getCode());
        assertEquals("反馈提交已截止", exception.getMessage());
    }

    @Test
    void statusIncludesExistingFeedbackWhenEditingIsClosed() {
        activity.setFeedbackDeadline(LocalDateTime.now().minusMinutes(1));
        when(feedbackMapper.selectByActivityAndUser(11L, 8L)).thenReturn(feedback(20L, 4, 5, 4));

        var result = feedbackService.mine(11L, participantAuthentication);

        assertFalse(result.canSubmit());
        assertEquals("反馈提交已截止", result.unavailableReason());
        assertEquals(20L, result.feedback().id());
    }

    @Test
    void organizerDashboardCalculatesAnonymousStatistics() {
        Feedback first = feedback(20L, 5, 4, 5);
        Feedback second = feedback(21L, 4, 5, 3);
        when(feedbackMapper.selectByActivityId(11L)).thenReturn(List.of(first, second));
        when(attendanceMapper.countSuccessfulByActivityId(11L)).thenReturn(4L);

        var result = feedbackService.dashboard(11L, authentication(user(7L, UserRole.ORGANIZER)));

        assertEquals(2, result.feedbackCount());
        assertEquals(50.0, result.responseRate());
        assertEquals(4.5, result.averageOverallRating());
        assertEquals(1L, result.overallRatingDistribution().get(4));
        assertEquals(1L, result.overallRatingDistribution().get(5));
    }

    @Test
    void otherOrganizerCannotViewDashboard() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> feedbackService.dashboard(11L, authentication(user(9L, UserRole.ORGANIZER)))
        );

        assertEquals(403, exception.getCode());
        verify(feedbackMapper, never()).selectByActivityId(11L);
    }

    private FeedbackRequest request() {
        return new FeedbackRequest(5, 4, 5, "内容很实用");
    }

    private Feedback feedback(Long id, int overall, int content, int service) {
        Feedback feedback = new Feedback();
        feedback.setId(id);
        feedback.setActivityId(11L);
        feedback.setUserId(8L);
        feedback.setOverallRating(overall);
        feedback.setContentRating(content);
        feedback.setServiceRating(service);
        feedback.setComment("匿名意见");
        return feedback;
    }

    private User user(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setName("用户" + id);
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
