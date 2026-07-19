# woqianne-app-backend

“我钱呢”记账应用的后端服务，提供邮箱验证码登录、JWT 鉴权和账目云端同步能力。

## 主要功能

- 发送邮箱验证码并完成无密码登录
- 首次登录时自动创建用户
- 使用 JWT 保护同步接口
- 上传和下载增量账目数据
- 基于更新时间的最后写入优先策略
- 使用软删除同步删除状态
- 提供服务健康检查接口

## 技术栈

- Java 17
- Javalin 6
- SQLite
- Maven
- Docker / Docker Compose

## API 概览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/auth/send-code` | 发送邮箱验证码 |
| `POST` | `/api/auth/login` | 使用验证码登录并获取 JWT |
| `POST` | `/api/sync/upload` | 上传本地账目变更 |
| `GET` | `/api/sync/download?since=...` | 下载指定时间后的账目变更 |
| `GET` | `/api/health` | 检查服务状态 |

## 本地运行

环境要求：JDK 17 和 Maven。

```bash
export LUCKY_JWT_SECRET=replace-with-a-long-random-secret
mvn clean package
java -jar target/lucky-server-2.0.0.jar
```

服务默认监听 `8080` 端口，SQLite 数据保存在 `data/lucky.db`。

## Docker 部署

在项目根目录创建 `.env`，并按实际环境填写：

```dotenv
LUCKY_PORT=8080
LUCKY_JWT_SECRET=replace-with-a-long-random-secret
LUCKY_SMTP_HOST=smtp.example.com
LUCKY_SMTP_PORT=465
LUCKY_SMTP_USERNAME=your-smtp-account
LUCKY_SMTP_PASSWORD=your-smtp-password
LUCKY_SMTP_FROM=Lucky
```

启动服务：

```bash
docker compose up -d --build
curl http://localhost:8080/api/health
```

Compose 会将宿主机的 `./data` 挂载到容器，用于持久化 SQLite 数据。

## 配置项

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `LUCKY_PORT` | `8080` | HTTP 服务端口 |
| `LUCKY_JWT_SECRET` | 开发占位值 | JWT 签名密钥，生产环境必须显式覆盖 |
| `LUCKY_SMTP_HOST` | `smtpdm.aliyun.com` | SMTP 服务器地址 |
| `LUCKY_SMTP_PORT` | `465` | SMTP 服务器端口 |
| `LUCKY_SMTP_USERNAME` | 空 | SMTP 用户名 |
| `LUCKY_SMTP_PASSWORD` | 空 | SMTP 密码 |
| `LUCKY_SMTP_FROM` | `Lucky` | 发件人名称或地址 |

## 相关项目

- [woqianne-app](https://github.com/77zane/woqianne-app)：UniApp 前端应用

## 安全说明

- 生产环境必须使用足够长且随机的 `LUCKY_JWT_SECRET`。
- 不要提交 `.env`、SMTP 密码、数据库文件或其他真实凭证。
- 建议在反向代理层启用 HTTPS、访问日志和请求频率限制。
