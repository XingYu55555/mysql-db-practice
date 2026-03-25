# SQL 判题引擎 - 数据流与时序图

## 1. 整体数据流概览

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              教师操作流程                                      │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   教师                        Gateway                  Problem Service        │
│     │                            │                          │               │
│     │───── POST /api/problem ───►│──────────────────────────►│               │
│     │                            │                          │               │
│     │  创建题目                   │                          │ 存储题目元数据     │
│     │  (含初始化SQL、              │                          │ 存储测试用例      │
│     │   测试用例、                 │                          │                │
│     │   标准答案)                  │                          │                │
│     │                            │                          │               │
│     │◄──────── 201 Created ──────│◄──────────────────────────│               │
│     │                            │                          │               │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│                              学生答题流程                                      │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   学生                        Gateway                 Submission Svc          │
│     │                            │                          │               │
│     │───── POST /api/submission ►│─────────────────────────►│               │
│     │                            │                          │               │
│     │  提交 SQL 答案              │                          │ 记录提交       │
│     │                            │                          │ 投递判题任务    │
│     │                            │                          │ 到 RabbitMQ    │
│     │                            │                          │               │
│     │◄──────── 202 Accepted ─────│◄──────────────────────────│               │
│     │  (返回 submission_id)       │                          │               │
│     │                            │                          │               │
└──────────────────────────────────────────────────────────────────────────────┘

                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                              判题异步流程                                     │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Judge Service              Container Manager          RabbitMQ              │
│         │                            │                      │               │
│         │◄─────── consume ───────────│                      │               │
│         │    (判题任务消息)             │                      │               │
│         │                            │                      │               │
│         │──── POST /container/acquire ►│                      │               │
│         │                            │                      │               │
│         │      申请容器                │ 从池中获取可用容器      │               │
│         │                            │ 返回连接信息           │               │
│         │◄─── 200 {container_id, ...} ─│                      │               │
│         │                            │                      │               │
│         │ 连接容器内 MySQL             │                      │               │
│         │ 执行题目初始化SQL            │                      │               │
│         │ 执行学生 SQL                 │                      │               │
│         │ 结果比对(DQL/DML/DDL/DCL)    │                      │               │
│         │                            │                      │               │
│         │──── POST /result ──────────►│ (通过内部调用)         │               │
│         │      回写判题结果             │                      │               │
│         │                            │                      │               │
│         │──── POST /container/release ►│                      │               │
│         │      释放容器(重置后放回池)    │                      │               │
│         │                            │                      │               │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 核心时序图

### 2.1 用户注册登录流程

```mermaid
sequenceDiagram
    participant Student as 学生/教师
    participant Gateway
    participant UserSvc as User Service
    participant DB as MySQL

    Student->>Gateway: POST /api/user/register {username, password, role}
    Gateway->>UserSvc: 转发注册请求
    UserSvc->>DB: 检查用户是否已存在
    alt 用户不存在
        DB-->>UserSvc: 用户不存在
        UserSvc->>DB: 插入用户记录 (密码BCrypt加密)
        DB-->>UserSvc: 成功
        UserSvc-->>Gateway: 201 {userId, username, role}
        Gateway-->>Student: 注册成功
    else 用户已存在
        UserSvc-->>Gateway: 409 Conflict
        Gateway-->>Student: 注册失败 (用户已存在)
    end

    Student->>Gateway: POST /api/user/login {username, password}
    Gateway->>UserSvc: 转发登录请求
    UserSvc->>DB: 查询用户
    alt 验证成功
        DB-->>UserSvc: 用户信息
        UserSvc->>UserSvc: 生成JWT Token
        UserSvc-->>Gateway: 200 {token, expiresIn}
        Gateway-->>Student: 登录成功 (返回Token)
    else 密码错误
        UserSvc-->>Gateway: 401 Unauthorized
        Gateway-->>Student: 登录失败 (密码错误)
    end
```

### 2.2 教师创建题目流程

