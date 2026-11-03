# ==========================================
# 阶段 1: 前端编译 (Node 22 LTS Alpine，支持 Vite 8 与 Rolldown)
# ==========================================
FROM node:22-alpine AS frontend-builder
WORKDIR /build/frontend

# 使用国内 npm 镜像加速构建
RUN npm config set registry https://registry.npmmirror.com

COPY frontend/package*.json ./
RUN npm install

COPY frontend/ ./
RUN npm run build

# ==========================================
# 阶段 2: 后端编译与打包 (Maven 3.9 + Java 17)
# ==========================================
FROM maven:3.9-eclipse-temurin-17 AS backend-builder
WORKDIR /build/backend

# 配置阿里云 Maven 镜像加速，极大提升国内云服务器构建速度
RUN mkdir -p /root/.m2 && \
    printf '<?xml version="1.0" encoding="UTF-8"?>\n\
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"\n\
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n\
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">\n\
  <mirrors>\n\
    <mirror>\n\
      <id>aliyunmaven</id>\n\
      <mirrorOf>*</mirrorOf>\n\
      <name>aliyun public</name>\n\
      <url>https://maven.aliyun.com/repository/public</url>\n\
    </mirror>\n\
  </mirrors>\n\
</settings>\n' > /root/.m2/settings.xml

COPY backend/pom.xml .
COPY backend/src ./src

# 将前端阶段生成的最新静态文件全量覆盖注入至后端的 static 静态目录
COPY --from=frontend-builder /build/frontend/dist/ ./src/main/resources/static/

RUN mvn clean package -DskipTests

# ==========================================
# 阶段 3: 最终运行镜像 (超轻量 JRE 17 Alpine)
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 时区设置为中国上海
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

COPY --from=backend-builder /build/backend/target/fund-valuation-0.1.0.jar /app/app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
