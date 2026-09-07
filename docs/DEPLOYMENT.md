# 部署说明

开发环境使用根目录的 `docker-compose.yml`，包含 MySQL、Redis、Spring Boot 后端和 Nginx 前端四个服务。

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

首次启动时 MySQL 会执行 `database/init.sql`。MySQL 数据保存在 `mysql-data` 卷，Redis 数据保存在 `redis-data` 卷。生产环境必须替换 Compose 中的数据库密码，并通过环境变量注入密钥，不要将敏感配置提交到仓库。
