# Docker 多服务共享 MySQL 部署与接入通用指南
(Shared MySQL Infrastructure Guide for Multi-Service Deployments)

> **适用场景**：单台云服务器（如 2C2G / 2C4G 等小规格主机）同时部署多个 Docker 微服务或 Web 应用。通过共享单个 MySQL 8 容器，避免每个应用自建 MySQL 导致的内存爆满（OOM）与磁盘冗余。

---

## 目录
1. [架构原理与收益](#一-架构原理与收益)
2. [公共基础设施初始化 (只需执行一次)](#二-公共基础设施初始化-只需执行一次)
3. [新服务接入标准化流程 (后续每个新服务)](#三-新服务接入标准化流程-后续每个新服务)
4. [既有服务无损平滑迁移流程 (老服务改造专用)](#四-既有服务无损平滑迁移流程-老服务改造专用)
5. [日常运维与自动化定时备份](#五-日常运维与自动化定时备份)
6. [常见问题与故障排查 (FAQ)](#六-常见问题与故障排查-faq)

---

## 一、 架构原理与收益

### 1. 架构拓扑
```text
┌─────────────────────────────────────────────────────────────┐
│ 宿主机 (Host Server)                                        │
│                                                             │
│   Docker 共享网桥: shared-net                               │
│   ┌─────────────────────────────────────────────────────┐   │
│   │                                                     │   │
│   │           ┌───────────────────────────────┐         │   │
│   │           │ 公共数据库: shared-mysql      │         │   │
│   │           │ 端口: 3306 (容器内)           │         │   │
│   │           │ 统一内存池 (256M~512M)        │         │   │
│   │           └───────┬───────────────┬───────┘         │   │
│   │                   │               │                 │   │
│   │         ┌─────────┴────────┐  ┌───┴─────────────┐   │   │
│   │         │ 业务 A (fund-app)│  │ 业务 B (blog-app)│   │   │
│   │         │ 库: fund_val...  │  │ 库: blog_db     │   │   │
│   │         │ 用户: fund_user  │  │ 用户: blog_user │   │   │
│   │         └──────────────────┘  └─────────────────┘   │   │
│   └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│                          ▼ 挂载持久化                       │
│             /opt/shared-infra/mysql/data                    │
└─────────────────────────────────────────────────────────────┘
```

### 2. 核心收益
- **内存骤降 60% ~ 75%**：无需为每个应用维持各自的 InnoDB 缓冲池与系统线程开销。单个公共实例总内存可稳固控制在 350MB ~ 600MB 之间。
- **权限与数据物理隔离**：每个应用拥有独立数据库（Database/Schema）与受限数据库用户，禁止越权跨库操作。
- **备份与维护集约化**：统一维护一份 `my.cnf`、统一挂载点，编写一个简单的 Cron 脚本即可全自动完成全库备份。

---

## 二、 公共基础设施初始化 (只需执行一次)

### 步骤 1：创建通用的 Docker 桥接网络
在服务器终端执行一次：
```bash
docker network create shared-net
```

### 步骤 2：创建公共基础设施目录结构
建议将所有公共底座统一部署在 `/opt/shared-infra/` 目录下：
```bash
sudo mkdir -p /opt/shared-infra/mysql/data
sudo mkdir -p /opt/shared-infra/mysql/conf.d
sudo mkdir -p /opt/shared-infra/mysql/backup
cd /opt/shared-infra/mysql
```

### 步骤 3：编写低内存优化配置 `conf.d/my.cnf`
针对低配置云主机，必须对 MySQL 8 的默认高耗内存项进行深度调优：
```bash
sudo tee conf.d/my.cnf <<-'EOF'
[mysqld]
# 1. 内存瘦身关键：关闭性能度量监控（直接省下 150MB ~ 250MB 物理内存）
performance_schema = OFF

# 2. 统一缓冲池大小（根据服务器总物理内存分配）：
# 2G 机器建议 256M；4G 机器建议 384M ~ 512M
innodb_buffer_pool_size = 256M

# 3. 适度控制并发连接总数，防止多个应用突发瞬时连接撑爆内存
max_connections = 120

# 4. 全局字符集与排序规则（全面支持 Emoji 与中文符号）
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci

# 5. 磁盘防护：自动清理 3 天前的二进制日志，防止磁盘被 binlog 撑满
binlog_expire_logs_seconds = 259200

# 6. 时区设置
default-time-zone = '+08:00'
EOF
```

### 步骤 4：编写编排文件 `docker-compose.yml`
```bash
sudo tee docker-compose.yml <<-'EOF'
services:
  shared-mysql:
    image: mysql:8.0
    container_name: shared-mysql
    restart: always
    environment:
      # 请务必修改为你自己的超强根密码
      MYSQL_ROOT_PASSWORD: YourSharedRootPassword123!
    volumes:
      - ./data:/var/lib/mysql
      - ./conf.d:/etc/mysql/conf.d
      - ./backup:/backup
    networks:
      - shared-net
    deploy:
      resources:
        limits:
          memory: 600M   # 设置硬性内存上限

networks:
  shared-net:
    external: true
EOF
```

### 步骤 5：启动共享数据库
```bash
docker compose up -d
# 验证启动状态
docker compose ps
docker compose logs -f
```

---

## 三、 新服务接入标准化流程 (后续每个新服务)

当您开发完一个新项目准备上线时，只需简单两步完成接入：

### 步骤 1：在 `shared-mysql` 中分配独立库与专用账号

以接入新业务应用 `my_new_service` 为例：
```bash
# 登录共享 MySQL 命令行
docker exec -it shared-mysql mysql -u root -p'YourSharedRootPassword123!'
```
执行以下 SQL（注意替换库名、用户名与强密码）：
```sql
-- 1. 创建该服务专属的数据库
CREATE DATABASE IF NOT EXISTS my_service_db 
  DEFAULT CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

-- 2. 创建该服务专用的账号（限制只能访问自己的库，杜绝越权）
CREATE USER 'my_service_user'@'%' IDENTIFIED BY 'MyServiceSecretPwd789!';

-- 3. 仅授权该数据库的全部权限
GRANT ALL PRIVILEGES ON my_service_db.* TO 'my_service_user'@'%';

-- 4. 刷新权限
FLUSH PRIVILEGES;
EXIT;
```

---

### 步骤 2：在新服务的 `docker-compose.yml` 中接入共享网络

新服务的 `docker-compose.yml` 中**不再需要定义任何 `mysql` service 或 `mysql-data` volume**，只需要：
1. 声明 `external: true` 的 `shared-net`；
2. 数据库连接主机名直接写容器名 `shared-mysql`，端口 `3306`。

#### 标准模版示例：
```yaml
services:
  my-app:
    build: .
    container_name: my-app
    restart: always
    ports:
      - "127.0.0.1:8081:8080"
    environment:
      - TZ=Asia/Shanghai
      # 主机名直接填写 shared-mysql，走 Docker 内部虚拟局域网通信，无需暴露 3306 到公网
      - DB_HOST=shared-mysql
      - DB_PORT=3306
      - DB_NAME=my_service_db
      - DB_USER=my_service_user
      - DB_PASSWORD=MyServiceSecretPwd789!
    networks:
      - shared-net
    deploy:
      resources:
        limits:
          memory: 512M

networks:
  shared-net:
    external: true
```

#### 各语言常见数据源连接串（直接拿来即用）：
- **Spring Boot (JDBC)**:
  `jdbc:mysql://shared-mysql:3306/my_service_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai`
- **Node.js (TypeORM / Sequelize / MySQL2)**:
  `host: 'shared-mysql', port: 3306, user: 'my_service_user', password: '...', database: 'my_service_db'`
- **Python (Django / SQLAlchemy)**:
  `mysql+pymysql://my_service_user:MyServiceSecretPwd789!@shared-mysql:3306/my_service_db?charset=utf8mb4`
- **Go (GORM)**:
  `my_service_user:MyServiceSecretPwd789!@tcp(shared-mysql:3306)/my_service_db?charset=utf8mb4&parseTime=True&loc=Local`

---

## 四、 既有服务无损平滑迁移流程 (老服务改造专用)

如果当前某个服务已经以独立 MySQL 容器运行且**积累了历史生产数据**，请严格按照以下步骤迁移，确保**零数据丢失、随时可秒级回滚**：

### 步骤 1：零风险导出旧容器数据
在服务器终端执行导出（全程只读，不影响线上运行）：
```bash
# 假设旧容器名叫 old-service-mysql，旧数据库名叫 old_service_db
docker exec -i old-service-mysql mysqldump -u root -p'OldRootPassword' \
  --default-character-set=utf8mb4 \
  --single-transaction \
  --quick \
  old_service_db | sudo tee /opt/old_service_backup.sql > /dev/null

# 确认导出文件非空且大小正常
ls -lh /opt/old_service_backup.sql
```

### 步骤 2：在 `shared-mysql` 中建库和用户
```bash
docker exec -it shared-mysql mysql -u root -p'YourSharedRootPassword123!'
```
```sql
CREATE DATABASE IF NOT EXISTS old_service_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'old_service_user'@'%' IDENTIFIED BY 'OldServicePwd456!';
GRANT ALL PRIVILEGES ON old_service_db.* TO 'old_service_user'@'%';
FLUSH PRIVILEGES;
EXIT;
```

### 步骤 3：将数据完整导入共享库
```bash
docker exec -i shared-mysql mysql -u root -p'YourSharedRootPassword123!' old_service_db < /opt/old_service_backup.sql
```
**数据完整性验证**（检查表和行数是否一致）：
```bash
docker exec -i shared-mysql mysql -u root -p'YourSharedRootPassword123!' -e "
  SELECT table_name, table_rows 
  FROM information_schema.tables 
  WHERE table_schema='old_service_db';
"
```

### 步骤 4：修改业务服务配置并重启
1. 修改该服务的 `docker-compose.yml`（参考第三节，接入 `shared-net`，指向 `shared-mysql:3306`，删掉旧 mysql service 定义）；
2. 执行重启：
   ```bash
   docker compose down
   docker compose up -d
   docker compose logs -f
   ```

### 步骤 5：平稳观察与安全清理
- 观察业务正常运行 24~48 小时；
- 确认全部数据无误后，删除旧 MySQL 孤立卷释放磁盘空间：
  ```bash
  # 查看旧数据卷
  docker volume ls
  # docker volume rm <旧容器的数据卷名>
  ```

---

## 五、 日常运维与自动化定时备份

### 1. 通用自动化备份脚本
在 `/opt/shared-infra/mysql` 目录下创建自动备份脚本 `backup.sh`：
```bash
sudo tee /opt/shared-infra/mysql/backup.sh <<-'EOF'
#!/bin/bash
BACKUP_DIR="/opt/shared-infra/mysql/backup"
DATE=$(date +%Y%m%d_%H%M%S)
ROOT_PASSWORD="YourSharedRootPassword123!"

mkdir -p ${BACKUP_DIR}

# 1. 执行全实例逻辑备份并使用 gzip 压缩
docker exec shared-mysql mysqldump -u root -p"${ROOT_PASSWORD}" \
  --all-databases \
  --single-transaction \
  --quick | gzip > "${BACKUP_DIR}/all_databases_${DATE}.sql.gz"

# 2. 自动删除 7 天以前的历史备份文件，防止占满磁盘
find ${BACKUP_DIR} -name "all_databases_*.sql.gz" -mtime +7 -exec rm -f {} \;

echo "[${DATE}] Backup completed successfully." >> ${BACKUP_DIR}/backup.log
EOF

sudo chmod +x /opt/shared-infra/mysql/backup.sh
```

### 2. 配置 Crontab 每日凌晨自动备份
```bash
# 打开定时任务编辑器
crontab -e
```
添加以下定时任务（每天凌晨 3:30 自动备份）：
```text
30 3 * * * /bin/bash /opt/shared-infra/mysql/backup.sh >/dev/null 2>&1
```

---

## 六、 常见问题与故障排查 (FAQ)

### Q1: 应用启动报错 `UnknownHostException: shared-mysql` 或无法连接？
- **原因**：应用容器没有加入 `shared-net` 网络，或者名字拼写错误。
- **解决**：检查应用的 `docker-compose.yml` 是否正确配置了 `networks: - shared-net` 以及底部声明了 `external: true`。使用以下命令检查网络连通性：
  ```bash
  docker network inspect shared-net
  ```
  在输出中确认 `shared-mysql` 和你的业务应用都在同一个 `Containers` 列表中。

### Q2: 出现 `Access denied for user 'xxx'@'%'` 权限拒绝？
- **原因**：账号授权的主机范围不匹配或密码错误。
- **解决**：创建用户时务必确保主机通配符为 `'%'`（例如 `'user'@'%'`），而不是 `'user'@'localhost'`。因为容器间通过桥接网络通信，来源 IP 是 Docker 网桥 IP（如 `172.x.x.x`），并非本地环回。

### Q3: 内存还是偏紧，如何实时查看各容器资源开销？
- **执行命令**：
  ```bash
  docker stats --no-stream
  ```
  可精确查看到 `shared-mysql` 以及各个业务应用的实时 CPU、内存开销与上限比。如果 MySQL 占用超过预期，可适度下调 `conf.d/my.cnf` 中的 `innodb_buffer_pool_size`（如由 `256M` 调为 `192M`）。
