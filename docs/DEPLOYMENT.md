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

二维码链接来自前端构建变量 `VITE_PUBLIC_APP_URL`。局域网测试时，Docker Compose 在根目录 `.env` 中设置它；Vite 开发服务器在 `frontend/.env.local` 中设置它。示例值为 `http://192.168.1.20:5173`，设置后执行 `docker compose up -d --build frontend` 或 `npm run dev -- --host 0.0.0.0`。手机和电脑必须连接同一网络，并确保防火墙放行 TCP 5173；不要使用 `localhost`，因为手机会把它解析为手机自身。

正式环境应使用 DNS 域名和 HTTPS（例如 `https://activities.example.edu`），让 Nginx 或网关将请求转发到前端容器，前端再通过 `/api` 代理访问后端。生产环境请使用独立的数据库密码和 JWT 密钥、关闭 `dev` Profile 演示数据，并限制数据库和 Redis 端口不要直接暴露到公网。
