# SQL 判题引擎 - 判题容器架构

## 1. 概述

判题容器是 SQL 判题引擎的核心执行环境，负责在隔离的 Docker 容器中执行学生提交的 SQL 代码，确保学生之间的环境互不影响。

---

## 2. 容器架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Docker Network (judge_net)                          │
│                                                                             │
│  ┌──────────────────┐         ┌─────────────────────────────────────────┐ │
│  │  judge-service   │         │         Judge Container                  │ │
│  │                  │◄──────►│                                         │ │
│  │  - Java Spring   │         │  ┌─────────────────────────────────┐   │ │
│  │  - RabbitMQ      │  3306   │  │      MySQL 8.0 Server           │   │ │
│  │  Consumer        │         │  │                                 │   │ │
│  │                  │         │  │  judge 用户                      │   │ │
│  └──────────────────┘         │  │  problem_xxx 数据库              │   │ │
│                               │  │                                 │   │ │
│  ┌──────────────────────────────┐│  └─────────────────────────────────┘   │ │
│  │    container-manager         ││                                         │ │
│  │    - 容器池管理              ││  OS: debian-slim                      │ │
│  │    - Docker API 调用         ││  资源: 1核 1GB                        │ │
│  │    - 健康检查                ││                                         │ │
│  └──────────────────────────────┘└─────────────────────────────────────────┘ │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │  Judge Container Pool (5个预启动容器)                                    │ │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐    │ │
│  │  │ Ctn-1  │  │ Ctn-2  │  │ Ctn-3  │  │ Ctn-4  │  │ Ctn-5  │    │ │
│  │  │ READY  │  │ IN_USE │  │ READY  │  │ READY  │  │ ERROR  │    │ │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘  └─────────┘    │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 容器镜像设计

### 3.1 基础信息

| 项目 | 值 |
|------|-----|
| 基础镜像 | `mysql:8.0` |
| 镜像名称 | `sql-judge/mysql-judge` |
| 镜像标签 | `1.0` |
| 镜像大小 | ~600MB |

### 3.2 Dockerfile

```dockerfile
FROM mysql:8.0

ENV MYSQL_ROOT_PASSWORD_FILE="/run/secrets/mysql_root_password"
ENV MYSQL_DATABASE="judge_db"

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        mysql-client \
    && rm -rf /var/lib/apt/lists/*

COPY conf/my.cnf /etc/mysql/conf.d/custom.cnf
COPY docker-entrypoint-initdb.d/* /docker-entrypoint-initdb.d/

EXPOSE 3306

CMD ["mysqld", "--user=mysql"]
```

### 3.3 MySQL 配置 (my.cnf)

```ini
[mysqld]
# 字符集
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci

# 网络（仅容器内访问）
bind-address = 127.0.0.1
port = 3306

# 内存限制
innodb_buffer_pool_size = 64M
innodb_log_file_size = 16M
max_connections = 50

# 超时
wait_timeout = 600
interactive_timeout = 600

# 数据包
max_allowed_packet = 16M

# 安全
local_infile = 0
skip_grant_tables = 0
skip_show_database = 1

# 禁用二进制日志
log_bin = 0
relay_log = 0
```

---

## 4. 用户权限设计

### 4.1 MySQL 用户

| 用户 | 权限 | 用途 |
|------|------|------|
| `root` | ALL | 容器管理器内部使用 |
| `judge` | `problem_%`.* | 学生 SQL 执行 |

### 4.2 judge 用户创建脚本

```sql
-- 创建 judge 用户
CREATE USER IF NOT EXISTS 'judge'@'%' IDENTIFIED BY 'temp_password';

-- 授予权限：只能操作 problem_ 开头的数据库
GRANT ALL PRIVILEGES ON `problem_%`.* TO 'judge'@'%';

-- 禁止 GRANT 权限（防止提权）
REVOKE GRANT OPTION ON `problem_%`.* FROM 'judge'@'%';

FLUSH PRIVILEGES;
```

### 4.3 权限限制效果

```
✓ 可以执行：CREATE TABLE ON problem_100.*
✓ 可以执行：INSERT ON problem_100.*
✓ 可以执行：SELECT ON problem_100.*
✗ 无法访问：mysql.user
✗ 无法访问：information_schema（完全）
✗ 无法执行：GRANT ON *.*（提权被禁止）
✗ 无法执行：SHUTDOWN
✗ 无法执行：SUPER
```

---

## 5. 容器生命周期

### 5.1 状态机

