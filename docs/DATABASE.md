# 数据库说明

`database/init.sql` 负责初始化 `activity_system` 数据库及核心表：

- `users`
- `activities`
- `registrations`
- `checkin_tokens`
- `attendances`
- `feedbacks`
- `operation_logs`

报名和签到表已预留活动/用户唯一约束，容量与评分也有数据库约束。具体事务、锁和状态转换将在相应业务 Phase 中实现。
