# 基金实时估值服务 (Fund Valuation Service)

基于 Spring Boot 3 + MySQL 8 + React 18 开发的 A 股公募基金盘中实时估值服务（前后端一体化单 Jar 分发 / Docker 容器化部署）。基于公开季报持仓及交易所盘中实时行情，估算自选基金当日净值走势，精度为专业参考级。

---

## 核心特性

- **基金类型全覆盖**：
  - **场内交易型基金 (ETF / LOF，如 510300、159919)**：直通交易所秒级连续竞价真实撮合成交行情，价格与涨跌幅 100% 精准无延迟。
  - **场外股票型 / 混合型**：依据季度公布前十大重仓股票，盘中每 2 分钟加权实时估算。
  - **纯债型基金（如 008559）**：通过“10年期国债收益率指数 / 国债指数”与组合久期模型动态估算，告别恒定为 0 的静态局限。
- **数据源高可用双引擎（跨域自愈）**：
  - 东方财富行情（主通道）+ 腾讯财经行情（`qt.gtimg.cn` 高可用兜底），彻底免疫海外/国内云服务器 IP 防爬限流。
  - 缺失行情按需即时懒加载，冷启动/闭市启动自动初次采样预热，杜绝全盘 null 报错。
- **用户安全与自选管理**：
  - 基于 JWT + BCrypt 强哈希加密的用户认证体系，自选列表端到端持久化隔离。
  - 支持拖拽排序（桌面端专属抓手手柄 `☰`，移动端长按平滑排序），垂直单轴吸附，永不横移穿框。
- **响应式极致体验**：
  - 移动端原生金融卡片流 + 涨跌药丸徽章（Pill Badges），桌面端多维财务数据大看板。
  - SWR（Stale-While-Revalidate）零闪烁缓存，详情页返回列表秒级回显。
  - ECharts 日内实时走势图（开盘基准锚点 + 分时脉冲心电图）。
  - Server-Sent Events (SSE) 盘中实时推送，免去手动频繁刷新的繁琐。
- **生产级网络加速与 SPA 路由适配**：
  - 内置服务端 Gzip 硬件流式压缩，传输体积缩减 75% 以上。
  - 全路由懒加载代码分割（Code Splitting），首屏秒开。
  - Spring Boot 自动 fallback 转发 index.html，彻底解决 SPA 刷新 404/内部错误。

---

## 技术栈与项目结构

- **后端**：Java 17+、Spring Boot 3.2.5、MyBatis-Plus 3.5.5、HikariCP、JJWT 0.12.5、Lombok
- **前端**：Node 18+、Vite 5+、React 18、TypeScript、Ant Design 5、@dnd-kit、ECharts 5
- **数据库**：MySQL 8.0+

```text
fund-valuation/
├── backend/          # Spring Boot 后端项目 (含内嵌前端静态资源)
│   ├── src/
│   └── pom.xml
├── frontend/         # React + Vite 前端工程
│   ├── src/
│   └── package.json
└── README.md
```

---

## 本地开发与调试

### 1. 数据库准备 (MySQL)
在本地 MySQL 执行建表脚本：
```bash
mysql -u root -p < backend/src/main/resources/db/schema.sql
```

### 2. 启动后端
在 `backend/src/main/resources/application.yml` 中配置数据库密码后：
```bash
cd backend
mvn spring-boot:run
```

### 3. 启动前端
```bash
cd frontend
npm install
npm run dev
```
浏览器打开 `http://localhost:5173`（本地已配置 proxy 自动代理 `/api` 至后端 `8080` 端口）。

---

## 生产环境服务器部署指南（Docker Compose 容器化推荐）

本方案针对各类云主机（腾讯云、阿里云、华为云、Azure 等，推荐配置 2C2G、2C4G 或 4C4G），采用**前端内嵌至 Spring Boot 单 Jar 包 + Docker 多服务隔离编排**架构。总内存开销克制在约 **800MB ~ 1GB**，给同机未来部署其他业务留出充沛空间。

### 第一步：本地一键联合打包

在本地电脑终端（Windows PowerShell）执行：

```powershell
# 1. 编译前端最新生产静态文件 (自动分包)
cd frontend
npm run build

# 2. 将编译产物全量覆盖至后端 static 资源目录
Remove-Item "..\backend\src\main\resources\static\*" -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item "dist\*" "..\backend\src\main\resources\static\" -Recurse -Force

# 3. 后端编译打包成独立可执行 Jar
cd ..\backend
mvn clean package -DskipTests
```
> 打包成功后获得：
> 1. 最终发布包：`backend/target/fund-valuation-0.1.0.jar`
> 2. 数据库脚本：`backend/src/main/resources/db/schema.sql`

---

### 第二步：云服务器控制台放通防火墙 / 安全组

登录云厂商（腾讯云/阿里云）控制台，在服务器的 **防火墙** 或 **安全组** 页面中添加入站规则：
- **放行端口**：`TCP:8888`（以及原有的 `TCP:22` 用于 SSH 登录）
- **授权策略**：允许，来源：`All`（或 `0.0.0.0/0`）

---

### 第三步：SSH 登录服务器初始化环境