```
                    ┌─────────────────┐
                    │   NOT_CREATED   │
                    └────────┬────────┘
                             │ create()
                             ▼
                    ┌─────────────────┐
         ┌────────►│    CREATING     │◄────────┐
         │         └────────┬────────┘         │
         │ create() 失败   │   健康检查通过    │ destroy()
         │                  │                  │
         │                  ▼                  │
         │         ┌─────────────────┐        │
         │         │     READY       │────────┘
         │         │  (可用容器池)    │  destroy()
         │         └────────┬────────┘
         │                  │ acquire()
         │                  ▼
         │         ┌─────────────────┐
         │         │    IN_USE       │
         │         │  (正在判题)      │
         │         └────────┬────────┘
         │                  │ release()
         │                  ▼
         │         ┌─────────────────┐
         │         │   RESETTING     │ (删除数据库)
         │         └────────┬────────┘
         │                  │
         │         使用次数 < 100? ───► YES ──► READY (回到池中)
         │                  │
         │                  NO
         │                  ▼
         │         ┌─────────────────┐
         └─────────│    DESTROYING   │
                   └─────────────────┘
```

### 5.2 acquire 流程

```java
public ContainerInfo acquire(int problemId) {
    // 1. 从池中获取可用容器
    Container container = containerPool.acquire();

    // 2. 生成随机密码作为连接令牌
    String newPassword = generateRandomPassword();

    // 3. 重置 judge 用户密码
    container.executeSql("ALTER USER 'judge'@'%' IDENTIFIED BY '" + newPassword + "'");

    // 4. 创建题目数据库
    container.executeSql("CREATE DATABASE IF NOT EXISTS `problem_" + problemId + "`");

    // 5. 执行初始化 SQL
    String initSql = getProblemInitSql(problemId);
    if (initSql != null && !initSql.trim().isEmpty()) {
        container.executeSql(initSql);
    }

    // 6. 返回连接信息
    return ContainerInfo.builder()
            .containerId(container.getId())
            .containerName(container.getName())
            .ipAddress(container.getIp())
            .mysqlPort(3306)
            .mysqlUser("judge")
            .connectionToken(newPassword)  // 临时令牌
            .tokenExpiresAt(calculateExpiry(problemId))
            .status("IN_USE")
            .build();
}
```

**实际实现说明**:
- 容器获取使用 HTTP POST `/api/container/acquire` 接口
- 请求体包含 `problemId` 和可选的 `timeout` 参数
- 返回的 `connectionToken` 即为临时生成的 judge 用户密码
- Token 有效期根据题目难度计算：EASY(10分钟), MEDIUM(30分钟), HARD(60分钟)

### 5.3 release 流程

```java
public void release(String containerId) {
    Container container = containerPool.get(containerId);

    // 1. 删除题目数据库
    for (String db : container.listDatabases("problem_%")) {
        container.executeSql("DROP DATABASE IF EXISTS `" + db + "`");
    }

    // 2. 检查使用次数
    container.incrementUsageCount();
    if (container.getUsageCount() >= 100) {
        // 3. 超过100次，销毁容器
        container.destroy();
        containerPool.spawnNewContainer(); // 补充新容器
    } else {
        // 4. 重置后放回池中
        container.reset();
        containerPool.release(container);
    }
}
```

---


## 6. 生产环境优化项

> **📌 设计意图记录**
>
> 以下为生产环境优化项，当前开发环境采用简化策略。
> 记录设计意图但不强制现在就实现。

### 6.1 容器池自愈策略

当前采用**"申请时检查 + 同步重建"**的简化策略：

```java
public ContainerInfo acquire(int problemId) {
    // 1. 尝试从池中获取可用容器
    Container container = containerPool.acquire();
    
    // 2. 健康检查
    if (!container.isHealthy()) {
        // 容器不健康，同步重建
        container.destroy();
        container = containerPool.spawnNewContainer();
    }
    
    // 3. 返回容器信息
    return container;
}
```

| 场景 | 当前处理 | 生产环境建议 |
|------|----------|--------------|
| 容器健康检查失败 | 销毁 + 同步重建新容器 | 后台线程定时扫描，自动重建 |
| 池中无容器 | 阻塞等待或抛异常 | 根据负载动态扩容 |

**说明**：不引入后台自愈线程，简化开发环境实现。生产环境可根据负载情况评估是否需要后台自愈机制。

---

## 7. 网络隔离

### 6.1 Docker 网络配置

```yaml
networks:
  judge_net:
    driver: bridge
    internal: false  # 允许 DNS 解析
```

### 6.2 容器网络配置

```yaml
services:
  judge-container:
    networks:
      - judge_net
    # 不暴露端口到宿主机
    # ports: 不设置
```

### 6.3 服务发现

```
judge-service 访问容器：
    ↓
jdbc:mysql://container-manager分配的容器名称:3306
    ↓
Docker DNS 解析容器名称为 IP
    ↓
连接到容器内 MySQL
```

