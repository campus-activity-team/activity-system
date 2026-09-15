package com.example.activity.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.activity.dto.ActivityRequest;
import com.example.activity.dto.RejectActivityRequest;
import com.example.activity.entity.Activity;
import com.example.activity.entity.ActivityStatus;
import com.example.activity.entity.UserRole;
import com.example.activity.exception.BusinessException;
import com.example.activity.mapper.ActivityMapper;
import com.example.activity.security.AuthenticatedUser;
import com.example.activity.vo.ActivityView;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class ActivityService {

    private final ActivityMapper activityMapper;

    public ActivityService(ActivityMapper activityMapper) {
        this.activityMapper = activityMapper;
    }

    @Transactional
    public ActivityView create(ActivityRequest request, Authentication authentication) {
        AuthenticatedUser user = requireOrganizer(authentication);
        validateSchedule(request);
        validateLocationSettings(request);
        Activity activity = new Activity();
        activity.setOrganizerId(user.user().getId());
        activity.setCurrentRegisteredCount(0);
        activity.setStatus(ActivityStatus.DRAFT);
        applyRequest(activity, request);
        activityMapper.insert(activity);
        return ActivityView.from(activity);
    }

    @Transactional
    public ActivityView update(Long id, ActivityRequest request, Authentication authentication) {
        Activity activity = getRequired(id);
        ensureCanManage(activity, authentication);
        ensureStatus(activity, ActivityStatus.DRAFT, ActivityStatus.REJECTED);
        validateSchedule(request);
        validateLocationSettings(request);
        int registered = activity.getCurrentRegisteredCount() == null ? 0 : activity.getCurrentRegisteredCount();
        if (request.capacity() < registered) {
            throw new BusinessException(409, "活动容量不能小于当前报名人数");
        }
        applyRequest(activity, request);
        activityMapper.updateById(activity);
        return ActivityView.from(activity);
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        Activity activity = getRequired(id);
        ensureCanManage(activity, authentication);
        ensureStatus(activity, ActivityStatus.DRAFT, ActivityStatus.REJECTED);
        activityMapper.deleteById(id);
    }

    @Transactional
    public ActivityView submit(Long id, Authentication authentication) {
        Activity activity = getRequired(id);
        ensureCanManage(activity, authentication);
        ensureStatus(activity, ActivityStatus.DRAFT, ActivityStatus.REJECTED);
        validateStoredSchedule(activity);
        activity.setStatus(ActivityStatus.PENDING_REVIEW);
        activity.setReviewComment(null);
        activityMapper.updateById(activity);
        return ActivityView.from(activity);
    }

    @Transactional
    public List<ActivityView> listPublic(String keyword, ActivityStatus status) {
        if (status != null && !isPublicStatus(status)) {
            throw new BusinessException(400, "当前状态不允许公开查询");
        }
        var query = Wrappers.<Activity>lambdaQuery()
                .in(Activity::getStatus,
                        ActivityStatus.PUBLISHED, ActivityStatus.ONGOING, ActivityStatus.ENDED)
                .and(keyword != null && !keyword.isBlank(),
                        wrapper -> wrapper.like(Activity::getTitle, keyword.trim())
                                .or()
                                .like(Activity::getLocation, keyword.trim()))
                .orderByAsc(Activity::getStartTime);
        return activityMapper.selectList(query).stream()
                .map(this::synchronizeStatus)
                .filter(activity -> isPublicStatus(activity.getStatus()))
                .filter(activity -> status == null || activity.getStatus() == status)
                .map(ActivityView::publicFrom)
                .toList();
    }

    @Transactional
    public ActivityView getPublic(Long id) {
        Activity activity = synchronizeStatus(getRequired(id));
        if (!isPublicStatus(activity.getStatus())) {
            throw new BusinessException(404, "活动不存在");
        }
        return ActivityView.publicFrom(activity);
    }

    @Transactional
    public List<ActivityView> listMine(Authentication authentication) {
        AuthenticatedUser user = requireOrganizer(authentication);
        var query = Wrappers.<Activity>lambdaQuery()
                .eq(user.getRole() != UserRole.ADMIN, Activity::getOrganizerId, user.user().getId())
                .orderByDesc(Activity::getCreatedAt);
        return activityMapper.selectList(query).stream().map(this::synchronizeStatus).map(ActivityView::from).toList();
    }

    @Transactional
    public ActivityView getMine(Long id, Authentication authentication) {
        Activity activity = synchronizeStatus(getRequired(id));
        ensureCanManage(activity, authentication);
        return ActivityView.from(activity);
    }

    @Transactional
    public List<ActivityView> listPendingReview() {
        return activityMapper.selectList(
                Wrappers.<Activity>lambdaQuery()
                        .eq(Activity::getStatus, ActivityStatus.PENDING_REVIEW)
                        .orderByAsc(Activity::getCreatedAt)
        ).stream().map(this::synchronizeStatus).map(ActivityView::from).toList();
    }

    @Transactional
    public List<ActivityView> listAll() {
        return activityMapper.selectList(Wrappers.<Activity>lambdaQuery().orderByDesc(Activity::getCreatedAt))
                .stream().map(this::synchronizeStatus).map(ActivityView::from).toList();
    }

    @Transactional
    public ActivityView approve(Long id) {
        Activity activity = getRequired(id);
        ensureStatus(activity, ActivityStatus.PENDING_REVIEW);
        activity.setStatus(ActivityStatus.APPROVED);
        activity.setReviewComment(null);
        activityMapper.updateById(activity);
        return ActivityView.from(activity);
    }

    @Transactional
    public ActivityView reject(Long id, RejectActivityRequest request) {
        Activity activity = getRequired(id);
        ensureStatus(activity, ActivityStatus.PENDING_REVIEW);
        activity.setStatus(ActivityStatus.REJECTED);
        activity.setReviewComment(request.reviewComment().trim());
        activityMapper.updateById(activity);
        return ActivityView.from(activity);
    }

    @Transactional
    public ActivityView publish(Long id) {
        Activity activity = getRequired(id);
        ensureStatus(activity, ActivityStatus.APPROVED);
        if (!activity.getEndTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(409, "活动已经结束，不能发布");
        }
        activity.setStatus(ActivityStatus.PUBLISHED);
        activityMapper.updateById(activity);
        return ActivityView.from(activity);
    }

    @Transactional
    public ActivityView unpublish(Long id) {
        Activity activity = getRequired(id);
        ensureStatus(activity, ActivityStatus.PUBLISHED, ActivityStatus.ONGOING);
        activity.setStatus(ActivityStatus.APPROVED);
        activityMapper.updateById(activity);
        return ActivityView.from(activity);
    }

    @Transactional
    public ActivityView start(Long id, Authentication authentication) {
        Activity activity = getRequired(id);
        ensureCanManage(activity, authentication);
        ensureStatus(activity, ActivityStatus.PUBLISHED);
        if (activity.getEndTime() == null || !activity.getEndTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(409, "活动已经结束，不能开启");
        }
        activity.setStatus(ActivityStatus.ONGOING);
        activityMapper.updateById(activity);
        return ActivityView.from(activity);
    }

    private Activity getRequired(Long id) {
        Activity activity = activityMapper.selectById(id);
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

    private AuthenticatedUser requireOrganizer(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)
                || (user.getRole() != UserRole.ORGANIZER && user.getRole() != UserRole.ADMIN)) {
            throw new BusinessException(403, "只有组织者或管理员可以管理活动");
        }
        return user;
    }

    private void ensureCanManage(Activity activity, Authentication authentication) {
        AuthenticatedUser user = requireOrganizer(authentication);
        if (user.getRole() != UserRole.ADMIN && !user.user().getId().equals(activity.getOrganizerId())) {
            throw new BusinessException(403, "不能管理其他组织者的活动");
        }
    }

    private void ensureStatus(Activity activity, ActivityStatus... allowed) {
        if (Arrays.stream(allowed).noneMatch(status -> status == activity.getStatus())) {
            throw new BusinessException(409, "当前活动状态不允许执行此操作");
        }
    }

    private boolean isPublicStatus(ActivityStatus status) {
        return status == ActivityStatus.PUBLISHED
                || status == ActivityStatus.ONGOING
                || status == ActivityStatus.ENDED;
    }

    private void validateSchedule(ActivityRequest request) {
        if (!request.registrationStartTime().isBefore(request.registrationEndTime())) {
            throw new BusinessException(400, "报名开始时间必须早于报名结束时间");
        }
        if (!request.startTime().isBefore(request.endTime())) {
            throw new BusinessException(400, "活动开始时间必须早于活动结束时间");
        }
        if (request.registrationEndTime().isAfter(request.startTime())) {
            throw new BusinessException(400, "报名结束时间不能晚于活动开始时间");
        }
        if (request.requireFeedback() == Boolean.TRUE && request.feedbackDeadline() != null
                && request.feedbackDeadline().isBefore(request.endTime())) {
            throw new BusinessException(400, "反馈截止时间不能早于活动结束时间");
        }
    }

    private void validateLocationSettings(ActivityRequest request) {
        boolean hasLatitude = request.checkinLatitude() != null;
        boolean hasLongitude = request.checkinLongitude() != null;
        boolean hasRadius = request.checkinRadiusMeters() != null;
        if (hasLatitude != hasLongitude) {
            throw new BusinessException(400, "位置签到必须同时填写纬度和经度");
        }
        if ((hasLatitude || hasLongitude) && !hasRadius) {
            throw new BusinessException(400, "启用位置签到时必须填写允许半径");
        }
        if (!hasLatitude && !hasLongitude && hasRadius) {
            throw new BusinessException(400, "填写签到半径前请先设置签到位置");
        }
    }

    private void validateStoredSchedule(Activity activity) {
        if (!activity.getRegistrationStartTime().isBefore(activity.getRegistrationEndTime())
                || !activity.getStartTime().isBefore(activity.getEndTime())
                || activity.getRegistrationEndTime().isAfter(activity.getStartTime())) {
            throw new BusinessException(400, "活动时间安排不合法，无法提交审核");
        }
    }

    private void applyRequest(Activity activity, ActivityRequest request) {
        activity.setTitle(request.title().trim());
        activity.setDescription(request.description().trim());
        activity.setCoverImage(blankToNull(request.coverImage()));
        activity.setLocation(request.location().trim());
        activity.setCheckinLatitude(request.checkinLatitude());
        activity.setCheckinLongitude(request.checkinLongitude());
        activity.setCheckinRadiusMeters(request.checkinRadiusMeters());
        activity.setStartTime(request.startTime());
        activity.setEndTime(request.endTime());
        activity.setRegistrationStartTime(request.registrationStartTime());
        activity.setRegistrationEndTime(request.registrationEndTime());
        activity.setCapacity(request.capacity());
        activity.setRequireFeedback(Boolean.TRUE.equals(request.requireFeedback()));
        activity.setFeedbackDeadline(request.feedbackDeadline());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
