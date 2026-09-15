package com.example.activity.service;

import com.example.activity.entity.Activity;
import com.example.activity.entity.CheckinAnomaly;
import com.example.activity.entity.CheckinAnomalyReason;
import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.entity.UserStatus;
import com.example.activity.exception.BusinessException;
import com.example.activity.mapper.ActivityMapper;
import com.example.activity.mapper.CheckinAnomalyMapper;
import com.example.activity.mapper.UserMapper;
import com.example.activity.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckinAnomalyServiceTest {

    private CheckinAnomalyMapper checkinAnomalyMapper;
    private ActivityMapper activityMapper;
    private CheckinAnomalyService checkinAnomalyService;
    private User organizer;
    private Authentication organizerAuthentication;

    @BeforeEach
    void setUp() {
        checkinAnomalyMapper = mock(CheckinAnomalyMapper.class);
        activityMapper = mock(ActivityMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        checkinAnomalyService = new CheckinAnomalyService(checkinAnomalyMapper, activityMapper, userMapper);

        organizer = new User();
        organizer.setId(7L);
        organizer.setUsername("organizer");
        organizer.setName("组织者");
        organizer.setRole(UserRole.ORGANIZER);
        organizer.setStatus(UserStatus.ACTIVE);
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(organizer);
        organizerAuthentication = new UsernamePasswordAuthenticationToken(
                authenticatedUser, null, authenticatedUser.getAuthorities()
        );
    }

    @Test
    void recordsFailureContext() {
        checkinAnomalyService.recordFailure(
                11L,
                organizer,
                CheckinAnomalyReason.OUTSIDE_GEOFENCE,
                "超出签到范围",
                31.2404,
                121.4737,
                1112
        );

        verify(checkinAnomalyMapper).insert(any(CheckinAnomaly.class));
    }

    @Test
    void rejectsOtherOrganizerFromViewingAnomalies() {
        Activity activity = new Activity();
        activity.setId(11L);
        activity.setOrganizerId(99L);
        when(activityMapper.selectById(11L)).thenReturn(activity);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> checkinAnomalyService.listForActivity(11L, organizerAuthentication)
        );

        assertEquals(403, exception.getCode());
    }
}
