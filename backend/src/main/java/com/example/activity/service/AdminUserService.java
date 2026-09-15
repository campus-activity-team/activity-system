package com.example.activity.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.activity.dto.AdminRoleChangeRequest;
import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.entity.UserStatus;
import com.example.activity.exception.BusinessException;
import com.example.activity.mapper.UserMapper;
import com.example.activity.security.AuthenticatedUser;
import com.example.activity.vo.UserProfile;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;

    public AdminUserService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            OperationLogService operationLogService
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public List<UserProfile> listUsers() {
        return userMapper.selectList(Wrappers.<User>lambdaQuery().orderByDesc(User::getCreatedAt))
                .stream()
                .map(UserProfile::from)
                .toList();
    }

    @Transactional
    public UserProfile changeRole(Long userId, AdminRoleChangeRequest request, Authentication authentication) {
        User administrator = currentAdministrator(authentication);
        if (!passwordEncoder.matches(request.currentPassword(), administrator.getPasswordHash())) {
            throw new BusinessException(403, "当前管理员密码不正确");
        }
        if (administrator.getId().equals(userId)) {
            throw new BusinessException(409, "不能修改自己的管理员角色");
        }

        User target = userMapper.selectById(userId);
        if (target == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (target.getRole() == request.role()) {
            return UserProfile.from(target);
        }
        if (request.role() != UserRole.USER && target.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(409, "停用账号不能获得活动管理权限");
        }
        if (target.getRole() == UserRole.ADMIN && request.role() != UserRole.ADMIN) {
            long activeAdministratorCount = userMapper.selectCount(
                    Wrappers.<User>lambdaQuery()
                            .eq(User::getRole, UserRole.ADMIN)
                            .eq(User::getStatus, UserStatus.ACTIVE)
            );
            if (activeAdministratorCount <= 1) {
                throw new BusinessException(409, "系统必须至少保留一名有效管理员");
            }
        }

        UserRole previousRole = target.getRole();
        target.setRole(request.role());
        userMapper.updateById(target);
        operationLogService.record(
                administrator.getId(),
                "USER_ROLE_CHANGED_" + previousRole.name() + "_TO_" + request.role().name(),
                "USER",
                target.getId()
        );
        return UserProfile.from(target);
    }

    private User currentAdministrator(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)
                || authenticatedUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException(403, "只有管理员可以管理用户角色");
        }
        return authenticatedUser.user();
    }
}
