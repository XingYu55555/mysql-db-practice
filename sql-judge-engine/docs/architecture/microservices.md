# SQL 判题引擎 - 微服务架构设计

## 1. 架构概述

本项目采用微服务架构实现一个基于 MySQL 的在线 SQL 判题引擎，支持多用户（教师/学生）、多种 SQL 类型（DQL/DML/DDL/DCL）的自动判题。

### 1.1 系统特点

- **容器隔离**：每个学生答题过程在独立 Docker 容器中执行，确保环境隔离
- **容器池管理**：预启动容器池，支持快速获取/释放
- **文档驱动开发**：先定义 API 契约，再实现代码
- **多 Agent 协作**：架构师、API设计师、开发者、测试Agent分工配合

### 1.2 技术栈

| 组件 | 技术选型 | 版本 |
|------|----------|------|
| 后端框架 | Java Spring Boot | 3.2.3 |
| Java 版本 | Java | 17 |
| Spring Cloud | Spring Cloud | 2023.0.0 |
| 数据库 | MySQL 8.0 (业务库) + 容器内 MySQL (判题库) | 8.0 |
| 消息队列 | RabbitMQ | 3.x |
| 容器管理 | Docker SDK | - |
| API 文档 | OpenAPI 3.0 / Swagger | 3.0.3 |
| 服务网关 | Nginx (Gateway) | - |
| 容器编排 | Docker Compose (开发) / Kubernetes (生产) | - |
| AI 服务 | 阿里云百炼 (OpenAI 兼容) | - |

---

## 2. 微服务拆分

### 2.1 服务列表

| 服务名 | 职责 | 技术栈 | 端口 |
|--------|------|--------|------|
| **user-service** | 用户管理（教师/学生）、登录认证、JWT 签发 | Spring Boot + MySQL + JWT | 8081 |
| **problem-service** | 题目 CRUD、测试用例管理、初始化脚本存储 | Spring Boot + MySQL | 8082 |
| **submission-service** | 接收学生提交、记录提交历史、调用判题 | Spring Boot + RabbitMQ | 8083 |
| **judge-service** | 核心判题逻辑：调度容器、执行 SQL、结果比对 | Spring Boot + Docker SDK | 8084 |
| **container-manager** | 管理容器池、获取/释放容器、健康检查 | Spring Boot + Docker API | 8085 |
| **result-service** | 判题结果存储、成绩查询、排行榜 | Spring Boot + MySQL | 8086 |
| **gateway** | 统一入口、鉴权、限流、路由 | Nginx | 8080 |

### 2.2 各服务详细职责

#### user-service (用户服务)

**职责**：
- 用户注册（教师/学生）
- 用户登录，签发 JWT
- 用户信息查询
- 权限验证

**数据表**：
- `users`: 用户表

**API 边界**：
- 接收：注册请求、登录请求
- 输出：JWT Token、用户信息

---

#### problem-service (题目服务)

**职责**：
- 题目 CRUD（创建、读取、更新、删除）
- 初始化 SQL 脚本存储
- 教师设定标准答案（系统自动生成期望结果）

**数据表**：
- `problems`: 题目表（含期望结果）
- `tags`: 标签表
- `problem_tags`: 题目-标签关联表

**API 边界**：
- 接收：教师创建/更新题目请求
- 输出：题目详情、标签列表
- 下游调用：无（纯提供方）

---

#### submission-service (提交服务)

**职责**：
- 接收学生 SQL 提交
- 记录提交历史
- 将判题任务投递到消息队列
- 查询判题进度

**数据表**：
- `submissions`: 提交记录表

**API 边界**：
- 接收：学生提交 SQL
- 输出：提交记录、判题结果（异步）
- 下游调用：向 RabbitMQ 投递判题任务

---

#### judge-service (判题服务)

**职责**：
- 从消息队列消费判题任务
- 向 container-manager 申请容器
- 在容器内执行初始化 SQL
- 在容器内执行学生 SQL
- 结果比对（DQL 结果集、DML 快照、DDL 元数据、DCL 权限）
- AI 辅助 DDL/DCL 语义分析（判断学生答案与标准答案是否语义等价）
- 回写判题结果到 result-service

