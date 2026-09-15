package com.example.activity.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.activity.entity.Activity;
import com.example.activity.entity.ActivityStatus;
import com.example.activity.entity.Attendance;
import com.example.activity.entity.Registration;
import com.example.activity.entity.RegistrationStatus;
import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.exception.BusinessException;
import com.example.activity.mapper.ActivityMapper;
import com.example.activity.mapper.AttendanceMapper;
import com.example.activity.mapper.RegistrationMapper;
import com.example.activity.mapper.UserMapper;
import com.example.activity.security.AuthenticatedUser;
import com.example.activity.vo.RegistrationView;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class RegistrationService {

    private static final DateTimeFormatter CSV_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final AttendanceMapper attendanceMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public RegistrationService(
            ActivityMapper activityMapper,
            RegistrationMapper registrationMapper,
            AttendanceMapper attendanceMapper,
            UserMapper userMapper,
            NotificationService notificationService
    ) {
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.attendanceMapper = attendanceMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public RegistrationView register(Long activityId, Authentication authentication) {
        User user = currentUser(authentication);
        Activity activity = requiredLockedActivity(activityId);
        ensureRegistrationOpen(activity);

        Registration existing = registrationMapper.selectByActivityAndUser(activityId, user.getId());
        if (existing != null && existing.getStatus() == RegistrationStatus.REGISTERED) {
            throw new BusinessException(409, "你已经报名该活动");
        }
        int registeredCount = activity.getCurrentRegisteredCount() == null ? 0 : activity.getCurrentRegisteredCount();
        if (registeredCount >= activity.getCapacity()) {
            throw new BusinessException(409, "活动报名人数已满");
        }

        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            Registration registration = new Registration();
            registration.setActivityId(activityId);
            registration.setUserId(user.getId());
            registration.setStatus(RegistrationStatus.REGISTERED);
            registration.setRegisteredAt(now);
            registrationMapper.insert(registration);
            existing = registration;
        } else {
            existing.setStatus(RegistrationStatus.REGISTERED);
            existing.setRegisteredAt(now);
            existing.setCancelledAt(null);
            registrationMapper.updateById(existing);
        }

        activity.setCurrentRegisteredCount(registeredCount + 1);
        activityMapper.updateById(activity);
        notificationService.create(
                user.getId(),
                "REGISTRATION_SUCCESS",
                "活动报名成功",
                "你已成功报名“" + activity.getTitle() + "”，请留意活动开始提醒。",
                "ACTIVITY",
                activity.getId()
        );
        return toView(existing, activity, user);
    }

    @Transactional
    public void cancel(Long activityId, Authentication authentication) {
        User user = currentUser(authentication);
        Activity activity = requiredLockedActivity(activityId);
        Registration registration = registrationMapper.selectByActivityAndUser(activityId, user.getId());
        if (registration == null || registration.getStatus() != RegistrationStatus.REGISTERED) {
            throw new BusinessException(409, "你尚未报名该活动");
        }
        if (!LocalDateTime.now().isBefore(activity.getStartTime())) {
            throw new BusinessException(409, "活动开始后不能取消报名");
        }

        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(LocalDateTime.now());
        registrationMapper.updateById(registration);
        int registeredCount = activity.getCurrentRegisteredCount() == null ? 0 : activity.getCurrentRegisteredCount();
        activity.setCurrentRegisteredCount(Math.max(registeredCount - 1, 0));
        activityMapper.updateById(activity);
    }

    public RegistrationView mineForActivity(Long activityId, Authentication authentication) {
        User user = currentUser(authentication);
        Activity activity = requiredActivity(activityId);
        Registration registration = registrationMapper.selectByActivityAndUser(activityId, user.getId());
        return registration == null ? null : toView(registration, activity, user);
    }

    public List<RegistrationView> mine(Authentication authentication) {
        User user = currentUser(authentication);
        return registrationMapper.selectList(Wrappers.<Registration>lambdaQuery()
                        .eq(Registration::getUserId, user.getId())
                        .orderByDesc(Registration::getRegisteredAt))
                .stream()
                .map(registration -> {
                    Activity activity = activityMapper.selectById(registration.getActivityId());
                    return activity == null ? null : toView(registration, activity, user);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public List<RegistrationView> listForActivity(Long activityId, Authentication authentication) {
        Activity activity = requiredActivity(activityId);
        ensureCanManage(activity, authentication);
        return registrationMapper.selectByActivityId(activityId)
                .stream()
                .map(registration -> toView(registration, activity, userMapper.selectById(registration.getUserId())))
                .toList();
    }

    public byte[] exportForActivity(Long activityId, Authentication authentication) {
        List<RegistrationView> registrations = listForActivity(activityId, authentication);
        StringBuilder csv = new StringBuilder("\uFEFF姓名,用户名,学号,报名状态,报名时间,签到状态,签到时间,签到方式\r\n");
        for (RegistrationView registration : registrations) {
            csv.append(csvCell(registration.name())).append(',')
                    .append(csvCell(registration.username())).append(',')
                    .append(csvCell(registration.studentId())).append(',')
                    .append(csvCell(registration.status() == RegistrationStatus.REGISTERED ? "已报名" : "已取消")).append(',')
                    .append(csvCell(formatCsvTime(registration.registeredAt()))).append(',')
                    .append(csvCell(registration.checkedIn() ? "已签到" : "未签到")).append(',')
                    .append(csvCell(formatCsvTime(registration.checkinTime()))).append(',')
                    .append(csvCell(checkinMethodLabel(registration.checkinMethod())))
                    .append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void ensureRegistrationOpen(Activity activity) {
        ActivityStatus resolvedStatus = ActivityStatusResolver.resolve(activity, LocalDateTime.now());
        if (resolvedStatus != activity.getStatus()) {
            activity.setStatus(resolvedStatus);
            activityMapper.updateById(activity);
        }
        if (resolvedStatus != ActivityStatus.PUBLISHED && resolvedStatus != ActivityStatus.ONGOING) {
            throw new BusinessException(409, "当前活动暂未开放报名");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getRegistrationStartTime()) || now.isAfter(activity.getRegistrationEndTime())) {
            throw new BusinessException(409, "当前不在活动报名时间内");
        }
    }

    private Activity requiredLockedActivity(Long activityId) {
        Activity activity = activityMapper.selectByIdForUpdate(activityId);
        if (activity == null) {
            throw new BusinessException(404, "活动不存在");
        }
        return activity;
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
            throw new BusinessException(403, "不能查看其他组织者的报名名单");
        }
    }

    private RegistrationView toView(Registration registration, Activity activity, User user) {
        Attendance attendance = attendanceMapper.selectByActivityAndUser(activity.getId(), user.getId());
        boolean checkedIn = attendance != null && attendance.getStatus() != null
                && attendance.getStatus().name().equals("SUCCESS");
        return new RegistrationView(
                registration.getId(),
                activity.getId(),
                activity.getTitle(),
                ActivityStatusResolver.resolve(activity, LocalDateTime.now()),
                Boolean.TRUE.equals(activity.getRequireFeedback()),
                activity.getFeedbackDeadline(),
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getStudentId(),
                registration.getStatus(),
                registration.getRegisteredAt(),
                registration.getCancelledAt(),
                checkedIn,
                checkedIn ? attendance.getCheckinTime() : null,
                checkedIn ? attendance.getCheckinMethod().name() : null
        );
    }

    private String formatCsvTime(LocalDateTime value) {
        return value == null ? "" : CSV_TIME_FORMATTER.format(value);
    }

    private String checkinMethodLabel(String method) {
        if (method == null) {
            return "";
        }
        return "MANUAL".equals(method) ? "手动补签" : "二维码";
    }

    private String csvCell(String value) {
        String safeValue = value == null ? "" : value;
        if (!safeValue.isEmpty() && "=+-@".indexOf(safeValue.charAt(0)) >= 0) {
            safeValue = "'" + safeValue;
        }
        return '"' + safeValue.replace("\"", "\"\"") + '"';
    }
}
