package com.example.activity.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.activity.entity.Activity;
import com.example.activity.entity.CheckinAnomaly;
import com.example.activity.entity.CheckinAnomalyReason;
import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.exception.BusinessException;
import com.example.activity.mapper.ActivityMapper;
import com.example.activity.mapper.CheckinAnomalyMapper;
import com.example.activity.mapper.UserMapper;
import com.example.activity.security.AuthenticatedUser;
import com.example.activity.vo.CheckinAnomalyView;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CheckinAnomalyService {

    private final CheckinAnomalyMapper checkinAnomalyMapper;
    private final ActivityMapper activityMapper;
    private final UserMapper userMapper;

    public CheckinAnomalyService(
            CheckinAnomalyMapper checkinAnomalyMapper,
            ActivityMapper activityMapper,
            UserMapper userMapper
    ) {
        this.checkinAnomalyMapper = checkinAnomalyMapper;
        this.activityMapper = activityMapper;
        this.userMapper = userMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            Long activityId,
            User user,
            CheckinAnomalyReason reason,
            String message,
            Double latitude,
            Double longitude,
            Integer distanceMeters
    ) {
        CheckinAnomaly anomaly = new CheckinAnomaly();
        anomaly.setActivityId(activityId);
        anomaly.setUserId(user.getId());
        anomaly.setReason(reason);
        anomaly.setMessage(message);
        anomaly.setLatitude(latitude);
        anomaly.setLongitude(longitude);
        anomaly.setDistanceMeters(distanceMeters);
        anomaly.setCreatedAt(LocalDateTime.now());
        checkinAnomalyMapper.insert(anomaly);
    }

    @Transactional(readOnly = true)
    public List<CheckinAnomalyView> listForActivity(Long activityId, Authentication authentication) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(404, "活动不存在");
        }
        User currentUser = currentUser(authentication);
        if (currentUser.getRole() != UserRole.ADMIN && !currentUser.getId().equals(activity.getOrganizerId())) {
            throw new BusinessException(403, "不能查看其他组织者的异常签到记录");
        }
        return checkinAnomalyMapper.selectList(
                        Wrappers.<CheckinAnomaly>lambdaQuery()
                                .eq(CheckinAnomaly::getActivityId, activityId)
                                .orderByDesc(CheckinAnomaly::getCreatedAt)
                ).stream()
                .map(anomaly -> toView(anomaly, userMapper.selectById(anomaly.getUserId())))
                .toList();
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new BusinessException(401, "请先登录");
        }
        return authenticatedUser.user();
    }

    private CheckinAnomalyView toView(CheckinAnomaly anomaly, User user) {
        return new CheckinAnomalyView(
                anomaly.getId(),
                anomaly.getActivityId(),
                anomaly.getUserId(),
                user.getUsername(),
                user.getName(),
                user.getStudentId(),
                anomaly.getReason(),
                anomaly.getMessage(),
                anomaly.getLatitude(),
                anomaly.getLongitude(),
                anomaly.getDistanceMeters(),
                anomaly.getCreatedAt()
        );
    }
}
