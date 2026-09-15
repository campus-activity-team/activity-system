package com.example.activity.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.activity.entity.Activity;
import com.example.activity.entity.ActivityStatus;
import com.example.activity.entity.Registration;
import com.example.activity.entity.RegistrationStatus;
import com.example.activity.mapper.ActivityMapper;
import com.example.activity.mapper.RegistrationMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class NotificationReminderScheduler {

    private static final DateTimeFormatter REMINDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM月dd日 HH:mm");

    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final NotificationService notificationService;

    public NotificationReminderScheduler(
            ActivityMapper activityMapper,
            RegistrationMapper registrationMapper,
            NotificationService notificationService
    ) {
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "${notification.reminder.poll-interval-ms:60000}")
    @Transactional
    public void sendUpcomingActivityReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderWindowEnd = now.plusHours(24);
        activityMapper.selectList(Wrappers.<Activity>lambdaQuery()
                        .eq(Activity::getStatus, ActivityStatus.PUBLISHED)
                        .gt(Activity::getStartTime, now)
                        .le(Activity::getStartTime, reminderWindowEnd))
                .forEach(this::notifyRegisteredUsers);
    }

    private void notifyRegisteredUsers(Activity activity) {
        registrationMapper.selectList(Wrappers.<Registration>lambdaQuery()
                        .eq(Registration::getActivityId, activity.getId())
                        .eq(Registration::getStatus, RegistrationStatus.REGISTERED))
                .forEach(registration -> notificationService.createDeduplicated(
                        registration.getUserId(),
                        "ACTIVITY_START_REMINDER",
                        "活动即将开始",
                        "你报名的“" + activity.getTitle() + "”将在"
                                + REMINDER_TIME_FORMATTER.format(activity.getStartTime())
                                + "开始，地点：" + activity.getLocation() + "。",
                        "ACTIVITY",
                        activity.getId(),
                        "activity-start:" + activity.getId() + ":" + registration.getUserId()
                ));
    }
}
