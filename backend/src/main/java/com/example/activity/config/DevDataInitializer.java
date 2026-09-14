package com.example.activity.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.activity.entity.Activity;
import com.example.activity.entity.ActivityStatus;
import com.example.activity.entity.Registration;
import com.example.activity.entity.RegistrationStatus;
import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.entity.UserStatus;
import com.example.activity.mapper.ActivityMapper;
import com.example.activity.mapper.RegistrationMapper;
import com.example.activity.mapper.UserMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private static final String ONGOING_ACTIVITY_TITLE = "【测试】现场签到演示活动";

    private final UserMapper userMapper;
    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final PasswordEncoder passwordEncoder;

    public DevDataInitializer(
            UserMapper userMapper,
            ActivityMapper activityMapper,
            RegistrationMapper registrationMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userMapper = userMapper;
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createIfMissing("admin", "admin123", "系统管理员", null, UserRole.ADMIN);
        createIfMissing("organizer", "organizer123", "活动组织者", "ORG-001", UserRole.ORGANIZER);
        createIfMissing("student", "student123", "测试学生", "STU-001", UserRole.USER);
        createIfMissing("student2", "student2123", "测试学生二", "STU-002", UserRole.USER);
        createIfMissing("student3", "student3123", "测试学生三", "STU-003", UserRole.USER);

        User organizer = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, "organizer")
        );
        if (organizer != null) {
            seedActivities(organizer.getId());
            seedOngoingRegistrations();
        }
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

    private void seedActivities(Long organizerId) {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        renameLegacyActivityTitle(organizerId, "【测试】学生社团招新说明会（草稿）", "【测试】学生社团招新说明会");
        renameLegacyActivityTitle(organizerId, "【测试】科研经验分享会（待审核）", "【测试】科研经验分享会");
        renameLegacyActivityTitle(organizerId, "【测试】志愿服务培训（已驳回）", "【测试】志愿服务培训");
        renameLegacyActivityTitle(organizerId, "【测试】校园摄影采风（待发布）", "【测试】校园摄影采风");
        renameLegacyActivityTitle(organizerId, "【测试】往期演示活动（已结束）", "【测试】往期演示活动");
        renameLegacyActivityTitle(organizerId, "【测试】现场签到演示活动（进行中）", ONGOING_ACTIVITY_TITLE);

        createActivityIfMissing(
                organizerId,
                "【测试】校园 AI 应用入门讲座",
                "面向全校学生介绍人工智能基础概念、常用工具和学习路径，安排现场问答环节。",
                "大学生活动中心 A101",
                now.minusDays(2), now.plusDays(5),
                now.plusDays(7).withHour(19).withMinute(0), now.plusDays(7).withHour(21).withMinute(0),
                120, ActivityStatus.PUBLISHED, null, true, now.plusDays(10)
        );
        createActivityIfMissing(
                organizerId,
                "【测试】学生社团招新说明会",
                "介绍本学期社团招新安排、报名方式和优秀社团活动案例。",
                "大学生活动中心报告厅",
                now.plusDays(1), now.plusDays(12),
                now.plusDays(14).withHour(18).withMinute(30), now.plusDays(14).withHour(20).withMinute(0),
                80, ActivityStatus.DRAFT, null, false, null
        );
        createActivityIfMissing(
                organizerId,
                "【测试】科研经验分享会",
                "邀请高年级学生分享科研入门、文献阅读和项目实践经验。",
                "图书馆学术报告厅",
                now.plusDays(1), now.plusDays(19),
                now.plusDays(21).withHour(19).withMinute(0), now.plusDays(21).withHour(21).withMinute(0),
                200, ActivityStatus.PENDING_REVIEW, null, false, null
        );
        createActivityIfMissing(
                organizerId,
                "【测试】志愿服务培训",
                "面向新志愿者开展服务礼仪、安全须知和现场协作培训。",
                "学生服务中心 201",
                now.plusDays(2), now.plusDays(25),
                now.plusDays(28).withHour(14).withMinute(0), now.plusDays(28).withHour(16).withMinute(0),
                60, ActivityStatus.REJECTED, "请补充活动安全须知后重新提交。", false, null
        );
        createActivityIfMissing(
                organizerId,
                "【测试】校园摄影采风",
                "组织摄影爱好者进行校园主题采风，活动结束后开展作品交流。",
                "东校区南门集合",
                now.plusDays(3), now.plusDays(32),
                now.plusDays(35).withHour(9).withMinute(0), now.plusDays(35).withHour(12).withMinute(0),
                40, ActivityStatus.APPROVED, null, false, null
        );
        createActivityIfMissing(
                organizerId,
                "【测试】往期演示活动",
                "用于验证已结束活动在公开活动列表中的展示。",
                "大学生活动中心 B201",
                now.minusDays(30), now.minusDays(15),
                now.minusDays(14).withHour(14).withMinute(0), now.minusDays(14).withHour(16).withMinute(0),
                100, ActivityStatus.ENDED, null, false, null
        );
        createActivityIfMissing(
                organizerId,
                ONGOING_ACTIVITY_TITLE,
                "用于测试进行中状态、报名名单和 60 秒签到令牌。",
                "大学生活动中心 C102",
                now.minusDays(2), now.plusHours(2),
                now.minusHours(1), now.plusHours(2),
                30, ActivityStatus.ONGOING, null, false, null
        );
    }

    private void renameLegacyActivityTitle(Long organizerId, String legacyTitle, String currentTitle) {
        Activity legacyActivity = activityMapper.selectOne(Wrappers.<Activity>lambdaQuery()
                .eq(Activity::getOrganizerId, organizerId)
                .eq(Activity::getTitle, legacyTitle));
        if (legacyActivity == null || activityMapper.selectCount(Wrappers.<Activity>lambdaQuery()
                .eq(Activity::getOrganizerId, organizerId)
                .eq(Activity::getTitle, currentTitle)) > 0) {
            return;
        }
        legacyActivity.setTitle(currentTitle);
        activityMapper.updateById(legacyActivity);
    }

    private void createActivityIfMissing(
            Long organizerId,
            String title,
            String description,
            String location,
            LocalDateTime registrationStartTime,
            LocalDateTime registrationEndTime,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int capacity,
            ActivityStatus status,
            String reviewComment,
            boolean requireFeedback,
            LocalDateTime feedbackDeadline
    ) {
        if (activityMapper.selectCount(Wrappers.<Activity>lambdaQuery()
                .eq(Activity::getOrganizerId, organizerId)
                .eq(Activity::getTitle, title)) > 0) {
            return;
        }

        Activity activity = new Activity();
        activity.setTitle(title);
        activity.setDescription(description);
        activity.setOrganizerId(organizerId);
        activity.setLocation(location);
        activity.setStartTime(startTime);
        activity.setEndTime(endTime);
        activity.setRegistrationStartTime(registrationStartTime);
        activity.setRegistrationEndTime(registrationEndTime);
        activity.setCapacity(capacity);
        activity.setCurrentRegisteredCount(0);
        activity.setStatus(status);
        activity.setReviewComment(reviewComment);
        activity.setRequireFeedback(requireFeedback);
        activity.setFeedbackDeadline(feedbackDeadline);
        activityMapper.insert(activity);
    }

    private void seedOngoingRegistrations() {
        Activity activity = activityMapper.selectOne(Wrappers.<Activity>lambdaQuery()
                .eq(Activity::getTitle, ONGOING_ACTIVITY_TITLE));
        if (activity == null) {
            return;
        }
        for (String username : new String[]{"student", "student2"}) {
            User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
            if (user == null || registrationMapper.selectByActivityAndUser(activity.getId(), user.getId()) != null) {
                continue;
            }
            Registration registration = new Registration();
            registration.setActivityId(activity.getId());
            registration.setUserId(user.getId());
            registration.setStatus(RegistrationStatus.REGISTERED);
            registration.setRegisteredAt(LocalDateTime.now().minusMinutes(30));
            registrationMapper.insert(registration);
            activity.setCurrentRegisteredCount((activity.getCurrentRegisteredCount() == null ? 0 : activity.getCurrentRegisteredCount()) + 1);
        }
        activityMapper.updateById(activity);
    }
}