**AI 辅助判题**：
- 判断依据：**题目描述 + 教师标准答案 + 学生答案**
- 语义等价：学生答案与标准答案语义相同时判定为正确
- 适用场景：DDL/DCL 类型题目（结果比对不确定时）
- DQL/DML 类型：使用规则比对（结果集/数据快照），一般不需要 AI

**API 边界**：
- 接收：RabbitMQ 消息（判题任务）
- 输出：判题结果
- 下游调用：container-manager（申请容器）、result-service（回写结果）

---

#### container-manager (容器管理服务)

**职责**：
- 维护容器池（预启动 Docker 容器）
- 提供容器获取/释放 API
- 容器健康检查
- 容器内 MySQL 连接管理

**API 边界**：
- 接收：容器申请/释放请求
- 输出：容器连接信息（IP、端口、凭据）

**容器池配置**：
- 开发环境：5 个预启动容器
- 生产环境：根据负载动态调整
- 容器镜像：预装 MySQL 8.0，容器启动后 MySQL 服务自动运行

---

#### result-service (结果服务)

**职责**：
- 存储判题结果
- 提供成绩查询 API
- 生成排行榜
- 导出学生报告

**数据表**：
- `judge_results`: 判题结果表

**API 边界**：
- 接收：judge-service 回写的判题结果
- 输出：成绩列表、排行榜、报告数据

---

#### gateway (API 网关)

**职责**：
- 统一入口，所有请求经过网关
- JWT 鉴权
- 限流
- 路由到各微服务

