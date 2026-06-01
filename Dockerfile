# ====== 构建阶段 ======
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ====== 运行阶段 ======
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 安装 curl（用于健康检查）
RUN apk add --no-cache curl

# 创建数据目录
RUN mkdir -p /app/data

# 从构建阶段复制 JAR
COPY --from=builder /app/target/lucky-server-*.jar /app/lucky-server.jar

# 暴露 API 端口
EXPOSE 8080

# 启动命令
CMD ["java", "-jar", "/app/lucky-server.jar"]
