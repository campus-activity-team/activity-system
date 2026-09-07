# API 基础约定

所有业务接口使用 `/api` 前缀，成功响应结构如下：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

Phase 1 提供：

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

默认账号只在 `dev` Profile 下初始化，生产环境不会创建这些账号。

错误响应使用对应 HTTP 状态码，并保持相同 JSON 结构。认证、活动、报名、签到、反馈接口会在后续 Phase 添加。
