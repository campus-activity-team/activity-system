package com.example.activity.service;

import com.example.activity.entity.Activity;
import com.example.activity.entity.ActivityStatus;

import java.time.LocalDateTime;

public final class ActivityStatusResolver {

    private ActivityStatusResolver() {
    }

    public static ActivityStatus resolve(Activity activity, LocalDateTime now) {
        ActivityStatus status = activity.getStatus();
        if (status == ActivityStatus.PUBLISHED) {
            if (activity.getEndTime() != null && !activity.getEndTime().isAfter(now)) {
                return ActivityStatus.ENDED;
            }
            if (activity.getStartTime() != null && !activity.getStartTime().isAfter(now)) {
                return ActivityStatus.ONGOING;
            }
        } else if (status == ActivityStatus.ONGOING
                && activity.getEndTime() != null
                && !activity.getEndTime().isAfter(now)) {
            return ActivityStatus.ENDED;
        }
        return status;
    }
}
