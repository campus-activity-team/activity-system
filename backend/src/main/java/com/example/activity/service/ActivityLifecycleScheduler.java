package com.example.activity.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.activity.entity.Activity;
import com.example.activity.entity.ActivityStatus;
import com.example.activity.mapper.ActivityMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class ActivityLifecycleScheduler {

    private final ActivityMapper activityMapper;

    public ActivityLifecycleScheduler(ActivityMapper activityMapper) {
        this.activityMapper = activityMapper;
    }

    @Scheduled(fixedDelayString = "${activity.lifecycle.poll-interval-ms:30000}")
    @Transactional
    public void advanceStatuses() {
        LocalDateTime now = LocalDateTime.now();
        activityMapper.selectList(Wrappers.<Activity>lambdaQuery()
                        .eq(Activity::getStatus, ActivityStatus.PUBLISHED)
                        .le(Activity::getEndTime, now))
                .forEach(activity -> updateStatus(activity, ActivityStatus.ENDED));
        activityMapper.selectList(Wrappers.<Activity>lambdaQuery()
                        .eq(Activity::getStatus, ActivityStatus.PUBLISHED)
                        .le(Activity::getStartTime, now)
                        .gt(Activity::getEndTime, now))
                .forEach(activity -> updateStatus(activity, ActivityStatus.ONGOING));
        activityMapper.selectList(Wrappers.<Activity>lambdaQuery()
                        .eq(Activity::getStatus, ActivityStatus.ONGOING)
                        .le(Activity::getEndTime, now))
                .forEach(activity -> updateStatus(activity, ActivityStatus.ENDED));
    }

    private void updateStatus(Activity activity, ActivityStatus status) {
        activity.setStatus(status);
        activityMapper.updateById(activity);
    }
}