**路由规则**：
| 路径前缀 | 目标服务 | 说明 |
|----------|----------|------|
| /api/user/** | user-service | |
| /api/problem/** | problem-service | |
| /api/submission/** | submission-service | |
| /api/result/** | result-service | 查询接口 |
| /api/judge/** | (内部服务) | 不通过 gateway 路由，仅 RabbitMQ 消费 |
| /api/container/** | (内部服务) | 不通过 gateway 路由，仅内部 HTTP 调用 |

**说明**：judge-service 和 container-manager 为内部服务，不暴露公网 API。

---

## 3. 数据库设计

### 3.1 业务库（业务数据存储）

#### users 表
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('TEACHER', 'STUDENT') NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### problems 表
```sql
CREATE TABLE problems (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    difficulty ENUM('EASY', 'MEDIUM', 'HARD') DEFAULT 'MEDIUM',
    sql_type ENUM('DQL', 'DML', 'DDL', 'DCL') NOT NULL,
    status ENUM('DRAFT', 'READY', 'PUBLISHED', 'ARCHIVED') DEFAULT 'DRAFT',
    teacher_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (teacher_id) REFERENCES users(id)
);
```

#### tags 表
```sql
CREATE TABLE tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    color VARCHAR(20) DEFAULT '#3B82F6',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### problem_tags 表
```sql
CREATE TABLE problem_tags (
    problem_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (problem_id, tag_id),
    FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);
```

#### test_cases 表
```sql
CREATE TABLE test_cases (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    problem_id BIGINT NOT NULL,
    name VARCHAR(100),
    init_sql TEXT,
    expected_result TEXT,
    expected_type ENUM('RESULT_SET', 'DATA_SNAPSHOT', 'METADATA', 'PRIVILEGE') NOT NULL,
    weight INT DEFAULT 1,
    FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE
);
```

#### submissions 表
```sql
CREATE TABLE submissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    problem_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    sql_content TEXT NOT NULL,
    status ENUM('PENDING', 'JUDGING', 'SUCCESS', 'FAILED') DEFAULT 'PENDING',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (problem_id) REFERENCES problems(id),
    FOREIGN KEY (student_id) REFERENCES users(id)
);
```

#### judge_results 表
```sql
CREATE TABLE judge_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    test_case_id BIGINT,
    score DECIMAL(5,2),
    status ENUM('CORRECT', 'INCORRECT', 'TIME_LIMIT', 'ERROR', 'AI_APPROVED', 'AI_REJECTED') NOT NULL,
    error_message TEXT,
    execution_time_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (submission_id) REFERENCES submissions(id),
    FOREIGN KEY (test_case_id) REFERENCES test_cases(id)
);
```

### 3.2 判题库（容器内 MySQL）

判题库在容器内运行，每个容器有独立的 MySQL 实例。

#### 题目数据库

教师创建题目时，系统会：
1. 在判题库创建对应的数据库（命名：`problem_{problem_id}`）
2. 执行初始化 SQL 脚本
3. 保存标准答案执行后的状态快照

学生答题时：
1. 在独立容器中创建/恢复题目数据库
2. 执行学生 SQL
3. 提取结果与快照比对

---

## 4. 服务间通信

### 4.1 同步通信（REST API）

| 调用方 | 被调用方 | 场景 |
|--------|----------|------|
| gateway | user-service | 用户认证 |
| gateway | problem-service | 题目查询 |
| gateway | submission-service | 提交答题 |
| gateway | result-service | 查询成绩 |
| submission-service | problem-service | 获取题目详情 |
| judge-service | container-manager | 申请/释放容器 |
| judge-service | result-service | 回写结果 |

### 4.2 异步通信（RabbitMQ）

| 队列名 | 生产者 | 消费者 | 消息内容 |
|--------|--------|--------|----------|
| judge-tasks | submission-service | judge-service | 判题任务 |

**说明**：
- 判题结果通过 HTTP API 回写到 result-service（不通过 RabbitMQ）
- RabbitMQ 仅用于判题任务的分发

### 4.3 RabbitMQ 详细配置

#### 4.3.1 交换机与队列声明

```python
# 主交换机
channel.exchange_declare(
    exchange='judge-exchange',
    exchange_type='direct',
    durable=True  # 持久化
)

# 主队列
channel.queue_declare(
    queue='judge-tasks',
    durable=True,  # 持久化，RabbitMQ 重启后队列依然存在
    arguments={
        'x-dead-letter-exchange': 'judge-dlx',      # 指定死信交换机
        'x-dead-letter-routing-key': 'judge.task'  # 死信路由键
    }
)

# 绑定主队列到主交换机
channel.queue_bind(
    queue='judge-tasks',
    exchange='judge-exchange',
    routing_key='judge.task'
)

# 死信交换机
channel.exchange_declare(
    exchange='judge-dlx',
    exchange_type='direct',
    durable=True
)

# 死信队列
channel.queue_declare(
    queue='judge-tasks-dlq',
    durable=True
)

# 绑定死信队列到死信交换机
channel.queue_bind(
    queue='judge-tasks-dlq',
    exchange='judge-dlx',
    routing_key='judge.task'
)
```

#### 4.3.2 消费者配置

- 多个 judge-service 实例**共享同一个队列**（不需要为每个实例创建单独队列）
- RabbitMQ 自动在多个消费者间分发消息
- 慢的实例不会被其他实例帮助（消息已投递给慢实例）

```python
# 手动 ACK，整个判题流程结束后才发送 ACK
channel.basic_consume(
    queue='judge-tasks',
    on_message_callback=callback,
    auto_ack=False  # 手动确认
)
```

#### 4.3.3 判题任务消息格式

```json
{
  "messageId": "uuid",
  "submissionId": 12345,
  "problemId": 100,
  "sqlContent": "SELECT ...",
  "studentId": 1001,
  "timeLimit": 30,
  "maxMemory": 1024,
  "retryCount": 0,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| messageId | uuid | 消息唯一标识 |
| submissionId | long | 提交ID |
| problemId | long | 题目ID |
| sqlContent | string | 学生提交的 SQL |
| studentId | long | 学生ID |
| timeLimit | int | 超时时间（秒），从题目获取 |
| maxMemory | int | 内存限制（MB） |
| retryCount | int | 重试次数，用于死信处理 |
| timestamp | datetime | 消息创建时间 |

#### 4.3.4 消息持久化配置

| 组件 | 配置 | 说明 |
|------|------|------|
| 交换机 | durable=true | RabbitMQ 重启后交换机依然存在 |
| 队列 | durable=true | RabbitMQ 重启后队列依然存在 |
| 消息 | delivery_mode=2 | 消息持久化 |

---

## 5. 容器池架构

### 5.1 容器生命周期

```
学生提交 SQL
    ↓
submission-service 记录提交，投递任务到 RabbitMQ
    ↓
judge-service 消费任务
    ↓
向 container-manager 申请容器
    ↓
container-manager 从池中获取可用容器，返回连接信息
    ↓
judge-service 连接容器内 MySQL
    ↓
执行初始化 SQL（恢复题目初始状态）
    ↓
执行学生 SQL
    ↓
结果比对（DQL/DML/DDL/DCL）
    ↓
回写结果到 result-service
    ↓
释放容器（重置后放回池中）
```

### 5.2 容器池配置

| 配置项 | 开发环境 | 生产环境 |
|--------|----------|----------|
| 预启动容器数 | 5 | 50+ |
| 容器内 MySQL 版本 | 8.0 | 8.0 |
| 容器资源限制 | 1核 1G | 2核 2G |
| 单次判题超时 | 按难度 | 按难度 |
| 容器获取超时 | 10s | 5s |
| 容器最大使用次数 | 100次 | 100次 |
| 容器网络 | judge_net | judge_net |

### 5.3 容器镜像要求

- 基于 `mysql:8.0` 镜像
- 预装必要的 SQL 客户端工具（mysql-client）
- 容器启动后自动启动 MySQL 服务
- MySQL 仅监听 `127.0.0.1:3306`（不暴露到宿主机）
- 创建专用 `judge` 用户，权限限制在 `problem_%` 数据库

### 5.4 容器 MySQL 配置

| 配置项 | 值 | 说明 |
|--------|-----|------|
| innodb_buffer_pool_size | 64MB | 限制内存占用 |
| max_connections | 50 | 限制并发连接 |
| max_allowed_packet | 16MB | 单条 SQL 最大大小 |
| local_infile | 0 | 禁用 LOAD DATA LOCAL INFILE |
| log_bin | 0 | 禁用二进制日志 |
| sql_mode | STRICT_TRANS_TABLES | 严格模式 |

### 5.5 容器生命周期

```
容器创建（启动 MySQL）
    ↓
首次使用：acquire Container
    ↓
重置 judge 用户密码（动态）
    ↓
执行初始化 SQL（创建题目数据库 problem_{id}）
    ↓
执行学生 SQL
    ↓
比对结果
    ↓
重置容器（删除 problem_xxx 数据库）
    ↓
归还容器池（release）
    ↓
后续使用：复用容器（重新初始化）
    ↓
使用 100 次后销毁重建
```

### 5.6 容器重置策略

每次判题完成后执行重置：

```sql
-- 删除所有题目数据库
DROP DATABASE IF EXISTS `problem_{id}`;

-- 可选：重置 judge 用户密码
ALTER USER 'judge'@'%' IDENTIFIED BY 'new_random_password';
FLUSH PRIVILEGES;
```

### 5.7 网络隔离

- 创建专用 Docker 网络 `judge_net`
- 所有判题容器和 judge-service 加入该网络
- 判题容器 MySQL 不映射到宿主机端口
- judge-service 通过 Docker 服务发现访问容器

---

## 6. 安全考虑

### 6.1 学生 SQL 限制

| 限制类型 | 实现方式 |
|----------|----------|
| 超时限制 | `MAX_EXECUTION_TIME` 或超时中断 |
| 资源限制 | cgroups 限制 CPU/内存 |
| 危险 SQL 拦截 | 词法分析拦截 DROP/ALTER 等 |
| 数据库范围 | 仅允许访问题目对应数据库 |

### 6.2 鉴权

- 所有公网 API 需通过 gateway 统一鉴权
- JWT Token 包含用户 ID 和角色
- 教师接口需要 TEACHER 角色
- 学生接口需要 STUDENT 角色

### 6.3 内部服务隔离

**内部服务（不暴露公网）**：

| 服务 | 说明 |
|------|------|
| **judge-service** | 核心判题逻辑，仅通过 RabbitMQ 消费任务，不提供 HTTP API |
| **container-manager** | 容器池管理，仅供内部服务调用，不暴露公网 |
| **result-service** (创建结果接口) | 仅供 judge-service 回写结果 |

**隔离策略**：

```
公网用户
    ↓
gateway (鉴权、路由)
    ↓
┌─────────────────────────────────────────┐
│  公网服务 (通过 gateway 访问)             │
│  - user-service:8081                   │
│  - problem-service:8082                 │
│  - submission-service:8083             │
│  - result-service:8086 (查询接口)        │
└─────────────────────────────────────────┘

内部服务 (不暴露公网，仅内网访问)
┌─────────────────────────────────────────┐
│  - judge-service:8084 (无 HTTP API)    │
│  - container-manager:8085               │
│  - result-service:8086 (回写接口)       │
└─────────────────────────────────────────┘
```

### 6.4 容器连接认证

容器连接使用**临时令牌机制**：

| 机制 | 说明 |
|------|------|
| 令牌生成 | 每次 `acquire` 时生成新令牌 |
| 有效期 | 5 分钟，过期自动失效 |
| 认证方式 | `connectionToken` 替代明文密码 |
| 验证 | judge-service 使用令牌连接容器 MySQL |

---

## 7. 部署架构

### 7.1 开发环境

使用 Docker Compose 启动所有服务：

```
┌─────────────────────────────────────────────────────┐
│                   Docker Network                     │
│                                                      │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐             │
│  │ Gateway │  │  User   │  │Problem  │             │
│  │ :8080   │  │ :8081   │  │ :8082   │             │
│  └────┬────┘  └─────────┘  └─────────┘             │
│       │                                                │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐ │
│  │Submit   │  │ Result* │  │Container*│           │
│  │ :8083   │  │ :8086   │  │ :8085   │           │
│  └────┬────┘  └────┬────┘  └────┬────┘           │
│       │            │            │                   │
│       └────────────┴────────────┘                   │
│                    ↑                                 │
│         (内部服务，不暴露公网)                        │
│                                                      │
│  ┌─────────────────────────────────────────────────┐ │
│  │              MySQL (业务库) :3306               │ │
│  └─────────────────────────────────────────────────┘ │
│                                                      │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐               │
│  │Container│ │Container│ │Container│  (判题容器池) │
│  │  MySQL  │ │  MySQL  │ │  MySQL  │               │
│  └─────────┘ └─────────┘ └─────────┘               │
│                                                      │
└─────────────────────────────────────────────────────┘

* Result 和 Container 服务仅内部调用，不暴露公网
```

**说明**：
- Gateway、Nginx 仅路由到公网服务
- judge-service、container-manager、result-service(部分接口) 为内部服务
- 内部服务在同一 Docker Network 内通过内网 IP 通信

### 7.2 生产环境（简要）

- Kubernetes 部署各微服务
- 配置 HPA 自动扩缩容
- 使用 ConfigMap/Secret 管理配置
- 日志统一收集到 ELK

---

## 8. 扩展性考虑

### 8.1 判题服务扩展

- 判题是性能瓶颈，可以水平扩展 judge-service 实例
- RabbitMQ 消费者组确保负载均衡

### 8.2 容器池扩展

- container-manager 可以独立扩展
- 监控容器池水位，动态启停容器

### 8.3 AI 辅助扩展

- DDL/DCL 语义分析可以作为独立 AI 服务
- 通过 RPC 调用，保持判题服务解耦

---

## 9. 出题流程

### 9.1 题目状态流转

```
DRAFT (草稿)
    ↓ 教师完善题目信息
    ↓
READY (准备就绪)
    ↓ 教师发布
    ↓
PUBLISHED (已发布，学生可见)
    ↓ 教师归档
    ↓
ARCHIVED (已归档)
```

### 9.2 出题详细流程

```
教师创建题目 (DRAFT)
    ↓
填写题目信息（标题、描述、难度、SQL类型）
    ↓
编写初始化 SQL (initSql)
    ↓
编写标准答案 (standardAnswer)
    ↓
创建测试用例
    ↓
设置题目标签（可选）
    ↓
状态变更为 READY
    ↓
（系统自动生成期望结果，教师可调整）
    ↓
发布题目 (PUBLISHED)
```

### 9.3 期望结果自动生成

当教师设置 `standardAnswer` 后，系统自动执行标准答案生成期望结果：

| SQL 类型 | 期望结果类型 | 生成方式 |
|----------|-------------|----------|
| DQL | RESULT_SET | 执行标准答案，保存结果集 |
| DML | DATA_SNAPSHOT | 执行标准答案，保存数据快照 |
| DDL | METADATA | 执行标准答案，保存表结构 |
| DCL | PRIVILEGE | 执行标准答案，保存权限信息 |

---

## 10. 文档输出

本架构文档为设计阶段的产物，后续将产出：

1. **OpenAPI 规范** (`docs/api/*.yaml`) - 各服务 API 契约
2. **数据库 Schema** (`docs/schema/`) - 完整 DDL
3. **数据流时序图** (`docs/architecture/data-flow.md`) - 服务交互流程
4. **测试策略** (`docs/testing/`) - 单元/集成/E2E 测试规范
