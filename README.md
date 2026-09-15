# 高校活动报名与签到系统

这是一个前后端分离的高校/机构活动报名与签到系统。目前已完成 **Phase 1-5**：项目基础、用户认证、活动生命周期与审核、活动报名与签到、活动反馈与统计。

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

手机扫码（局域网测试）：管理者可在动态二维码区域直接填写电脑局域网地址，例如 `http://192.168.1.20:5173`。设置会保存在当前浏览器，系统会拒绝生成 `localhost` 二维码。也可以在 `.env` 中通过 `VITE_PUBLIC_APP_URL` 配置默认值，然后重新构建前端：

```bash
docker compose up -d --build frontend
```

手机和电脑连接同一 Wi-Fi 后，先在手机浏览器打开该局域网地址确认能够访问，再生成二维码。若打不开，请检查系统防火墙是否允许 TCP 5173，以及 Wi-Fi 是否开启了客户端隔离。若使用 Vite 本地开发，请执行 `npm run dev -- --host 0.0.0.0`。

投入实际使用时，建议使用公网或校园网域名 + HTTPS，通过 Nginx/网关反向代理到前端容器；将手机访问地址或 `VITE_PUBLIC_APP_URL` 设置为该 HTTPS 地址，配置 DNS、防火墙和有效证书，并替换生产数据库密码、JWT 密钥，使用 `prod` Profile，关闭开发演示数据初始化。Compose 仅向局域网开放前端 `5173`，后端、MySQL 和 Redis 端口只绑定本机。

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
- Phase 3 活动草稿、审核、发布、下架和公开活动列表，支持手动开启/结束与按时间自动推进活动状态
- Phase 4 活动报名、取消报名、报名名单、60 秒动态二维码签到、签到记录和重复签到校验；组织者和管理员可手动开启活动
- Phase 5 已签到参与者在活动结束后提交或修改三项评分与匿名意见，组织者查看反馈率、平均分、评分分布和意见明细
- 身份治理：学生申请活动发起者、管理员审批、管理员角色授权与操作审计
- 现场管理：组织者导出报名/签到 CSV 名单，为有效报名用户手动补签或撤销误签，变更写入操作审计
- 活动可选配置签到坐标和 20～2000 米地理围栏，后端校验手机定位与活动地点距离
- 前端登录/注册、活动管理、报名签到、名单查看和浏览器令牌持久化；本地/局域网演示默认保持登录 30 天
- 可选的 DeepSeek 活动文案助手（需配置 AI 参数）

手机端登录后，顶部会显示“扫码签到”入口。使用 HTTPS 时，支持兼容浏览器直接调用后置摄像头识别二维码；局域网 HTTP 页面通常不能调用网页摄像头，请使用手机系统相机或微信扫码，扫码链接会在同一浏览器中复用已保存的登录状态。切换 `localhost`、局域网 IP 或公网域名会被浏览器视为不同站点，需要分别登录一次。

活动状态在后台定时推进，并在接口读取时即时校正，因此活动结束后不会因 30 秒轮询间隔继续显示“进行中”。公开页面仅展示已发布、进行中和已结束活动；报名状态按报名起止时间显示“报名未开始 / 报名中 / 报名已截止”。

开发 Profile 会自动初始化以下演示账号和活动数据。它们仅供本地开发，生产环境不会初始化；活动数据按名称幂等创建，反馈演示活动会持续确保测试资格和截止时间有效。

| 角色 | 用户名 | 密码 | 用途 |
| --- | --- | --- | --- |
| ADMIN | `admin` | `admin123` | 审核、发布和下架活动 |
| ORGANIZER | `organizer` | `organizer123` | 创建、编辑和提交活动 |
| USER | `student` | `student123` | 普通用户登录测试 |
| USER | `student2` | `student2123` | 多用户场景测试 |
| USER | `student3` | `student3123` | 多用户场景测试 |

同时会为 `organizer` 创建 7 个覆盖不同状态的测试活动，为进行中的签到演示活动创建三个测试报名记录，并为往期演示活动创建两名已签到参与者及一份匿名反馈。`student3` 默认已报名但未签到，可用于测试手动补签；`student` 可直接测试提交反馈，`student2` 可查看或修改预置反馈，`organizer` 可查看统计看板。

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
