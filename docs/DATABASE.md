# 数据库说明

`database/init.sql` 负责初始化 `activity_system` 数据库及核心表：

- `users`
- `activities`
- `registrations`
- `checkin_tokens`
- `attendances`
- `feedbacks`
- `operation_logs`

报名和签到表已设置活动/用户唯一约束，容量与评分也有数据库约束。报名接口使用事务和行锁保护容量并发更新，签到令牌保存过期时间，活动状态由后台定时任务自动推进。反馈表已完成结构预留，业务接口仍待后续阶段实现。
