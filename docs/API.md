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

错误响应使用对应 HTTP 状态码，并保持相同 JSON 结构。认证、活动、报名、签到、反馈接口会在后续 Phase 添加。