```mermaid
sequenceDiagram
    participant Teacher as 教师
    participant Gateway
    participant ProblemSvc as Problem Service
    participant DB as MySQL (业务库)

    Teacher->>Gateway: POST /api/problem<br/>{title, description, sqlType,<br/>difficulty, initSql,<br/>testCases: [{name, expectedResult, type}]}
    Gateway->>ProblemSvc: 验证JWT (role=TEACHER)
    ProblemSvc->>DB: 开启事务
    DB-->>ProblemSvc: 事务开启

    ProblemSvc->>DB: INSERT problems 表 (status=DRAFT)
    DB-->>ProblemSvc: problem_id

    loop 每个测试用例
        ProblemSvc->>DB: INSERT test_cases 表
        DB-->>ProblemSvc: test_case_id
    end

    ProblemSvc->>DB: 提交事务
    DB-->>ProblemSvc: 成功

    ProblemSvc-->>Gateway: 201 {problemId, title, status: DRAFT}
    Gateway-->>Teacher: 题目创建成功 (草稿状态)
```

### 2.2.1 教师设置标准答案和期望结果

```mermaid
sequenceDiagram
    participant Teacher as 教师
    participant Gateway
    participant ProblemSvc as Problem Service
    participant JudgeSvc as Judge Service (内部)
    participant Container as Docker Container (判题库)

    Teacher->>Gateway: PUT /api/problem/{problemId}<br/>{standardAnswer: "CREATE TABLE ..."}
    Gateway->>ProblemSvc: 更新题目标准答案

    ProblemSvc->>JudgeSvc: 内部调用 - 生成期望结果
    JudgeSvc->>Container: 获取临时容器
    JudgeSvc->>Container: 执行 initSql 初始化题目数据库
    JudgeSvc->>Container: 执行 standardAnswer
    JudgeSvc->>Container: 提取期望结果 (元数据/数据快照)
    JudgeSvc->>Container: 释放容器

    JudgeSvc-->>ProblemSvc: 期望结果 JSON
    ProblemSvc->>DB: 更新 test_cases.expected_result
    ProblemSvc-->>Gateway: 200 OK
    Gateway-->>Teacher: 标准答案已保存，期望结果已生成
```

### 2.2.2 题目状态流转

```mermaid
sequenceDiagram
    participant Teacher as 教师
    participant Gateway
    participant ProblemSvc as Problem Service

    Teacher->>Gateway: PUT /api/problem/{problemId}/status<br/>{status: "READY"}
    Gateway->>ProblemSvc: 更新状态
    ProblemSvc->>DB: UPDATE problems SET status='READY'
    ProblemSvc-->>Gateway: 200 OK
    Gateway-->>Teacher: 题目已准备就绪

    Note over Teacher: 教师确认题目无误后发布

    Teacher->>Gateway: PUT /api/problem/{problemId}/status<br/>{status: "PUBLISHED"}
    Gateway->>ProblemSvc: 更新状态
    ProblemSvc->>DB: UPDATE problems SET status='PUBLISHED'
    ProblemSvc-->>Gateway: 200 OK
    Gateway-->>Teacher: 题目已发布，学生可见
```

### 2.2.3 题目标签管理

```mermaid
sequenceDiagram
    participant Teacher as 教师
    participant Gateway
    participant ProblemSvc as Problem Service
    participant DB as MySQL

    Teacher->>Gateway: GET /api/tag (获取可用标签)
    Gateway->>ProblemSvc: 查询标签列表
    ProblemSvc->>DB: SELECT * FROM tags
    DB-->>ProblemSvc: 标签列表
    ProblemSvc-->>Gateway: [{tagId, name, color}, ...]
    Gateway-->>Teacher: 标签列表

    Teacher->>Gateway: PUT /api/problem/{problemId}/tags<br/>{tagIds: [1, 3, 5]}
    Gateway->>ProblemSvc: 更新题目标签
    ProblemSvc->>DB: DELETE FROM problem_tags WHERE problem_id=?
    loop 每个标签
        ProblemSvc->>DB: INSERT INTO problem_tags VALUES (problem_id, tag_id)
    end
    ProblemSvc-->>Gateway: 200 OK
    Gateway-->>Teacher: 标签已更新
```

