package com.example.activity.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.entity.UserStatus;
import com.example.activity.mapper.UserMapper;
import com.example.activity.service.OperationLogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
public class BootstrapAdminInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;

    @Value("${app.bootstrap-admin.username:}")
    private String username;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    @Value("${app.bootstrap-admin.name:系统管理员}")
    private String name;

    public BootstrapAdminInitializer(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            OperationLogService operationLogService
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.operationLogService = operationLogService;
    }

    @Override
    public void run(String... args) {
        long administratorCount = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getRole, UserRole.ADMIN)
        );
        if (administratorCount > 0) {
            return;
        }
        if (username == null || username.trim().length() < 3 || password == null || password.length() < 12) {
            throw new IllegalStateException("Bootstrap administrator requires a username and a password of at least 12 characters");
        }
        String normalizedUsername = username.trim();
        if (userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, normalizedUsername)
        ) > 0) {
            throw new IllegalStateException("Bootstrap administrator username already belongs to a non-administrator account");
        }

        User administrator = new User();
        administrator.setUsername(normalizedUsername);
        administrator.setPasswordHash(passwordEncoder.encode(password));
        administrator.setName(name == null || name.isBlank() ? "系统管理员" : name.trim());
        administrator.setRole(UserRole.ADMIN);
        administrator.setStatus(UserStatus.ACTIVE);
        userMapper.insert(administrator);
        operationLogService.record(null, "BOOTSTRAP_ADMIN_CREATED", "USER", administrator.getId());
    }
}
