# 部署说明

开发环境使用根目录的 `docker-compose.yml`，包含 MySQL、Redis、Spring Boot 后端和 Nginx 前端四个服务。

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

首次启动时 MySQL 会执行 `database/init.sql`。MySQL 数据保存在 `mysql-data` 卷，Redis 数据保存在 `redis-data` 卷。生产环境必须替换 Compose 中的数据库密码，并通过环境变量注入密钥，不要将敏感配置提交到仓库。

后端 JWT 使用 `JWT_SECRET` 签名。开发时在 `.env` 中设置不少于 32 个字符的随机值；生产环境必须使用独立的高强度密钥，并设置合理的 `JWT_EXPIRATION`。

## 手机与二维码访问

管理者可在动态二维码区域填写手机访问地址，浏览器会保存该设置；系统会拒绝 `localhost`、`127.0.0.1` 和 `0.0.0.0`。局域网示例为 `http://192.168.1.20:5173`。也可以用构建变量 `VITE_PUBLIC_APP_URL` 提供默认值：Docker Compose 在根目录 `.env` 中设置，Vite 开发服务器在 `frontend/.env.local` 中设置。

局域网测试时，手机和电脑必须连接同一网络，先用手机浏览器访问系统地址，再生成二维码。如果无法访问，请检查电脑局域网 IP 是否变化、操作系统防火墙是否放行 TCP 5173，以及路由器/AP 是否启用客户端隔离。Docker Compose 的前端端口监听所有网卡；后端、MySQL 和 Redis 仅绑定 `127.0.0.1`，手机通过前端 Nginx 的 `/api` 代理访问后端。

登录令牌默认有效 30 天并保存在当前浏览器的本地存储中，适合局域网演示时重复扫码。浏览器无痕模式、清理站点数据、切换浏览器或切换访问地址（例如从 `localhost` 换成 `192.168.x.x`）都会要求重新登录；这不是局域网故障。正式部署应在 `.env` 中设置更短的 `JWT_EXPIRATION`，并使用 HTTPS。

位置签到依赖浏览器 Geolocation API。多数手机浏览器只允许 HTTPS 页面获取定位，因此局域网 HTTP 演示应关闭活动的位置限制；部署有效 HTTPS 证书后再启用坐标和半径校验。GPS 存在误差且可能被伪造，地理围栏只能作为动态二维码的辅助防作弊手段。

从旧版本数据库升级时需执行一次 `database/migrations/001_add_checkin_location.sql`；全新数据库已由 `database/init.sql` 直接创建这些字段。

旧数据库还需按顺序执行 `database/migrations/002_create_checkin_anomalies.sql`、`database/migrations/003_create_organizer_applications.sql` 和 `database/migrations/004_create_feedbacks.sql`。

生产环境不应开放管理员注册。首次部署且数据库中尚无管理员时，可临时设置 `BOOTSTRAP_ADMIN_ENABLED=true`、`BOOTSTRAP_ADMIN_USERNAME`、`BOOTSTRAP_ADMIN_PASSWORD`（至少 12 位）和可选的 `BOOTSTRAP_ADMIN_NAME`；首个管理员创建成功后立即将 `BOOTSTRAP_ADMIN_ENABLED` 改回 `false` 并移除明文密码。后续管理员在“用户与权限”页面将已注册普通账号提升为管理员。

后端 CORS 默认允许本机和常见私有网段（`192.168.*`、`10.*`、`172.*`）的前端来源，解决手机浏览器发送局域网 `Origin` 时被拒绝的问题。正式环境应在 `.env` 将 `APP_CORS_ALLOWED_ORIGIN_PATTERNS` 收紧为实际 HTTPS 域名，例如 `https://activities.example.edu`，不要长期使用宽泛的通配规则。

正式环境应使用 DNS 域名和 HTTPS（例如 `https://activities.example.edu`），让 Nginx 或网关将请求转发到前端容器，前端再通过 `/api` 代理访问后端。生产环境请使用独立的数据库密码和 JWT 密钥、关闭 `dev` Profile 演示数据，并限制数据库和 Redis 端口不要直接暴露到公网。