### 2.2.4 批量导入题目

```mermaid
sequenceDiagram
    participant Teacher as 教师
    participant Gateway
    participant ProblemSvc as Problem Service
    participant DB as MySQL

    Teacher->>Gateway: POST /api/problem/batch<br/>{problems: [{title, description, ...}, ...]}
    Gateway->>ProblemSvc: 批量导入请求
    ProblemSvc->>DB: 开启事务

    loop 每个题目
        ProblemSvc->>DB: INSERT problems 表
        ProblemSvc->>DB: INSERT test_cases 表
    end

    alt 全部成功
        DB-->>ProblemSvc: 提交成功
        ProblemSvc-->>Gateway: 201 {successCount: 10, failCount: 0}
        Gateway-->>Teacher: 导入成功 (10题)
    else 部分失败
        DB-->>ProblemSvc: 部分失败
        ProblemSvc-->>Gateway: 201 {successCount: 8, failCount: 2,<br/>errors: [{index, title, error}]}
        Gateway-->>Teacher: 导入完成 (8成功, 2失败)
    end
```

### 2.3 学生提交答案流程

```mermaid
sequenceDiagram
    participant Student as 学生
    participant Gateway
    participant SubmitSvc as Submission Service
    participant ProblemSvc as Problem Service
    participant DB as MySQL (业务库)
    participant MQ as RabbitMQ

    Student->>Gateway: POST /api/submission<br/>{problemId, sqlContent}
    Gateway->>SubmitSvc: 验证JWT (role=STUDENT)
    SubmitSvc->>ProblemSvc: GET /problem/{problemId} 验证题目存在
    ProblemSvc-->>SubmitSvc: 题目信息

    SubmitSvc->>DB: INSERT submissions 表 (status=PENDING)
    DB-->>SubmitSvc: submission_id

    SubmitSvc->>MQ: 发布判题任务消息<br/>{submissionId, problemId, sqlContent}
    MQ-->>SubmitSvc: 消息发布成功

    SubmitSvc-->>Gateway: 202 Accepted<br/>{submissionId, status: PENDING}
    Gateway-->>Student: 提交成功

    Note over Student: 轮询或WebSocket<br/>查询判题结果
    Student->>Gateway: GET /api/submission/{submissionId}
    Gateway->>SubmitSvc: 查询提交状态
    SubmitSvc-->>Gateway: {status, result}
    Gateway-->>Student: 提交状态/结果
```

### 2.4 核心判题流程

