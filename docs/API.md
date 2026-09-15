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

公开注册始终创建 `USER` 普通用户，不接受客户端传入角色，避免自行注册为发起者或管理员。

## 身份与权限接口

- `GET /api/organizer-applications/mine`：查看自己的发起者申请
- `POST /api/organizer-applications`：普通用户提交或在驳回后重新提交发起者申请
- `GET /api/admin/organizer-applications`：管理员查看全部发起者申请
- `POST /api/admin/organizer-applications/{id}/approve`：通过申请并将用户升级为 `ORGANIZER`
- `POST /api/admin/organizer-applications/{id}/reject`：驳回申请并填写审核意见
- `GET /api/admin/users`：管理员查看用户列表
- `POST /api/admin/users/{id}/role`：管理员验证当前密码后调整其他用户角色

管理员不能修改自己的角色，系统不能移除最后一名有效管理员。管理员不提供公开注册入口：首个管理员由部署引导创建，后续管理员应先注册普通账号，再由现有管理员提升。身份申请审批和角色变更都会写入 `operation_logs`。

开发环境会自动创建以下账号（仅用于本地演示）：

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| ADMIN | `admin` | `admin123` |
| ORGANIZER | `organizer` | `organizer123` |
| USER | `student` | `student123` |
| USER | `student2` | `student2123` |
| USER | `student3` | `student3123` |

默认账号只在 `dev` Profile 下初始化，生产环境不会创建这些账号。

开发环境还会为 `organizer` 自动创建一组测试活动，覆盖 `DRAFT`、`PENDING_REVIEW`、`REJECTED`、`APPROVED`、`PUBLISHED`、`ONGOING` 和 `ENDED` 状态，并为进行中的签到演示活动创建三个测试报名记录。

## Phase 3 活动与审核接口

- `GET /api/activities`：公开查询已发布、进行中和已结束的活动，可按名称或地点搜索
- `GET /api/activities/{id}`：读取公开活动详情
- `GET /api/organizer/activities`：组织者查看自己的活动
- `GET /api/organizer/activities/{id}`：组织者查看活动详情
- `POST /api/activities`：创建活动草稿
- `PUT /api/activities/{id}`：编辑草稿或已驳回活动
- `DELETE /api/activities/{id}`：删除草稿或已驳回活动
- `POST /api/activities/{id}/submit`：提交管理员审核
- `POST /api/activities/{id}/start`：组织者或管理员手动开启已发布活动，立即进入 `ONGOING` 状态
- `POST /api/activities/{id}/end`：组织者或管理员手动结束进行中活动，立即进入 `ENDED` 状态
- `POST /api/activities/{id}/withdraw`：活动发起者或管理员将审核中的活动撤回为草稿
- `POST /api/activities/{id}/cancel`：活动发起者或管理员填写原因取消待发布、已发布或进行中的活动
- `GET /api/admin/reviews`：管理员查看待审核活动
- `POST /api/admin/activities/{id}/approve`：管理员通过审核
- `POST /api/admin/activities/{id}/reject`：管理员驳回活动
- `POST /api/admin/activities/{id}/publish`：管理员发布已通过活动
- `POST /api/admin/activities/{id}/unpublish`：管理员下架已发布或进行中活动

活动状态会由后台定时任务根据开始和结束时间自动从 `PUBLISHED` 推进至 `ONGOING` 或 `ENDED`；公开、管理、报名和签到接口读取时也会即时校正过期状态，避免定时任务间隔造成显示延迟。组织者或管理员也可以手动开启尚未结束的已发布活动，或提前结束进行中的活动；自动状态推进仍作为兜底。手动开启和结束都会写入操作日志。

审核中的活动可以由发起者撤回并恢复为草稿。待发布、已发布和进行中的活动可填写原因取消，已结束活动不能取消；取消后停止报名与签到，保留原报名记录供参与者查看，并向全部有效报名者发送站内通知。管理员取消其他发起者的活动时，发起者也会收到通知。撤回和取消均写入操作日志。

公开活动只返回 `PUBLISHED`、`ONGOING`、`ENDED`，草稿、审核中、已驳回、待发布和已取消活动不会出现在公开列表或详情中。前端会根据报名起止时间显示“报名未开始”“报名中”或“报名已截止”，不会将所有已发布活动都显示为报名中。

## Phase 4 报名与签到接口

