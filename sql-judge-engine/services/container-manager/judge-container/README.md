# Judge Container

判题容器镜像，用于在隔离环境中执行学生提交的 SQL 代码。

## 架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Docker Network (judge_net)                │
│                                                              │
│  ┌─────────────┐              ┌─────────────────────────┐   │
│  │ judge-svc   │              │    Judge Container      │   │
│  │  (Java)     │◄────────────►│    (MySQL 8.0)         │   │
│  │             │   port 3306   │                       │   │
│  └─────────────┘              │  judge用户              │   │
│                                │  problem_xxx 数据库      │   │
│                                └─────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 安全设计

### 1. 用户权限隔离

- `judge` 用户仅被授予对 `problem_%` 数据库的权限
- 无法访问 `mysql` 系统库
- 无法执行 `GRANT` 操作（防止提权）
- `root` 用户仅供容器管理器内部使用

### 2. 网络隔离

- 容器加入自定义网络 `judge_net`
- MySQL 监听 `0.0.0.0:3306`（允许 judge_net 网络内的容器访问）
- judge-service 通过 Docker 服务发现访问容器

### 3. 资源限制

| 配置项 | 值 | 说明 |
|--------|-----|------|
| innodb_buffer_pool_size | 64MB | 限制内存占用 |
| max_connections | 50 | 限制并发连接 |
| max_allowed_packet | 16MB | 单条 SQL 最大大小 |

### 4. 危险操作拦截

在 judge-service 层拦截以下操作：
- `DROP DATABASE`（防止删除系统库）
- `ALTER SYSTEM`（防止修改 MySQL 配置）
- `SHUTDOWN`（防止关闭 MySQL）
- `KILL`（防止终止其他连接）

## 构建

```bash
cd services/container-manager/judge-container
docker build -t sql-judge/mysql-judge:1.0 .
```

## 运行

```bash
# 创建网络
docker network create judge_net

# 运行容器
docker run -d \
  --name judge-container-1 \
  --network judge_net \
  -e MYSQL_ROOT_PASSWORD=secret \
  -e MYSQL_DATABASE=judge_db \
  -m 1g \
  --cpus 1 \
  sql-judge/mysql-judge:1.0
```

## 环境变量

| 变量 | 必填 | 说明 |
|------|------|------|
| MYSQL_ROOT_PASSWORD | 是 | Root 密码 |
| MYSQL_DATABASE | 否 | 默认创建的数据库 |

## 端口

| 端口 | 说明 |
|------|------|
| 3306 | MySQL 端口（仅容器内访问） |

## 健康检查

容器内 MySQL 使用 `mysqladmin ping` 进行健康检查：
- 间隔：10秒
- 超时：5秒
- 重试次数：5次

## 容器生命周期

```
容器启动（MySQL 初始化）
    ↓
被容器管理器分配（acquire）
    ↓
重置 judge 用户密码（动态）
    ↓
创建/恢复题目数据库
    ↓
执行学生 SQL
    ↓
比对结果
    ↓
重置容器（删除 problem_xxx 数据库）
    ↓
归还容器池（release）
    ↓
被分配给下一个判题任务（reuse）
```

## 容器重置

每次判题完成后，执行以下重置操作：

```sql
-- 删除所有题目数据库
DROP DATABASE IF EXISTS `problem_1`;
DROP DATABASE IF EXISTS `problem_2`;
...

-- 可选：重置 judge 用户密码
ALTER USER 'judge'@'%' IDENTIFIED BY 'new_random_password';
FLUSH PRIVILEGES;
```

## 注意事项

1. **不要在生产环境使用默认镜像** - 务必修改 `MYSQL_ROOT_PASSWORD`
2. **定期轮换密码** - 建议每次容器重置时更换密码
3. **监控资源使用** - 确保 CPU/内存限制生效
4. **日志收集** - 配置 Docker 日志驱动便于排查问题
