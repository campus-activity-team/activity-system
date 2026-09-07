# 高校活动报名与签到系统

这是一个前后端分离的高校/机构活动报名与签到系统。目前完成 **Phase 1：项目基础**，后续认证、活动、报名、签到和反馈功能按阶段实现。

## 技术栈

- 后端：Java 17、Spring Boot 3.3、Spring MVC、MyBatis-Plus、MySQL 8、Redis 7
- 前端：Vue 3、TypeScript、Vite、Element Plus、Pinia、Axios
- 编排：Docker Compose

## 目录结构

```text
activity-system/
├── backend/       Spring Boot API 服务
├── frontend/      Vue 3 + TypeScript 前端
├── database/      MySQL 初始化脚本
├── docs/          API、数据库与部署文档
└── docker-compose.yml
```

## 环境要求

- Docker Desktop 及 Compose v2（推荐，直接启动完整基础环境）
- 本地开发：Java 17、Maven 3.9+、Node.js 20+、npm 10+

## Docker 启动

在 `activity-system` 目录执行：

```bash
cp .env.example .env
# 编辑 .env，替换两个数据库密码
docker compose up -d --build
```

- 前端：http://localhost:5173
- 后端健康检查：http://localhost:8080/api/health
- MySQL：localhost:3306，数据库 `activity_system`
- Redis：localhost:6379

停止服务：

```bash
docker compose down
```

如需同时删除开发数据，确认无误后执行 `docker compose down -v`。

## 本地启动

先启动 MySQL 与 Redis，再分别执行：

```bash
cd backend
mvn spring-boot:run
```

```bash
cd frontend
npm install
npm run dev
```

当前开发容器连接参数通过 `.env` 和 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、`REDIS_PORT` 环境变量注入。`.env` 不应提交到版本库。

## 当前实现

- Spring Boot 应用入口与 `/api/health` 健康检查
- 统一 `ApiResponse` 返回结构：`code`、`message`、`data`
- 参数校验、业务异常和未知异常的统一处理，不向客户端返回堆栈
- MySQL/Redis 配置及完整核心业务表初始化脚本
- Vue 3/TypeScript/Vite 前端壳、API 客户端和后端状态检查页
- Docker Compose、后端/前端生产镜像配置
- Phase 2 用户注册、登录、退出、JWT 认证和角色权限基础设施
- 前端登录/注册页面、路由守卫和浏览器令牌持久化

开发 Profile 会自动初始化演示账号：`admin/admin123`、`organizer/organizer123`、`student/student123`。这些账号仅供本地开发，生产环境不会初始化。

## 验证

```bash
cd backend && mvn test
cd frontend && npm run build
```

若本机没有 Java/Maven 但已安装 Docker，可使用 Docker Compose 进行构建；若两者都未安装，需要先准备对应环境。

## 认证请求示例

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"student","password":"student123"}'
```

将返回的 `data.token` 放入后续请求头：

```text
Authorization: Bearer <token>
```
