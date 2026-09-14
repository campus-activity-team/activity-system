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

正式环境应使用 DNS 域名和 HTTPS（例如 `https://activities.example.edu`），让 Nginx 或网关将请求转发到前端容器，前端再通过 `/api` 代理访问后端。生产环境请使用独立的数据库密码和 JWT 密钥、关闭 `dev` Profile 演示数据，并限制数据库和 Redis 端口不要直接暴露到公网。
