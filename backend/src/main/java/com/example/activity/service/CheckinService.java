package com.example.activity.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.activity.dto.CheckinRequest;
import com.example.activity.entity.Activity;
import com.example.activity.entity.ActivityStatus;
import com.example.activity.entity.Attendance;
import com.example.activity.entity.AttendanceStatus;
import com.example.activity.entity.CheckinMethod;
import com.example.activity.entity.CheckinToken;
import com.example.activity.entity.Registration;
import com.example.activity.entity.RegistrationStatus;
import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.exception.BusinessException;
import com.example.activity.mapper.ActivityMapper;
import com.example.activity.mapper.AttendanceMapper;
import com.example.activity.mapper.CheckinTokenMapper;
import com.example.activity.mapper.RegistrationMapper;
import com.example.activity.mapper.UserMapper;
import com.example.activity.security.AuthenticatedUser;
import com.example.activity.vo.AttendanceView;
import com.example.activity.vo.CheckinTokenView;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class CheckinService {

    private static final long TOKEN_TTL_SECONDS = 60;
    private final SecureRandom secureRandom = new SecureRandom();

    private final ActivityMapper activityMapper;
    private final CheckinTokenMapper checkinTokenMapper;
    private final RegistrationMapper registrationMapper;
    private final AttendanceMapper attendanceMapper;
    private final UserMapper userMapper;

    public CheckinService(
            ActivityMapper activityMapper,
            CheckinTokenMapper checkinTokenMapper,
            RegistrationMapper registrationMapper,
            AttendanceMapper attendanceMapper,
            UserMapper userMapper
    ) {
        this.activityMapper = activityMapper;
        this.checkinTokenMapper = checkinTokenMapper;
        this.registrationMapper = registrationMapper;
        this.attendanceMapper = attendanceMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public CheckinTokenView issueToken(Long activityId, Authentication authentication) {
        Activity activity = requiredActivity(activityId);
        ensureCanManage(activity, authentication);
        LocalDateTime now = LocalDateTime.now();
        ActivityStatus resolvedStatus = ActivityStatusResolver.resolve(activity, now);
        if (resolvedStatus != activity.getStatus()) {
            activity.setStatus(resolvedStatus);
            activityMapper.updateById(activity);
        }
        if (resolvedStatus != ActivityStatus.ONGOING) {
            throw new BusinessException(409, "只有进行中的活动可以开启签到");
        }

        CheckinToken token = new CheckinToken();
        token.setToken(randomToken());
        token.setActivityId(activityId);
        token.setCreatedAt(now);
        token.setExpiresAt(now.plusSeconds(TOKEN_TTL_SECONDS));
        checkinTokenMapper.insert(token);
        return new CheckinTokenView(activityId, token.getToken(), token.getExpiresAt(), TOKEN_TTL_SECONDS);
    }

    @Transactional
    public AttendanceView checkin(CheckinRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        CheckinToken token = checkinTokenMapper.selectByToken(request.token().trim());
        if (token == null || token.getExpiresAt() == null || !token.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(409, "签到二维码已过期，请扫描最新二维码");
        }

        Activity activity = requiredActivity(token.getActivityId());
        ActivityStatus resolvedStatus = ActivityStatusResolver.resolve(activity, LocalDateTime.now());
        if (resolvedStatus != activity.getStatus()) {
            activity.setStatus(resolvedStatus);
            activityMapper.updateById(activity);
        }
        if (resolvedStatus != ActivityStatus.ONGOING) {
            throw new BusinessException(409, "当前不在活动签到时间内");
        }
        Registration registration = registrationMapper.selectByActivityAndUser(activity.getId(), user.getId());
        if (registration == null || registration.getStatus() != RegistrationStatus.REGISTERED) {
            throw new BusinessException(409, "请先报名后再签到");
        }
        Attendance existing = attendanceMapper.selectByActivityAndUser(activity.getId(), user.getId());
        if (existing != null && existing.getStatus() == AttendanceStatus.SUCCESS) {
            throw new BusinessException(409, "你已经签到过了");
        }

        Attendance attendance = existing == null ? new Attendance() : existing;
        attendance.setActivityId(activity.getId());
        attendance.setUserId(user.getId());
        attendance.setRegistrationId(registration.getId());
        attendance.setCheckinTime(LocalDateTime.now());
        attendance.setCheckinMethod(CheckinMethod.QR_CODE);
        attendance.setStatus(AttendanceStatus.SUCCESS);
        if (existing == null) {
            attendanceMapper.insert(attendance);
        } else {
            attendanceMapper.updateById(attendance);
        }
        return toView(attendance, activity, user);
    }

    public List<AttendanceView> listForActivity(Long activityId, Authentication authentication) {
        Activity activity = requiredActivity(activityId);
        ensureCanManage(activity, authentication);
        return attendanceMapper.selectList(Wrappers.<Attendance>lambdaQuery()
                        .eq(Attendance::getActivityId, activityId)
                        .eq(Attendance::getStatus, AttendanceStatus.SUCCESS)
                        .orderByDesc(Attendance::getCheckinTime))
                .stream()
                .map(attendance -> toView(attendance, activity, userMapper.selectById(attendance.getUserId())))
                .toList();
    }

    private String randomToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
            throw new BusinessException(403, "不能管理其他组织者的签到");
        }
    }

    private AttendanceView toView(Attendance attendance, Activity activity, User user) {
        return new AttendanceView(
                activity.getId(),
                user.getId(),
                activity.getTitle(),
                user.getUsername(),
                user.getName(),
                user.getStudentId(),
                attendance.getCheckinTime(),
                attendance.getCheckinMethod().name()
        );
    }
}
