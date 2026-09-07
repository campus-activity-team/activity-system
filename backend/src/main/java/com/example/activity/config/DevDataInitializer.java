package com.example.activity.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.entity.UserStatus;
import com.example.activity.mapper.UserMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public DevDataInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createIfMissing("admin", "admin123", "系统管理员", null, UserRole.ADMIN);
        createIfMissing("organizer", "organizer123", "活动组织者", "ORG-001", UserRole.ORGANIZER);
        createIfMissing("student", "student123", "测试学生", "STU-001", UserRole.USER);
    }

    private void createIfMissing(String username, String password, String name, String studentId, UserRole role) {
        if (userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, username)) > 0) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setName(name);
        user.setStudentId(studentId);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        userMapper.insert(user);
    }
}