```bash
# 1. 挂载 2GB Swap 虚拟内存（防突发 OOM 关键保活）
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 2. 安装 Docker 与 Compose
sudo apt update && sudo apt install -y docker.io curl
sudo systemctl enable --now docker

# （境内服务器必选）配置国内 Docker 镜像加速器（避免拉取 Docker Hub 失败）
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com",
    "https://docker.m.daocloud.io",
    "https://docker.1panel.live"
  ]
}
EOF
sudo systemctl daemon-reload && sudo systemctl restart docker

# 安装 Docker Compose 最新官方独立插件
sudo curl -SL "https://ghproxy.net/https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo ln -sf /usr/local/bin/docker-compose /usr/local/lib/docker/cli-plugins/docker-compose

# 3. 创建独立部署目录
sudo mkdir -p /opt/fund-app
sudo chown -R $USER:$USER /opt/fund-app
cd /opt/fund-app
```

---

### 第四步：在服务器准备配置文件

在 `/opt/fund-app` 目录下创建两个配置文件：

#### 1. 创建 `Dockerfile`
```bash
nano Dockerfile
```
写入内容：
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY app.jar /app/app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```

#### 2. 创建 `docker-compose.yml`
```bash
nano docker-compose.yml
```
写入内容（带 MySQL 存活探针健康检查，防止初次启动时序冲突）：
```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: fund-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: YourSecurePassword123!
      MYSQL_DATABASE: fund_valuation
    volumes:
      - mysql-data:/var/lib/mysql
      - ./schema.sql:/docker-entrypoint-initdb.d/schema.sql
    command:
      - --innodb_buffer_pool_size=128M
      - --max_connections=50
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-pYourSecurePassword123!"]
      interval: 5s
      timeout: 3s
      retries: 10
    deploy:
      resources:
        limits:
          memory: 400M

  app:
    build: .
    container_name: fund-app
    restart: always
    depends_on:
      mysql:
        condition: service_healthy
    ports:
      - "8888:8080"
    volumes:
      - /etc/localtime:/etc/localtime:ro
      - /etc/timezone:/etc/timezone:ro
    environment:
      - TZ=Asia/Shanghai
      - JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/fund_valuation?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=YourSecurePassword123!
    deploy:
      resources:
        limits:
          memory: 750M

volumes:
  mysql-data:
```
*(注意：请将两处 `YourSecurePassword123!` 修改为你设定的数据库密码)*

---

### 第五步：上传产物并启动服务

在本地电脑 PowerShell 终端执行上传：
```powershell
scp F:\Agent\Workspace\fund-valuation\backend\target\fund-valuation-0.1.0.jar dyf@<服务器IP>:/opt/fund-app/app.jar
scp F:\Agent\Workspace\fund-valuation\backend\src\main\resources\db\schema.sql dyf@<服务器IP>:/opt/fund-app/schema.sql
```

切回云服务器终端启动并追踪日志：
```bash
cd /opt/fund-app
sudo docker compose up -d --build
sudo docker compose logs -f app
```
当看到控制台输出：`Started ValuationApplication in xx.xxx seconds` 时，系统全量就绪。

---

### 第六步：访问与日常维护

- **浏览器打开**：`http://<服务器公网IP>:8888`
  - 首次使用请切换至「新用户注册」页签注册账号。
  - 在自选列表中输入基金代码（如 `110022` 易方达消费、`510300` 沪深300ETF、`008559` 永赢纯债）即刻体验。

- **日常更新（改完代码后只需2步）**：
  
  1. 本地重新执行第一步打包，并 `scp` 上传 `app.jar`；
  2. 服务器端执行无缓存构建并重启：
     ```bash
     cd /opt/fund-app
     sudo docker compose build --no-cache app
     sudo docker compose up -d --force-recreate app
   ```
  
- **查看资源占用**：
  ```bash
  sudo docker stats --no-stream
  ```

---

## 核心 API 概览

### 1. 账号鉴权 (`/api/auth`)
- `POST /api/auth/register`：用户注册，Body: `{"username": "u1", "password": "password123"}`
- `POST /api/auth/login`：用户登录，返回 JWT Token

### 2. 自选基金管理 (`/api/watchlist`)
*(请求头需附带 `Authorization: Bearer <token>`)*
- `GET /api/watchlist`：获取当前登录用户的自选基金实时估值列表
- `POST /api/watchlist`：添加基金进自选，Body: `{"fundCode": "110022"}`
- `DELETE /api/watchlist/{fundCode}`：删除指定自选
- `PUT /api/watchlist`：自选排序，Body: `{"fundCodes": ["110022", "510300"]}`
- `GET /api/watchlist/stream`：SSE 盘中长连接实时推送流（支持 `?token=` 鉴权）

### 3. 基金详情 (`/api/fund`)
- `GET /api/fund/{fundCode}`：查询单只基金详情（基本信息、实时估值、持仓穿透股票与今日涨跌幅、当日分时估值走势点集、纯债基金利率驱动参数）

### 4. 运维管理 (`/api/admin`)
- `POST /api/admin/intraday?force=true`：强制触发一次估值与全量打点流水线
- `POST /api/admin/refresh-nav`：强制立即同步最新官方收盘单位净值
- `POST /api/admin/bond-duration`：手动校准维护纯债基金的组合久期

---

## 免责声明

本服务提供的所有基金实时估值数据均基于公开披露的季报持仓及市场行情通过算法估算得出，**估值仅供参考，不构成任何投资建议与决策依据，实际净值请以基金公司官方公布的数据为准**。
