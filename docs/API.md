# API 基础约定

所有业务接口使用 `/api` 前缀，成功响应结构如下：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## Phase 1 基础接口

- `GET /api/health`：返回后端服务状态

## Phase 2 认证接口

- `POST /api/auth/register`：注册普通用户，密码只以 BCrypt 哈希保存
- `POST /api/auth/login`：登录并返回 JWT，后续请求使用 `Authorization: Bearer <token>`
- `POST /api/auth/logout`：无状态 JWT 的客户端退出确认
- `GET /api/auth/me`：读取当前登录用户，不返回密码字段

开发环境会自动创建以下账号（仅用于本地演示）：

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| ADMIN | `admin` | `admin123` |
| ORGANIZER | `organizer` | `organizer123` |
| USER | `student` | `student123` |
| USER | `student2` | `student2123` |
| USER | `student3` | `student3123` |

默认账号只在 `dev` Profile 下初始化，生产环境不会创建这些账号。

开发环境还会为 `organizer` 自动创建一组测试活动，覆盖 `DRAFT`、`PENDING_REVIEW`、`REJECTED`、`APPROVED`、`PUBLISHED`、`ONGOING` 和 `ENDED` 状态，并为进行中的签到演示活动创建两个测试报名记录。

## Phase 3 活动与审核接口

- `GET /api/activities`：公开查询已发布、进行中和已结束的活动，可按名称或地点搜索
- `GET /api/activities/{id}`：读取公开活动详情
- `GET /api/organizer/activities`：组织者查看自己的活动
- `GET /api/organizer/activities/{id}`：组织者查看活动详情
- `POST /api/activities`：创建活动草稿
- `PUT /api/activities/{id}`：编辑草稿或已驳回活动
- `DELETE /api/activities/{id}`：删除草稿或已驳回活动
- `POST /api/activities/{id}/submit`：提交管理员审核
- `GET /api/admin/reviews`：管理员查看待审核活动
- `POST /api/admin/activities/{id}/approve`：管理员通过审核
- `POST /api/admin/activities/{id}/reject`：管理员驳回活动
- `POST /api/admin/activities/{id}/publish`：管理员发布已通过活动
- `POST /api/admin/activities/{id}/unpublish`：管理员下架已发布或进行中活动

活动状态会由后台定时任务根据开始和结束时间自动从 `PUBLISHED` 推进至 `ONGOING` 或 `ENDED`。

## Phase 4 报名与签到接口

- `GET /api/registrations/mine`：当前用户查看自己的报名记录及签到状态
- `GET /api/registrations/activities/{activityId}`：当前用户查看指定活动的报名状态
- `POST /api/registrations/activities/{activityId}`：报名活动，服务端按容量加锁校验
- `DELETE /api/registrations/activities/{activityId}`：活动开始前取消报名
- `GET /api/organizer/activities/{activityId}/registrations`：组织者或管理员查看报名名单
- `GET /api/organizer/activities/{activityId}/attendances`：组织者或管理员查看签到记录
- `POST /api/organizer/activities/{activityId}/checkin-token`：为进行中活动生成 60 秒有效签到令牌
- `POST /api/checkins`：已报名用户使用当前令牌签到；重复签到或过期令牌会返回业务错误

错误响应使用对应 HTTP 状态码，并保持相同 JSON 结构。反馈业务接口仍将在后续阶段添加。
