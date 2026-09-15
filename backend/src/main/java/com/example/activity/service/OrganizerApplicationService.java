package com.example.activity.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.activity.dto.OrganizerApplicationRequest;
import com.example.activity.dto.RejectOrganizerApplicationRequest;
import com.example.activity.entity.OrganizerApplication;
import com.example.activity.entity.OrganizerApplicationStatus;
import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.exception.BusinessException;
import com.example.activity.mapper.OrganizerApplicationMapper;
import com.example.activity.mapper.UserMapper;
import com.example.activity.security.AuthenticatedUser;
import com.example.activity.vo.OrganizerApplicationView;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrganizerApplicationService {

    private final OrganizerApplicationMapper organizerApplicationMapper;
    private final UserMapper userMapper;
    private final OperationLogService operationLogService;
    private final NotificationService notificationService;

    public OrganizerApplicationService(
            OrganizerApplicationMapper organizerApplicationMapper,
            UserMapper userMapper,
            OperationLogService operationLogService,
            NotificationService notificationService
    ) {
        this.organizerApplicationMapper = organizerApplicationMapper;
        this.userMapper = userMapper;
        this.operationLogService = operationLogService;
        this.notificationService = notificationService;
    }

    @Transactional
    public OrganizerApplicationView apply(OrganizerApplicationRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        if (user.getRole() != UserRole.USER) {
            throw new BusinessException(409, "当前账号已经具备活动管理权限，无需再次申请");
        }

        OrganizerApplication application = organizerApplicationMapper.selectByUserId(user.getId());
        if (application != null && application.getStatus() == OrganizerApplicationStatus.PENDING) {
            throw new BusinessException(409, "发起者申请正在审核中，请勿重复提交");
        }

        LocalDateTime now = LocalDateTime.now();
        if (application == null) {
            application = new OrganizerApplication();
            application.setUserId(user.getId());
            application.setCreatedAt(now);
        }
        application.setReason(request.reason().trim());
        application.setStatus(OrganizerApplicationStatus.PENDING);
        application.setReviewComment(null);
        application.setReviewedBy(null);
        application.setReviewedAt(null);
        application.setUpdatedAt(now);
        if (application.getId() == null) {
            organizerApplicationMapper.insert(application);
        } else {
            organizerApplicationMapper.updateById(application);
        }
        operationLogService.record(
                user.getId(),
                "ORGANIZER_APPLICATION_SUBMITTED",
                "ORGANIZER_APPLICATION",
                application.getId()
        );
        return toView(application, user);
    }

    @Transactional(readOnly = true)
    public OrganizerApplicationView mine(Authentication authentication) {
        User user = currentUser(authentication);
        OrganizerApplication application = organizerApplicationMapper.selectByUserId(user.getId());
        return application == null ? null : toView(application, user);
    }

    @Transactional(readOnly = true)
    public List<OrganizerApplicationView> listAll() {
        return organizerApplicationMapper.selectList(
                        Wrappers.<OrganizerApplication>lambdaQuery()
                                .orderByDesc(OrganizerApplication::getUpdatedAt)
                ).stream()
                .map(application -> toView(application, userMapper.selectById(application.getUserId())))
                .toList();
    }

    @Transactional
    public OrganizerApplicationView approve(Long id, Authentication authentication) {
        User administrator = currentAdministrator(authentication);
        OrganizerApplication application = requiredPending(id);
        User applicant = requiredUser(application.getUserId());
        if (applicant.getRole() == UserRole.ADMIN) {
            throw new BusinessException(409, "管理员账号不能通过发起者申请变更角色");
        }

        applicant.setRole(UserRole.ORGANIZER);
        userMapper.updateById(applicant);
        finishReview(application, administrator.getId(), OrganizerApplicationStatus.APPROVED, null);
        operationLogService.record(
                administrator.getId(),
                "ORGANIZER_APPLICATION_APPROVED",
                "ORGANIZER_APPLICATION",
                application.getId()
        );
        notificationService.create(
                applicant.getId(),
                "ORGANIZER_APPLICATION_APPROVED",
                "发起者申请已通过",
                "你已获得活动发起者权限，可以开始创建活动。",
                "ORGANIZER_APPLICATION",
                application.getId()
        );
        return toView(application, applicant);
    }

    @Transactional
    public OrganizerApplicationView reject(
            Long id,
            RejectOrganizerApplicationRequest request,
            Authentication authentication
    ) {
        User administrator = currentAdministrator(authentication);
        OrganizerApplication application = requiredPending(id);
        User applicant = requiredUser(application.getUserId());
        finishReview(
                application,
                administrator.getId(),
                OrganizerApplicationStatus.REJECTED,
                request.reviewComment().trim()
        );
        operationLogService.record(
                administrator.getId(),
                "ORGANIZER_APPLICATION_REJECTED",
                "ORGANIZER_APPLICATION",
                application.getId()
        );
        notificationService.create(
                applicant.getId(),
                "ORGANIZER_APPLICATION_REJECTED",
                "发起者申请未通过",
                "申请未通过。原因：" + application.getReviewComment(),
                "ORGANIZER_APPLICATION",
                application.getId()
        );
        return toView(application, applicant);
    }

    private void finishReview(
            OrganizerApplication application,
            Long administratorId,
            OrganizerApplicationStatus status,
            String reviewComment
    ) {
        LocalDateTime now = LocalDateTime.now();
        application.setStatus(status);
        application.setReviewComment(reviewComment);
        application.setReviewedBy(administratorId);
        application.setReviewedAt(now);
        application.setUpdatedAt(now);
        organizerApplicationMapper.updateById(application);
    }

    private OrganizerApplication requiredPending(Long id) {
        OrganizerApplication application = organizerApplicationMapper.selectByIdForUpdate(id);
        if (application == null) {
            throw new BusinessException(404, "发起者申请不存在");
        }
        if (application.getStatus() != OrganizerApplicationStatus.PENDING) {
            throw new BusinessException(409, "该申请已经处理，请刷新列表");
        }
        return application;
    }

    private User requiredUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "申请用户不存在");
        }
        return user;
    }

    private User currentAdministrator(Authentication authentication) {
        User user = currentUser(authentication);
        if (user.getRole() != UserRole.ADMIN) {
            throw new BusinessException(403, "只有管理员可以审核发起者申请");
        }
        return user;
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new BusinessException(401, "请先登录");
        }
        return authenticatedUser.user();
    }

    private OrganizerApplicationView toView(OrganizerApplication application, User user) {
        return new OrganizerApplicationView(
                application.getId(),
                application.getUserId(),
                user.getUsername(),
                user.getName(),
                user.getStudentId(),
                application.getReason(),
                application.getStatus(),
                application.getReviewComment(),
                application.getReviewedBy(),
                application.getReviewedAt(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}
