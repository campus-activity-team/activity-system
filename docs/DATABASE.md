# 数据库说明

`database/init.sql` 负责初始化 `activity_system` 数据库及核心表：

- `users`
- `activities`
- `registrations`
- `checkin_tokens`
- `attendances`
- `feedbacks`
- `notifications`
- `operation_logs`

报名、签到和反馈表均设置活动/用户唯一约束，容量与评分也有数据库约束。报名接口使用事务和行锁保护容量并发更新，签到令牌保存过期时间，活动状态由后台定时任务自动推进。活动取消时在 `activities` 保存取消原因、时间和操作人，并保留报名记录。反馈只允许活动结束后由已签到参与者提交，组织者看到的反馈明细不包含评价者身份。通知按用户、已读时间和创建时间建立索引，定时活动提醒及取消通知通过 `dedup_key` 唯一约束避免重复生成。
