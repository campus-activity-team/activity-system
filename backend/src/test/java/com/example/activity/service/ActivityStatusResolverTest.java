package com.example.activity.service;

import com.example.activity.entity.Activity;
import com.example.activity.entity.ActivityStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActivityStatusResolverTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 9, 14, 12, 0);

    @Test
    void marksPublishedActivityEndedAfterEndTime() {
        Activity activity = activity(ActivityStatus.PUBLISHED, now.minusHours(2), now.minusMinutes(1));

        assertEquals(ActivityStatus.ENDED, ActivityStatusResolver.resolve(activity, now));
    }

    @Test
    void startsPublishedActivityAtScheduledStartTime() {
        Activity activity = activity(ActivityStatus.PUBLISHED, now.minusMinutes(1), now.plusHours(1));

        assertEquals(ActivityStatus.ONGOING, ActivityStatusResolver.resolve(activity, now));
    }

    @Test
    void keepsManuallyStartedActivityOngoingBeforeScheduledStart() {
        Activity activity = activity(ActivityStatus.ONGOING, now.plusHours(1), now.plusHours(2));

        assertEquals(ActivityStatus.ONGOING, ActivityStatusResolver.resolve(activity, now));
    }

    private Activity activity(ActivityStatus status, LocalDateTime start, LocalDateTime end) {
        Activity activity = new Activity();
        activity.setStatus(status);
        activity.setStartTime(start);
        activity.setEndTime(end);
        return activity;
    }
}