- `GET /api/registrations/mine`：当前用户查看自己的报名记录及签到状态
- `GET /api/registrations/activities/{activityId}`：当前用户查看指定活动的报名状态
- `POST /api/registrations/activities/{activityId}`：报名活动，服务端按容量加锁校验
- `DELETE /api/registrations/activities/{activityId}`：活动开始前取消报名
- `GET /api/organizer/activities/{activityId}/registrations`：组织者或管理员查看报名名单
- `GET /api/organizer/activities/{activityId}/registrations/export`：导出带签到信息的 UTF-8 CSV 名单
- `GET /api/organizer/activities/{activityId}/attendances`：组织者或管理员查看签到记录
- `POST /api/organizer/activities/{activityId}/attendances/{userId}`：为有效报名用户手动补签
- `DELETE /api/organizer/activities/{activityId}/attendances/{userId}`：撤销误签记录
- `GET /api/organizer/activities/{activityId}/checkin-anomalies`：组织者或管理员查看该活动的异常签到尝试
- `POST /api/organizer/activities/{activityId}/checkin-token`：为进行中活动生成 60 秒有效签到令牌，前端将其编码为动态二维码
- `POST /api/checkins`：已报名用户扫码后使用二维码中的短期令牌签到；启用位置签到的活动还需提交 `latitude`、`longitude`，后端计算与活动坐标的距离；重复提交会幂等返回已有签到结果，过期令牌会返回业务错误

组织者端不会直接要求参会者手动输入令牌。二维码链接会打开 `/checkin` 页面，登录后自动提交并停留展示“已完成签到”；组织者和管理员页面每 3 秒同步最新签到记录。本地演示时请使用手机可访问的局域网前端地址生成二维码，不要使用手机无法访问的 `localhost` 地址。

局域网前端来源必须在后端 CORS 白名单中；Docker Compose 默认允许常见私有网段，正式部署应通过 `APP_CORS_ALLOWED_ORIGIN_PATTERNS` 配置精确的 HTTPS 前端域名。

活动创建时可选填 `checkinLatitude`、`checkinLongitude` 和 `checkinRadiusMeters` 启用地理围栏。三项必须同时配置，半径范围为 20～2000 米。未配置时保持纯动态二维码签到。手机浏览器定位通常要求 HTTPS，局域网 HTTP 环境下不要为演示活动启用位置校验。

系统会记录可关联到活动的过期二维码、非签到时段、未报名、未提供定位和超出地理围栏尝试。异常记录采用独立事务保存，仅对应活动的组织者和管理员可查看；无法关联活动的随机无效令牌不会写入，避免被恶意请求灌满日志。

手动补签和撤销只允许活动发起者或管理员在活动进行中、结束后操作，并写入操作日志。已提交反馈的参与者不能再撤销签到，避免反馈资格和统计口径失真。导出文件包含姓名、用户名、学号、报名状态、报名时间、签到状态、签到时间和签到方式，并对表格公式开头字符进行转义。

## Phase 5 活动反馈接口

- `GET /api/feedbacks/activities/{activityId}`：参与者查看反馈资格和自己的反馈内容
- `PUT /api/feedbacks/activities/{activityId}`：已签到参与者提交或修改三项 1～5 分评分及意见
- `GET /api/organizer/activities/{activityId}/feedbacks`：活动发起者或管理员查看匿名反馈统计看板

活动必须开启反馈收集并已结束，参与者必须保持有效报名且成功签到。设置反馈截止时间后，超过截止时间只能查看已有反馈，不能新增或修改。统计看板提供报名数、签到数、反馈数、反馈率、三项平均分、总体评分分布和匿名意见，不返回评价者身份。

## 通知与操作日志接口

- `GET /api/notifications`：当前用户查看最近 100 条站内通知
- `GET /api/notifications/unread-count`：当前用户查看未读通知数量
- `POST /api/notifications/{id}/read`：将属于当前用户的指定通知标为已读
- `POST /api/notifications/read-all`：将当前用户全部通知标为已读
- `GET /api/admin/operation-logs`：管理员查询最近操作日志，可按 `operation`、`targetType`、`userId` 过滤，`limit` 范围为 1～200

报名成功、活动审核/发布/下架以及发起者申请审批结果会生成站内通知。后台每分钟检查一次未来 24 小时内开始的已发布活动，为保持有效报名的用户生成一次开场提醒；提醒使用唯一键去重，不会因轮询重复发送。活动审核、发布、下架、手动开启/结束、手动补签/撤销、身份审批和角色变更等关键动作会写入管理员可查询的操作日志。

错误响应使用对应 HTTP 状态码，并保持相同 JSON 结构。
