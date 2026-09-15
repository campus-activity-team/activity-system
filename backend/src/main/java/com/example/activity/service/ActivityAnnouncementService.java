package com.example.activity.service;

import com.example.activity.dto.ActivityAnnouncementRequest;
import com.example.activity.entity.Activity;
import com.example.activity.entity.ActivityAnnouncement;
import com.example.activity.entity.ActivityStatus;
import com.example.activity.entity.Registration;
import com.example.activity.entity.RegistrationStatus;
import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.exception.BusinessException;
import com.example.activity.mapper.ActivityAnnouncementMapper;
import com.example.activity.mapper.ActivityMapper;
import com.example.activity.mapper.RegistrationMapper;
import com.example.activity.mapper.UserMapper;
import com.example.activity.security.AuthenticatedUser;
import com.example.activity.vo.ActivityAnnouncementView;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ActivityAnnouncementService {

    private final ActivityAnnouncementMapper announcementMapper;
    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;

    public ActivityAnnouncementService(
            ActivityAnnouncementMapper announcementMapper,
            ActivityMapper activityMapper,
            RegistrationMapper registrationMapper,
            UserMapper userMapper,
            NotificationService notificationService,
            OperationLogService operationLogService
    ) {
        this.announcementMapper = announcementMapper;
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public List<ActivityAnnouncementView> list(Long activityId, Authentication authentication) {
        Activity activity = requiredActivity(activityId);
        ensureCanManage(activity, authentication);
        return announcementMapper.selectByActivityId(activityId).stream()
                .map(announcement -> ActivityAnnouncementView.from(
                        announcement,
                        publisherName(announcement.getPublisherId())
                ))
                .toList();
    }

    @Transactional
    public ActivityAnnouncementView publish(
            Long activityId,
            ActivityAnnouncementRequest request,
            Authentication authentication
    ) {
        Activity activity = synchronizeStatus(requiredActivity(activityId));
        AuthenticatedUser publisher = ensureCanManage(activity, authentication);
        if (activity.getStatus() != ActivityStatus.PUBLISHED && activity.getStatus() != ActivityStatus.ONGOING) {
            throw new BusinessException(409, "当前活动状态不允许发布公告");
        }

        ActivityAnnouncement announcement = new ActivityAnnouncement();
        announcement.setActivityId(activityId);
        announcement.setPublisherId(publisher.user().getId());
        announcement.setTitle(request.title().trim());
        announcement.setContent(request.content().trim());
        announcement.setCreatedAt(LocalDateTime.now());
        announcementMapper.insert(announcement);

        String notificationTitle = "活动公告：" + announcement.getTitle();
        String notificationContent = "“" + activity.getTitle() + "”发布了新公告：\n" + announcement.getContent();
        registrationMapper.selectByActivityId(activityId).stream()
                .filter(registration -> registration.getStatus() == RegistrationStatus.REGISTERED)
                .map(Registration::getUserId)
                .forEach(userId -> notificationService.createDeduplicated(
                        userId,
                        "ACTIVITY_ANNOUNCEMENT",
                        notificationTitle,
                        notificationContent,
                        "ACTIVITY",
                        activityId,
                        "activity-announcement:" + announcement.getId() + ":" + userId
                ));

        operationLogService.record(
                publisher.user().getId(),
                "ACTIVITY_ANNOUNCEMENT_SENT",
                "ACTIVITY",
                activityId
        );
        return ActivityAnnouncementView.from(announcement, publisher.user().getName());
    }

    private Activity requiredActivity(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(404, "活动不存在");
        }
        return activity;
    }

    private Activity synchronizeStatus(Activity activity) {
        ActivityStatus resolved = ActivityStatusResolver.resolve(activity, LocalDateTime.now());
        if (resolved != activity.getStatus()) {
            activity.setStatus(resolved);
            activityMapper.updateById(activity);
        }
        return activity;
    }

    private AuthenticatedUser ensureCanManage(Activity activity, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(401, "请先登录");
        }
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.ORGANIZER) {
            throw new BusinessException(403, "只有组织者或管理员可以发布公告");
        }
        if (user.getRole() != UserRole.ADMIN && !user.user().getId().equals(activity.getOrganizerId())) {
            throw new BusinessException(403, "不能管理其他组织者的活动");
        }
        return user;
    }

    private String publisherName(Long publisherId) {
        User publisher = publisherId == null ? null : userMapper.selectById(publisherId);
        return publisher == null ? null : publisher.getName();
    }
}