```mermaid
sequenceDiagram
    participant JudgeSvc as Judge Service
    participant ContainerMgr as Container Manager
    participant Container as Docker Container (MySQL)
    participant ResultSvc as Result Service
    participant MQ as RabbitMQ
    participant AI as AI Service (阿里云百炼)

    JudgeSvc->>MQ: 消费判题任务
    MQ-->>JudgeSvc: {submissionId, problemId, sqlContent}

    JudgeSvc->>ContainerMgr: POST /container/acquire
    ContainerMgr-->>JudgeSvc: {containerId, ip, port, mysqlUser, connectionToken}

    JudgeSvc->>Container: 连接 MySQL (jdbc:mysql://{ip}:{port}/)
    JudgeSvc->>Container: CREATE DATABASE IF NOT EXISTS problem_{problemId}
    JudgeSvc->>Container: USE problem_{problemId}
    JudgeSvc->>Container: 执行题目初始化 SQL (设置初始状态)

    alt DQL (SELECT)
        JudgeSvc->>Container: 执行学生 SQL (SELECT)
        JudgeSvc->>Container: 获取结果集
        JudgeSvc->>JudgeSvc: 比对结果集 (列数、列名、行数、数据)
    else DML (INSERT/UPDATE/DELETE)
        JudgeSvc->>Container: 执行学生 SQL
        JudgeSvc->>JudgeSvc: 比对 affectedRows
    else DDL (CREATE/ALTER) - AI辅助
        JudgeSvc->>Container: 执行学生 DDL
        alt aiAssisted = true
            JudgeSvc->>AI: 调用 AI 服务分析语义等价性
            Note over JudgeSvc,AI: 输入: 题目描述 + 标准答案 + 学生答案
            AI-->>JudgeSvc: 返回判定结果 (isCorrect, confidence, reason)
            JudgeSvc->>JudgeSvc: 根据置信度返回 AI_APPROVED/AI_REJECTED
        else aiAssisted = false
            JudgeSvc->>JudgeSvc: 规则比对（标准化后字符串比较）
        end
    else DCL (GRANT/REVOKE) - AI辅助
        alt aiAssisted = true
            JudgeSvc->>AI: 调用 AI 服务分析语义等价性
            AI-->>JudgeSvc: 返回判定结果
        else aiAssisted = false
            JudgeSvc->>JudgeSvc: 关键字匹配（GRANT/REVOKE）
        end
    end

    JudgeSvc->>ResultSvc: POST /result {submissionId, score, status, ...}
    ResultSvc-->>JudgeSvc: 200 OK

    JudgeSvc->>ContainerMgr: POST /container/release {containerId}
    ContainerMgr->>Container: 重置容器 (清理数据)
    ContainerMgr-->>Container: 放回池中

    JudgeSvc->>MQ: 确认消息消费 (ACK)
```

### 2.5 查询成绩流程

```mermaid
sequenceDiagram
    participant Student as 学生
    participant Gateway
    participant ResultSvc as Result Service
    participant DB as MySQL

    Student->>Gateway: GET /api/result/submission/{submissionId}
    Gateway->>ResultSvc: 验证JWT (role=STUDENT)
    ResultSvc->>DB: 查询 judge_results + submissions
    DB-->>ResultSvc: 结果详情
    ResultSvc-->>Gateway: {submissionId, problemTitle, score, status, executionTime}
    Gateway-->>Student: 成绩详情

    Student->>Gateway: GET /api/result/history?page=1&size=10
    Gateway->>ResultSvc: 查询历史成绩
    ResultSvc-->>Gateway: 分页成绩列表
    Gateway-->>Student: 历史成绩

    Teacher->>Gateway: GET /api/result/problem/{problemId}/leaderboard
    Gateway->>ResultSvc: 查询题目排行榜
    ResultSvc-->>Gateway: 排行榜数据
    Gateway-->>Teacher: 排行榜
```

---

## 3. 数据模型关系

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│    users    │       │  problems   │       │ test_cases  │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ id (PK)     │       │ id (PK)     │       │ id (PK)     │
│ username    │       │ title       │◄──────│ problem_id  │
│ password    │       │ description │       │ name        │
│ role        │       │ sql_type    │       │ init_sql    │
│ email       │       │ difficulty  │       │ expected_*  │
│ created_at  │       │ status      │       │ weight      │
└─────────────┘       │ teacher_id  │       └─────────────┘
                      │ created_at  │
                      └─────────────┘
                            │
                            │ N:M
                            ▼
┌─────────────┐       ┌─────────────┐
│problem_tags │       │    tags     │
├─────────────┤       ├─────────────┤
│ problem_id  │◄──────│ id (PK)     │
│ tag_id     │       │ name        │
└─────────────┘       │ color       │
                            │               └─────────────┘
                            ▼
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│submissions  │       │judge_results│       │  RabbitMQ   │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ id (PK)     │──────►│ submission_id│      │ queue:      │
│ problem_id  │       │ test_case_id │      │ judge-tasks │
│ student_id  │       │ score        │      │ judge-results│
│ sql_content │       │ status       │      └─────────────┘
│ status      │       │ error_msg    │
│ submitted_at│       │ exec_time    │
└─────────────┘       └─────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  Docker 容器内 MySQL                         │
├─────────────────────────────────────────────────────────────┤
│  Database: problem_{problem_id}                            │
│  ├── 初始化SQL创建的表和数据                                  │
│  └── 学生SQL执行后的数据变更                                  │
│                                                             │
│  注意: 每个容器有独立的MySQL实例，互不干扰                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. 关键数据流转

