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
import com.example.activity.exception.BusinessException;
import com.example.activity.mapper.ActivityMapper;
import com.example.activity.mapper.AttendanceMapper;
import com.example.activity.mapper.FeedbackMapper;
import com.example.activity.mapper.RegistrationMapper;
import com.example.activity.security.AuthenticatedUser;
import com.example.activity.vo.FeedbackDashboardView;
import com.example.activity.vo.FeedbackStatusView;
import com.example.activity.vo.FeedbackView;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

@Service
public class FeedbackService {

    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final AttendanceMapper attendanceMapper;
    private final FeedbackMapper feedbackMapper;

    public FeedbackService(
            ActivityMapper activityMapper,
            RegistrationMapper registrationMapper,
            AttendanceMapper attendanceMapper,
            FeedbackMapper feedbackMapper
    ) {
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.attendanceMapper = attendanceMapper;
        this.feedbackMapper = feedbackMapper;
    }

    public FeedbackStatusView mine(Long activityId, Authentication authentication) {
        User user = currentUser(authentication);
        Activity activity = requiredActivity(activityId);
        Registration registration = registrationMapper.selectByActivityAndUser(activityId, user.getId());
        Attendance attendance = attendanceMapper.selectByActivityAndUser(activityId, user.getId());
        Feedback feedback = feedbackMapper.selectByActivityAndUser(activityId, user.getId());
        LocalDateTime now = LocalDateTime.now();
        ActivityStatus resolvedStatus = ActivityStatusResolver.resolve(activity, now);
        String unavailableReason = unavailableReason(activity, resolvedStatus, registration, attendance, now);
        boolean attended = attendance != null && attendance.getStatus() == AttendanceStatus.SUCCESS;
        return new FeedbackStatusView(
                activity.getId(),
                activity.getTitle(),
                resolvedStatus,
                Boolean.TRUE.equals(activity.getRequireFeedback()),
                attended,
                unavailableReason == null,
                unavailableReason,
                activity.getFeedbackDeadline(),
                feedback == null ? null : toView(feedback, activity)
        );
    }

    @Transactional
    public FeedbackView submit(Long activityId, FeedbackRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        Activity activity = requiredActivity(activityId);
        Registration registration = registrationMapper.selectByActivityAndUser(activityId, user.getId());
        Attendance attendance = attendanceMapper.selectByActivityAndUser(activityId, user.getId());
        LocalDateTime now = LocalDateTime.now();
        ActivityStatus resolvedStatus = ActivityStatusResolver.resolve(activity, now);
        String unavailableReason = unavailableReason(activity, resolvedStatus, registration, attendance, now);
        if (unavailableReason != null) {
            throw new BusinessException(409, unavailableReason);
        }

        Feedback feedback = feedbackMapper.selectByActivityAndUser(activityId, user.getId());
        if (feedback == null) {
            feedback = new Feedback();
            feedback.setActivityId(activityId);
            feedback.setUserId(user.getId());
            applyRequest(feedback, request);
            feedbackMapper.insert(feedback);
        } else {
            applyRequest(feedback, request);
            feedbackMapper.updateById(feedback);
        }
        return toView(feedback, activity);
    }

    public FeedbackDashboardView dashboard(Long activityId, Authentication authentication) {
        Activity activity = requiredActivity(activityId);
        ensureCanManage(activity, authentication);
        List<Feedback> feedbacks = feedbackMapper.selectByActivityId(activityId);
        long attendedCount = attendanceMapper.countSuccessfulByActivityId(activityId);
        int feedbackCount = feedbacks.size();
        int registeredCount = activity.getCurrentRegisteredCount() == null ? 0 : activity.getCurrentRegisteredCount();
        double responseRate = attendedCount == 0 ? 0.0 : roundOneDecimal(feedbackCount * 100.0 / attendedCount);

        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int rating = 1; rating <= 5; rating++) {
            int currentRating = rating;
            distribution.put(rating, feedbacks.stream()
                    .filter(feedback -> feedback.getOverallRating() == currentRating)
                    .count());
        }

        return new FeedbackDashboardView(
                activity.getId(),
                activity.getTitle(),
                registeredCount,
                attendedCount,
                feedbackCount,
                responseRate,
                average(feedbacks, Feedback::getOverallRating),
                average(feedbacks, Feedback::getContentRating),
                average(feedbacks, Feedback::getServiceRating),
                distribution,
                feedbacks.stream().map(feedback -> toView(feedback, activity)).toList()
        );
    }

    private String unavailableReason(
            Activity activity,
            ActivityStatus resolvedStatus,
            Registration registration,
            Attendance attendance,
            LocalDateTime now
    ) {
        if (!Boolean.TRUE.equals(activity.getRequireFeedback())) {
            return "该活动未开启反馈收集";
        }
        if (resolvedStatus != ActivityStatus.ENDED) {
            return "活动结束后才能提交反馈";
        }
        if (registration == null || registration.getStatus() != RegistrationStatus.REGISTERED) {
            return "只有成功报名的参与者才能提交反馈";
        }
        if (attendance == null || attendance.getStatus() != AttendanceStatus.SUCCESS) {
            return "只有已签到的参与者才能提交反馈";
        }
        if (activity.getFeedbackDeadline() != null && now.isAfter(activity.getFeedbackDeadline())) {
            return "反馈提交已截止";
        }
        return null;
    }

    private void applyRequest(Feedback feedback, FeedbackRequest request) {
        feedback.setOverallRating(request.overallRating());
        feedback.setContentRating(request.contentRating());
        feedback.setServiceRating(request.serviceRating());
        feedback.setComment(request.comment() == null || request.comment().isBlank() ? null : request.comment().trim());
    }

    private double average(List<Feedback> feedbacks, ToIntFunction<Feedback> ratingSelector) {
        if (feedbacks.isEmpty()) {
            return 0.0;
        }
        return roundOneDecimal(feedbacks.stream().mapToInt(ratingSelector).average().orElse(0.0));
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private FeedbackView toView(Feedback feedback, Activity activity) {
        return new FeedbackView(
                feedback.getId(),
                activity.getId(),
                activity.getTitle(),
                feedback.getOverallRating(),
                feedback.getContentRating(),
                feedback.getServiceRating(),
                feedback.getComment(),
                feedback.getCreatedAt(),
                feedback.getUpdatedAt()
        );
    }

    private Activity requiredActivity(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(404, "活动不存在");
        }
        return activity;
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new BusinessException(401, "请先登录");
        }
        return authenticatedUser.user();
    }

    private void ensureCanManage(Activity activity, Authentication authentication) {
        User user = currentUser(authentication);
        if (user.getRole() != UserRole.ADMIN && !user.getId().equals(activity.getOrganizerId())) {
            throw new BusinessException(403, "不能查看其他组织者的反馈数据");
        }
    }
}
