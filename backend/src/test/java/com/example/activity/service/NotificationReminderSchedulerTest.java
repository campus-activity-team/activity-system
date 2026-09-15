package com.example.activity.service;

import com.example.activity.entity.Activity;
import com.example.activity.entity.Registration;
import com.example.activity.entity.RegistrationStatus;
import com.example.activity.mapper.ActivityMapper;
import com.example.activity.mapper.RegistrationMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationReminderSchedulerTest {

    @Test
    void createsDeduplicatedReminderForRegisteredParticipant() {
        ActivityMapper activityMapper = mock(ActivityMapper.class);
        RegistrationMapper registrationMapper = mock(RegistrationMapper.class);
        NotificationService notificationService = mock(NotificationService.class);
        NotificationReminderScheduler scheduler = new NotificationReminderScheduler(
                activityMapper,
                registrationMapper,
                notificationService
        );
        Activity activity = new Activity();
        activity.setId(20L);
        activity.setTitle("校园讲座");
        activity.setLocation("报告厅");
        activity.setStartTime(LocalDateTime.of(2026, 9, 16, 19, 0));
        Registration registration = new Registration();
        registration.setActivityId(20L);
        registration.setUserId(8L);
        registration.setStatus(RegistrationStatus.REGISTERED);
        when(activityMapper.selectList(any())).thenReturn(List.of(activity));
        when(registrationMapper.selectList(any())).thenReturn(List.of(registration));

        scheduler.sendUpcomingActivityReminders();

        verify(notificationService).createDeduplicated(
                8L,
                "ACTIVITY_START_REMINDER",
                "活动即将开始",
                "你报名的“校园讲座”将在09月16日 19:00开始，地点：报告厅。",
                "ACTIVITY",
                20L,
                "activity-start:20:8"
        );
    }
}