---

## 8. 资源限制

### 7.1 Docker 资源限制

```yaml
services:
  judge-container:
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### 7.2 MySQL 资源限制

| 参数 | 值 | 说明 |
|------|-----|------|
| innodb_buffer_pool_size | 64MB | 最大缓存 |
| max_connections | 50 | 最大连接数 |
| max_allowed_packet | 16MB | 单包最大 |

### 7.3 按难度配置

| 难度 | CPU | 内存 | SQL 超时 | Token 有效期 |
|------|-----|------|----------|-------------|
| EASY | 0.5核 | 512MB | 10s | 10分钟 |
| MEDIUM | 1核 | 1GB | 30s | 30分钟 |
| HARD | 1核 | 1GB | 60s | 60分钟 |

---

## 9. 危险操作拦截

### 8.1 需要拦截的 SQL

| SQL 类型 | 示例 | 拦截原因 |
|----------|------|----------|
| 系统修改 | `DROP DATABASE mysql` | 破坏系统库 |
| 系统修改 | `ALTER SYSTEM` | 修改 MySQL 配置 |
| 用户管理 | `CREATE USER` | 创建新用户 |
| 权限管理 | `GRANT ALL` | 提权 |
| 连接管理 | `SHUTDOWN` | 关闭 MySQL |
| 文件操作 | `LOAD DATA INFILE` | 读取文件 |
| 进程管理 | `KILL` | 终止其他连接 |

### 8.2 拦截实现

```java
public class SqlSafetyValidator {

    private static final Set<String> BLOCKED_KEYWORDS = Set.of(
        "DROP DATABASE",
        "ALTER SYSTEM",
        "CREATE USER",
        "GRANT ALL",
        "REVOKE",
        "SHUTDOWN",
        "KILL",
        "LOAD DATA",
        "INTO OUTFILE"
    );

    public void validate(String sql) {
        String upperSql = sql.toUpperCase();

        for (String keyword : BLOCKED_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                throw new SqlSafetyException("禁止执行: " + keyword);
            }
        }

        // 检查是否为只读题目类型
        if (problemType == ProblemType.DQL && isWriteOperation(sql)) {
            throw new SqlSafetyException("DQL 题目不允许写操作");
        }
    }
}
```

---

## 10. 健康检查

### 9.1 容器健康检查

```bash
# 使用 mysqladmin ping
mysqladmin ping -h 127.0.0.1 -uroot -p$MYSQL_ROOT_PASSWORD
```

### 9.2 健康检查配置

```yaml
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "127.0.0.1", "-uroot", "-p${MYSQL_ROOT_PASSWORD}"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 30s
```

---

## 11. 日志与监控

### 10.1 容器日志

```yaml
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
```

### 10.2 监控指标

| 指标 | 说明 |
|------|------|
| `container_pool_size` | 当前池中容器数 |
| `container_available` | 可用容器数 |
| `container_in_use` | 使用中容器数 |
| `container_errors` | 错误容器数 |
| `container_acquire_time` | 容器获取耗时 |
| `container_usage_count` | 容器使用次数 |

---

## 12. 构建与部署

### 11.1 构建镜像

```bash
cd services/container-manager/judge-container
docker build -t sql-judge/mysql-judge:1.0 .
```

### 11.2 推送到 registry

```bash
docker tag sql-judge/mysql-judge:1.0 your-registry.com/sql-judge/mysql-judge:1.0
docker push your-registry.com/sql-judge/mysql-judge:1.0
```

### 11.3 预热容器池

```bash
# 启动时预热 5 个容器
for i in {1..5}; do
    docker run -d \
        --name judge-container-$i \
        --network judge_net \
        -e MYSQL_ROOT_PASSWORD=secret \
        sql-judge/mysql-judge:1.0
done
```

---

## 13. 故障处理

### 12.1 容器崩溃

```
检测：健康检查连续失败 5 次
    ↓
标记容器为 ERROR 状态
    ↓
从池中移除
    ↓
自动启动新容器补充池
    ↓
记录日志，告警通知
```

### 12.2 MySQL 崩溃

```java
try {
    container.executeSql(sql);
} catch (MySQLStatementException e) {
    if (e.getErrorCode() == 2013) { // Lost connection
        // 容器 MySQL 崩溃，销毁并重建
        containerManager.destroyContainer(containerId);
        throw new ContainerCrashException();
    }
}
```

### 12.3 磁盘空间不足

```bash
# 监控磁盘使用
docker stats --no-stream

# 清理策略
- 每次 release 时删除所有 problem_* 数据库
- 设置 MySQL innodb_log_file_size 限制
- 定期清理 Docker 日志
```