### 4.1 判题任务消息格式

```json
{
  "messageId": "uuid",
  "submissionId": 12345,
  "problemId": 100,
  "sqlContent": "SELECT * FROM employees WHERE salary > 3000",
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
| timeLimit | int | 超时时间（秒），从题目难度获取 |
| maxMemory | int | 内存限制（MB） |
| retryCount | int | 重试次数，用于死信处理 |
| timestamp | datetime | 消息创建时间 |

### 4.2 判题结果回写

```json
{
  "submissionId": 12345,
  "overallScore": 85.0,
  "overallStatus": "PARTIAL_CORRECT",
  "testCaseResults": [
    {
      "testCaseId": 1,
      "score": 85.0,
      "status": "PARTIAL_CORRECT",
      "executionTimeMs": 150,
      "errorMessage": null
    }
  ],
  "metadata": {
    "containerId": "container_001",
    "executionTimeMs": 1500
  }
}
```

---

## 5. 错误处理流程

### 5.1 容器获取失败

```mermaid
sequenceDiagram
    participant JudgeSvc as Judge Service
    participant ContainerMgr as Container Manager
    participant MQ as RabbitMQ

    JudgeSvc->>ContainerMgr: POST /container/acquire
    ContainerMgr-->>JudgeSvc: 503 Service Unavailable (无可用容器)

    JudgeSvc->>JudgeSvc: 等待重试 (指数退避, 最多3次)

    alt 重试成功
        ContainerMgr-->>JudgeSvc: 200 OK (容器)
        Note over JudgeSvc: 继续判题流程
    else 重试失败
        JudgeSvc->>ResultSvc: POST /result (status=ERROR)
        JudgeSvc->>MQ: NACK 消息 (不重试)
        Note over JudgeSvc: 记录错误日志
    end
```

### 5.2 SQL 执行超时

```mermaid
sequenceDiagram
    participant JudgeSvc as Judge Service
    participant Container as Container MySQL
    participant ResultSvc as Result Service

    JudgeSvc->>Container: 设置 MAX_EXECUTION_TIME=30000
    JudgeSvc->>Container: 执行学生 SQL

    alt 执行超时
        Container-->>JudgeSvc: Query execution timeout
        JudgeSvc->>ResultSvc: POST /result<br/>{status: TIME_LIMIT_EXCEEDED}
    else 执行成功
        JudgeSvc->>ResultSvc: POST /result<br/>{status: ACCEPTED/WRONG_ANSWER}
    end
```

---

## 6. 并发控制

### 6.1 容器池并发

- container-manager 使用连接池管理 Docker API
- 容器获取使用互斥锁防止同一容器被并发分配
- 容器池水位监控，低于阈值时异步启动新容器

### 6.2 判题并发

- judge-service 多个实例消费 RabbitMQ 同一队列
- RabbitMQ 消费者组确保消息负载均衡
- 每个实例维护本地容器缓存减少 container-manager 调用

---

## 7. 监控指标

| 指标 | 说明 | 采集点 |
|------|------|--------|
| container_pool_size | 当前池中容器数 | container-manager |
| container_available | 可用容器数 | container-manager |
| container_acquire_time | 容器获取耗时 | container-manager |
| judge_queue_size | 判题队列长度 | judge-service |
| judge_execution_time | 判题执行耗时 | judge-service |
| judge_success_rate | 判题成功率 | judge-service |
| submission_count | 提交总数 | submission-service |
